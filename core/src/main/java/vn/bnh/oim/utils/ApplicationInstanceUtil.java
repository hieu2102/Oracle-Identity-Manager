package vn.bnh.oim.utils;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcTaskNotFoundException;
import Thor.API.Operations.tcProvisioningOperationsIntf;
import Thor.API.tcResultSet;
import com.thortech.xl.orb.dataaccess.tcDataAccessException;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.provisioning.api.ApplicationInstanceService;
import oracle.iam.provisioning.api.ProvisioningConstants;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.exception.*;
import oracle.iam.provisioning.vo.*;

import java.util.*;
import java.util.stream.Collectors;

public class ApplicationInstanceUtil {
    private static final ODLLogger logger = ODLLogger.getODLLogger(ApplicationInstanceUtil.class.getName());
    private static final ApplicationInstanceService applicationInstanceService = OIMUtil.getService(ApplicationInstanceService.class);
    private static final tcProvisioningOperationsIntf provisioningOperationsIntf = OIMUtil.getService(tcProvisioningOperationsIntf.class);
    private static final ProvisioningService provisioningService = OIMUtil.getService(ProvisioningService.class);


    public static Account getAccount(long id) throws GenericProvisioningException, AccountNotFoundException {
        return provisioningService.getAccountDetails(id);
    }

