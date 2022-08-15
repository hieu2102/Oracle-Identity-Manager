package vn.bnh.oim.scheduledtasks;

import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.provisioning.vo.ChildTableRecord;
import oracle.iam.reconciliation.vo.Account;
import oracle.iam.scheduler.vo.TaskSupport;
import vn.bnh.oim.utils.ApplicationInstanceUtil;
import vn.bnh.oim.utils.LookupUtil;
import vn.bnh.oim.utils.ReconciliationUtil;
import vn.bnh.oim.utils.UserUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GrantO365LicenseByTitle extends TaskSupport {
    private static final ODLLogger logger = ODLLogger.getODLLogger(GrantO365LicenseByTitle.class.getName());
    private String titleToLicenseLookupTable;
    private HashMap<String, Object> taskParams;
    private String appInstName;
    private String resourceObjName;
    private static final String childFormName = "UD_O365_LIC";
    private static final String childFieldName = "UD_O365_LIC_LICENSE_NAME";

    private Set<User> getOIMUsersWithReconciledAccounts() {
        Set<Account> reconciledAccounts = ReconciliationUtil.getReconciliationEvents(this.resourceObjName);
        logger.log(ODLLevel.INFO, "Reconciled Account List: {0}", reconciledAccounts.size());
        return reconciledAccounts.stream().map(x -> {
            try {
                return UserUtil.getUser(x.getOwnerName());
            } catch (UserLookupException | NoSuchUserException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
    }

    @Override
    public void execute(HashMap hashMap) throws Exception {
        this.taskParams = hashMap;
        setAttributes();
        Set<User> oimUsers = getOIMUsersWithReconciledAccounts();
        for (User user : oimUsers) {
            String title = user.getAttribute("Title").toString();
            String licenseToAdd = LookupUtil.getLookupValue(this.titleToLicenseLookupTable, title);
            logger.log(ODLLevel.INFO, "Grant License {0} to User {1} with Title {2}", new Object[]{licenseToAdd, user.getLogin(), title});
            oracle.iam.provisioning.vo.Account o365Account = ApplicationInstanceUtil.getUserPrimaryAccount(user.getId(), this.appInstName);
            ArrayList<ChildTableRecord> grantedLicenses = o365Account.getAccountData().getChildData().get(childFormName);
            int userIsAlreadyGrantedLicense = (int) grantedLicenses.stream().filter(x -> x.getChildData().get(childFieldName).toString().equalsIgnoreCase(licenseToAdd)).count();
            if (userIsAlreadyGrantedLicense == 0) {
                ChildTableRecord newLicense = new ChildTableRecord();
                newLicense.setAction(ChildTableRecord.ACTION.Add);
                Map<String, Object> childData = new HashMap<>();
                childData.put(childFieldName, licenseToAdd);
                newLicense.setChildData(childData);
                grantedLicenses.add(newLicense);
                ApplicationInstanceUtil.modifyAccount(o365Account);
                logger.log(ODLLevel.INFO, "User {0} granted License {1}", new Object[]{user.getLogin(), licenseToAdd});
            } else {
                logger.log(ODLLevel.INFO, "User {0} is already granted License {1}", new Object[]{user.getLogin(), licenseToAdd});

            }
        }
    }

    @Override
    public HashMap getAttributes() {
        return null;
    }

    @Override
    public void setAttributes() {
        this.titleToLicenseLookupTable = this.taskParams.get("License Matrix Lookup Table").toString();
        this.appInstName = this.taskParams.get("Application Instance").toString();
        this.resourceObjName = this.taskParams.get("Resource Object Name").toString();
    }
}
