package vn.bnh.oim.utils;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcTaskNotFoundException;
import Thor.API.Operations.TaskDefinitionOperationsIntf;
import Thor.API.Operations.tcProvisioningOperationsIntf;
import Thor.API.tcResultSet;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApplicationInstanceUtil {
    private static final tcProvisioningOperationsIntf provisioningOperationsIntf = OIMUtil.provisioningOperationsIntf;
    private static final TaskDefinitionOperationsIntf taskDefOps = OIMUtil.taskDefOps;
    private static final ProvisioningService provisioningService = OIMUtil.provisioningService;

    public static List<Account> getProvisioningAccountsForUser(User user, String appInstanceName) throws UserNotFoundException, GenericProvisioningException {
        String userKey = user.getId();
        SearchCriteria provisionedCriteria = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.ACCOUNT_STATUS.getId(), ProvisioningConstants.ObjectStatus.PROVISIONING.getId(), SearchCriteria.Operator.EQUAL);
        return provisioningService.getAccountsProvisionedToUser(userKey, provisionedCriteria, new HashMap<>(), true).stream().filter(x -> x.getAppInstance().getApplicationInstanceName().equals(appInstanceName)).collect(Collectors.toList());
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
