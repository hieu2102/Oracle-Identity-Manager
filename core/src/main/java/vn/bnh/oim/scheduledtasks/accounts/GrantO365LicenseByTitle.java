package vn.bnh.oim.scheduledtasks.accounts;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcInvalidLookupException;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.provisioning.exception.AccountNotFoundException;
import oracle.iam.provisioning.exception.GenericAppInstanceServiceException;
import oracle.iam.provisioning.exception.GenericProvisioningException;
import oracle.iam.provisioning.exception.UserNotFoundException;
import oracle.iam.provisioning.vo.ApplicationInstance;
import oracle.iam.provisioning.vo.ChildTableRecord;
import oracle.iam.reconciliation.vo.Account;
import oracle.iam.scheduler.vo.TaskSupport;
import vn.bnh.oim.utils.ApplicationInstanceUtils;
import vn.bnh.oim.utils.LookupUtils;
import vn.bnh.oim.utils.ReconciliationUtils;
import vn.bnh.oim.utils.UserUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GrantO365LicenseByTitle extends TaskSupport {
    private static final ODLLogger logger = ODLLogger.getODLLogger(GrantO365LicenseByTitle.class.getName());
    private String licenseMatrixPrefix;
    private HashMap<String, Object> taskParams;
    private String appInstName;
    private String resourceObjName;
    private String fromDate;
    private String defaultLicense;
    private HashMap<String, HashMap<String, String>> licenseMatrices;
    private HashMap<String, String> o365Licenses;
    private static final String childFormName = "UD_O365_LIC";
    private static final String childFieldName = "UD_O365_LIC_LICENSE_NAME";

    private Set<User> getOIMUsersWithReconciledAccounts() {
        Set<Account> reconciledAccounts = ReconciliationUtils.getReconciliationEvents(this.resourceObjName, this.fromDate);
        logger.log(ODLLevel.INFO, "Reconciled Account List: {0}", reconciledAccounts.size());
        return reconciledAccounts.stream().map(x -> UserUtils.getUserByUserLogin(x.getOwnerName())).collect(Collectors.toSet());
    }

    private void grantLicense(
            User user,
            String title,
            String licenseToAdd
    ) throws UserNotFoundException, GenericProvisioningException, AccountNotFoundException {
        logger.log(ODLLevel.INFO, "Grant License {0} to User {1} with Title {2}", new Object[]{licenseToAdd, user.getLogin(), title});
        oracle.iam.provisioning.vo.Account o365Account = ApplicationInstanceUtils.getUserPrimaryAccount(user.getId(), this.appInstName);
        ArrayList<ChildTableRecord> grantedLicenses = o365Account.getAccountData().getChildData().get(childFormName);
        int userIsAlreadyGrantedLicense = (int) grantedLicenses.stream().filter(x -> x.getChildData().get(childFieldName).toString().equalsIgnoreCase(licenseToAdd)).count();
        if (userIsAlreadyGrantedLicense == 0) {
            ChildTableRecord newLicense = new ChildTableRecord();
            newLicense.setAction(ChildTableRecord.ACTION.Add);
            Map<String, Object> childData = new HashMap<>();
            childData.put(childFieldName, licenseToAdd);
            newLicense.setChildData(childData);
            grantedLicenses.add(newLicense);
            ApplicationInstanceUtils.modifyAccount(o365Account);
            logger.log(ODLLevel.INFO, "User {0} granted License {1}", new Object[]{user.getLogin(), licenseToAdd});
        } else {
            logger.log(ODLLevel.INFO, "User {0} is already granted License {1}", new Object[]{user.getLogin(), licenseToAdd});

        }

    }

    @Override
    public void execute(HashMap hashMap) throws Exception {
        this.taskParams = hashMap;
        setAttributes();
        Set<User> oimUsers = getOIMUsersWithReconciledAccounts();
        for (User user : oimUsers) {
            boolean grantDefaultLicense = true;
            String title = user.getAttribute("Title").toString();
            for (Map.Entry<String, HashMap<String, String>> entry : this.licenseMatrices.entrySet()) {
                String license = entry.getKey();
                HashMap<String, String> lookupContent = entry.getValue();
                for (String s : lookupContent.keySet()) {
                    if (title.toUpperCase().matches(".+" + s.toUpperCase() + "\\s.+")) {
                        String licenseToAdd = license;
                        grantDefaultLicense = false;
                        grantLicense(user, title, licenseToAdd);
                    }
                }
            }
            if (grantDefaultLicense) {
                grantLicense(user, title, defaultLicense);

            }
        }
    }

    @Override
    public HashMap getAttributes() {
        return null;
    }

    @Override
    public void setAttributes() {
        this.licenseMatrixPrefix = this.taskParams.get("License Matrix Lookup Table Prefix").toString();
        this.appInstName = this.taskParams.get("Application Instance").toString();
        this.resourceObjName = this.taskParams.get("Resource Object Name").toString();
        this.defaultLicense = this.taskParams.get("Default License").toString();
        this.fromDate = this.taskParams.get("From Date").toString().isEmpty() ? null : this.taskParams.get("From Date").toString();
//        get list of licenses
        try {
            ApplicationInstance applicationInstance = ApplicationInstanceUtils.getApplicationInstance(this.appInstName);
            String childFormLookupTable = applicationInstance.getChildForms().get(0).getFormFields().get(0).getProperties().get("Lookup Code").toString();
            this.licenseMatrices = new HashMap<>();
            this.o365Licenses = LookupUtils.getLookupValues(childFormLookupTable);
            for (Map.Entry<String, String> license : o365Licenses.entrySet()) {
                try {
                    String licenseMatrix = licenseMatrixPrefix + "." + license.getValue();
                    if (!license.getKey().equalsIgnoreCase(defaultLicense)) {
                        HashMap<String, String> matrixContent = LookupUtils.getLookupValues(licenseMatrix);
                        licenseMatrices.put(license.getKey(), matrixContent);
                    }
                } catch (tcInvalidLookupException e) {
//                    continue
                }
            }
        } catch (GenericAppInstanceServiceException | tcAPIException | tcColumnNotFoundException |
                 tcInvalidLookupException e) {
            throw new RuntimeException(e);
        }
    }
}
