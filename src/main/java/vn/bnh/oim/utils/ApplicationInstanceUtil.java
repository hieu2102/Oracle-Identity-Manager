package vn.bnh.oim.utils;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcTaskNotFoundException;
import Thor.API.Operations.TaskDefinitionOperationsIntf;
import Thor.API.Operations.tcProvisioningOperationsIntf;
import Thor.API.tcResultSet;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.provisioning.api.ProvisioningConstants;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.exception.AccountNotFoundException;
import oracle.iam.provisioning.exception.GenericProvisioningException;
import oracle.iam.provisioning.exception.UserNotFoundException;
import oracle.iam.provisioning.vo.Account;
import oracle.iam.provisioning.vo.AccountData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ApplicationInstanceUtil {
    private static final ODLLogger logger = ODLLogger.getODLLogger(ApplicationInstanceUtil.class.getName());

    private static final tcProvisioningOperationsIntf provisioningOperationsIntf = OIMUtil.provisioningOperationsIntf;
    private static final TaskDefinitionOperationsIntf taskDefOps = OIMUtil.taskDefOps;
    private static final ProvisioningService provisioningService = OIMUtil.provisioningService;

    public static Set<Account> getProvisioningAccountsForUser(User user, String appInstanceName) throws UserNotFoundException, GenericProvisioningException {
        String userKey = user.getId();
        SearchCriteria provisionedCriteria = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.ACCOUNT_STATUS.getId(), ProvisioningConstants.ObjectStatus.PROVISIONING.getId(), SearchCriteria.Operator.EQUAL);
        return provisioningService.getAccountsProvisionedToUser(userKey, provisionedCriteria, new HashMap<>(), true).stream().filter(x -> x.getAppInstance().getApplicationInstanceName().equals(appInstanceName)).collect(Collectors.toSet());
    }

    public static Account updateAccountData(User accountOwner, Account account, Map<String, Object> newAccountData) throws GenericProvisioningException, AccountNotFoundException {
        AccountData updatedAccountData = new AccountData(account.getAccountData().getFormKey(), account.getAccountData().getUdTablePrimaryKey(), newAccountData);
        updatedAccountData.setChildData(account.getAccountData().getChildData());
        Account modifiedAccount = new Account(account.getAccountID(), account.getProcessInstanceKey(), accountOwner.getId());
        modifiedAccount.setAccountData(updatedAccountData);
        modifiedAccount.setAppInstance(account.getAppInstance());
        provisioningService.modify(modifiedAccount);
        return modifiedAccount;
    }

    public static Set<Account> getProvisioningAccount(String appInstName) throws tcAPIException {
        HashMap<String, String> filter = new HashMap<>();
        filter.put("Objects.Name", appInstName);
        filter.put("Process Definition.Tasks.Task Name", "Create User");
        tcResultSet results = provisioningOperationsIntf.findAllOpenProvisioningTasks(filter, new String[]{"Rejected"});
        Set<Account> output = new HashSet<>();
        logger.log(ODLLevel.INFO, "{0} accounts with state PROVISIONING: {1}", new Object[]{appInstName, results.getTotalRowCount()});
        for (int i = 0; i < results.getTotalRowCount(); i++) {
            results.goToRow(i);
            try {
                User beneficiary = UserUtil.getUser(results.getStringValue("Process Instance.Task Information.Target User"));
                output.addAll(getProvisioningAccountsForUser(beneficiary, appInstName));
            } catch (tcColumnNotFoundException | tcAPIException | UserLookupException | NoSuchUserException |
                     UserNotFoundException | GenericProvisioningException e) {
                throw new RuntimeException(e);
            }
        }
        return output;
    }

    public static void retryAccountProvision(Account resourceAcct) throws tcAPIException, tcColumnNotFoundException, tcTaskNotFoundException {
        String procInstFormKey = resourceAcct.getProcessInstanceKey(); // (ORC_KEY) Process Form Instance Key
        String appInstName = resourceAcct.getAppInstance().getApplicationInstanceName(); // Application Instance Name
        HashMap<String, String> filter = new HashMap<>();
        filter.put("Objects.Name", appInstName);
        filter.put("Process Instance.Key", procInstFormKey);
        filter.put("Process Definition.Tasks.Task Name", "Create User");
        tcResultSet results = provisioningOperationsIntf.findAllOpenProvisioningTasks(filter, new String[]{"Rejected"});
        if (results.getTotalRowCount() > 0) {
            results.goToRow(0);
            provisioningOperationsIntf.retryTask(results.getLongValue("Process Instance.Task Details.Key"));
        }
    }
}