    public static Set<Account> getAccountsForUser(
            String userLogin,
            String appInstanceName,
            ProvisioningConstants.ObjectStatus accountStatus
    ) throws UserLookupException, NoSuchUserException, UserNotFoundException, GenericProvisioningException {
        String userKey = UserUtil.getUser(userLogin).getId();
        SearchCriteria provisionedCriteria = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.ACCOUNT_STATUS.getId(), accountStatus.getId(), SearchCriteria.Operator.EQUAL);
        return provisioningService.getAccountsProvisionedToUser(userKey, provisionedCriteria, new HashMap<>(), true).stream().filter(x -> x.getAppInstance().getApplicationInstanceName().equals(appInstanceName)).collect(Collectors.toSet());

//        return null;
    }

    public static Account getAccountForUser(
            String userKey,
            String appInstName,
            String accountStatus,
            long processInstKey
    ) throws UserNotFoundException, GenericProvisioningException {
        SearchCriteria provisionedCriteria = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.ACCOUNT_STATUS.getId(), accountStatus, SearchCriteria.Operator.EQUAL);
        List<Account> accountList = provisioningService.getAccountsProvisionedToUser(userKey, provisionedCriteria, new HashMap<>(), true);
        if (!accountList.isEmpty()) {
            return accountList.stream().filter(account -> account.getAppInstance().getApplicationInstanceName().equals(appInstName)).filter(account -> account.getProcessInstanceKey().equals(String.valueOf(processInstKey))).collect(Collectors.toList()).get(0);
        }
        return null;
    }

    public static ApplicationInstance getApplicationInstance(String resourceObjName) throws GenericAppInstanceServiceException {
        SearchCriteria sc = new SearchCriteria(ApplicationInstance., resourceObjName, SearchCriteria.Operator.EQUAL);
        return applicationInstanceService.findApplicationInstance(sc, new HashMap<>()).get(0);
    }

    public static Account getUserPrimaryAccount(
            String userID,
            String appInstName
    ) throws UserNotFoundException, GenericProvisioningException {
        SearchCriteria appSC = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.APPINST_NAME, appInstName, SearchCriteria.Operator.EQUAL);
        SearchCriteria typeSC = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.ACCOUNT_TYPE, "Primary", SearchCriteria.Operator.EQUAL);
        SearchCriteria mergedSC = new SearchCriteria(typeSC, appSC, SearchCriteria.Operator.AND);
        return provisioningService.getAccountsProvisionedToUser(userID, mergedSC, new HashMap<>(), true).get(0);
    }

    public static Set<Account> getProvisioningAccountsForUser(
            User user,
            String appInstanceName
    ) throws UserNotFoundException, GenericProvisioningException {
        String userKey = user.getId();
        SearchCriteria provisionedCriteria = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.ACCOUNT_STATUS.getId(), ProvisioningConstants.ObjectStatus.PROVISIONING.getId(), SearchCriteria.Operator.EQUAL);
        return provisioningService.getAccountsProvisionedToUser(userKey, provisionedCriteria, new HashMap<>(), true).stream().filter(x -> x.getAppInstance().getApplicationInstanceName().equals(appInstanceName)).collect(Collectors.toSet());
    }

    public static Account updateAccountData(
            Account account,
            Map<String, Object> newAccountData
    ) throws GenericProvisioningException, AccountNotFoundException {
        AccountData updatedAccountData = new AccountData(account.getAccountData().getFormKey(), account.getAccountData().getUdTablePrimaryKey(), newAccountData);
        updatedAccountData.setChildData(account.getAccountData().getChildData());
        Account modifiedAccount = new Account(account.getAccountID(), account.getProcessInstanceKey(), account.getUserKey());
        modifiedAccount.setAccountData(updatedAccountData);
        modifiedAccount.setAppInstance(account.getAppInstance());
        provisioningService.modify(modifiedAccount);
        return modifiedAccount;
    }

    public static Set<Account> getProvisioningAccount(String appInstName) throws tcAPIException, UserNotFoundException, GenericProvisioningException, tcColumnNotFoundException, UserLookupException, NoSuchUserException {
        HashMap<String, String> filter = new HashMap<>();
        filter.put("Objects.Name", appInstName);
        filter.put("Process Definition.Tasks.Task Name", "Create User");
        tcResultSet results = provisioningOperationsIntf.findAllOpenProvisioningTasks(filter, new String[]{"Rejected"});
        Set<Account> output = new HashSet<>();
        logger.log(ODLLevel.INFO, "{0} accounts with state PROVISIONING: {1}", new Object[]{appInstName, results.getTotalRowCount()});
        for (int i = 0; i < results.getTotalRowCount(); i++) {
            results.goToRow(i);
            User beneficiary = UserUtil.getUser(results.getStringValue("Process Instance.Task Information.Target User"));
            output.addAll(getProvisioningAccountsForUser(beneficiary, appInstName));
        }
        return output;
    }

    public static void retryAccountProvision(Account resourceAcct) throws tcAPIException, tcColumnNotFoundException, tcTaskNotFoundException {
        String processInstFormKey = resourceAcct.getProcessInstanceKey(); // (ORC_KEY) Process Form Instance Key
        String appInstName = resourceAcct.getAppInstance().getApplicationInstanceName(); // Application Instance Name
        HashMap<String, String> filter = new HashMap<>();
        filter.put("Objects.Name", appInstName);
        filter.put("Process Instance.Key", processInstFormKey);
        filter.put("Process Definition.Tasks.Task Name", "Create User");
        tcResultSet results = provisioningOperationsIntf.findAllOpenProvisioningTasks(filter, new String[]{"Rejected"});
        if (results.getTotalRowCount() > 0) {
            results.goToRow(0);
            provisioningOperationsIntf.retryTask(results.getLongValue("Process Instance.Task Details.Key"));
        }
    }

    public static void provisionAccount(
            String userLogin,
            String appInstName,
            Map<String, Object> parentData
    ) throws UserLookupException, NoSuchUserException, ApplicationInstanceNotFoundException, GenericAppInstanceServiceException, UserNotFoundException, GenericProvisioningException {
        User user = UserUtil.getUser(userLogin);
        ApplicationInstance appInst = applicationInstanceService.findApplicationInstanceByName(appInstName);
        Long resourceFormKey = appInst.getAccountForm().getFormKey();
        String udTablePrimaryKey = null;
        AccountData accountData = new AccountData(String.valueOf(resourceFormKey), udTablePrimaryKey, parentData);
        Account resAccount = new Account(appInst, accountData);
        provisioningService.provision(user.getId(), resAccount);
    }

    public static void provisionAccount(
            String userLogin,
            String appInstName,
            Map<String, Object> parentData,
            Map<String, Object> childData
    ) throws UserNotFoundException, ApplicationInstanceNotFoundException, GenericProvisioningException, UserLookupException, NoSuchUserException, GenericAppInstanceServiceException, tcAPIException, tcTaskNotFoundException, tcColumnNotFoundException {
        User user = UserUtil.getUser(userLogin);
        ApplicationInstance appInst = applicationInstanceService.findApplicationInstanceByName(appInstName);
        Long resourceFormKey = appInst.getAccountForm().getFormKey();
        String udTablePrimaryKey = null;
        AccountData accountData = new AccountData(String.valueOf(resourceFormKey), udTablePrimaryKey, parentData);
        ChildTableRecord ctr = new ChildTableRecord();
        ctr.setChildData(childData);
        ArrayList<ChildTableRecord> listChild = new ArrayList<>();
        listChild.add(ctr);
        HashMap<String, ArrayList<ChildTableRecord>> childTableMap = new HashMap<>();
        childTableMap.put("UD_GROUPS", listChild);
        accountData.setChildData(childTableMap);
        Account resAccount = new Account(appInst, accountData);
        provisioningService.provision(user.getId(), resAccount);
        ApplicationInstanceUtil.retryAccountProvision(resAccount);

    }

    public static Account getAccountByProcessInstKey(long processInstKey) throws GenericProvisioningException, AccountNotFoundException, tcAPIException, tcDataAccessException, tcColumnNotFoundException, UserNotFoundException {
        tcResultSet resultSet = provisioningOperationsIntf.getObjectDetail(processInstKey);
        String userKey;
        String appInstName;
        String accountStatus;
        if (resultSet.getTotalRowCount() > 0) {
            resultSet.goToRow(0);
            userKey = resultSet.getStringValue("Users.Key");
            appInstName = resultSet.getStringValue("Objects.Name");
            accountStatus = resultSet.getStringValue("Objects.Object Status.Status");
            return getAccountForUser(userKey, appInstName, accountStatus, processInstKey);
        }
        return null;
    }
}
