package vn.bnh.oim.scheduledtasks;

import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.provisioning.exception.GenericAppInstanceServiceException;
import oracle.iam.provisioning.vo.ApplicationInstance;
import oracle.iam.reconciliation.vo.Account;
import oracle.iam.scheduler.vo.TaskSupport;
import vn.bnh.oim.utils.ApplicationInstanceUtil;
import vn.bnh.oim.utils.LookupUtil;
import vn.bnh.oim.utils.ReconciliationUtil;
import vn.bnh.oim.utils.UserUtil;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class GrantO365LicenseByTitle extends TaskSupport {
    private static final ODLLogger logger = ODLLogger.getODLLogger(GrantO365LicenseByTitle.class.getName());
    private String titleToLicenseLookupTable;
    private String resouceObjName;
    private HashMap<String, Object> taskParams;
    private ApplicationInstance appInst;

    @Override
    public void execute(HashMap hashMap) throws Exception {
        this.taskParams = hashMap;
        setAttributes();
        Set<Account> reconciledAccounts = ReconciliationUtil.getReconciliationEvents(this.resouceObjName);
        logger.log(ODLLevel.INFO, "Reconciled Account List: {0}", reconciledAccounts.size());
        Set<User> oimUsers = reconciledAccounts.stream().map(x -> {
            try {
                return UserUtil.getUser(x.getOwnerName());
            } catch (UserLookupException | NoSuchUserException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
        for (User user : oimUsers) {
            String title = user.getAttribute("Title").toString();
            String licenseToAdd = LookupUtil.getLookupValue(this.titleToLicenseLookupTable, title);
            oracle.iam.provisioning.vo.Account o365Account = ApplicationInstanceUtil.getUserPrimaryAccount(user.getId(), this.appInst.getApplicationInstanceName());
            System.out.println(o365Account);
        }
    }

    @Override
    public HashMap getAttributes() {
        return null;
    }

    @Override
    public void setAttributes() {
        this.titleToLicenseLookupTable = this.taskParams.get("License Matrix Lookup Table").toString();
        this.resouceObjName = this.taskParams.get("Resource Object Name").toString();
        try {
            this.appInst = ApplicationInstanceUtil.getApplicationInstance(this.resouceObjName);
        } catch (GenericAppInstanceServiceException e) {
            throw new RuntimeException(e);
        }
    }
}
