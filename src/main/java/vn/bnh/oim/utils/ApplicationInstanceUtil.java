package vn.bnh.oim.utils;

import Thor.API.Operations.tcProvisioningOperationsIntf;
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
    private final tcProvisioningOperationsIntf provisioningOperationsIntf = OIMUtil.provisioningOperationsIntf;
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
}
