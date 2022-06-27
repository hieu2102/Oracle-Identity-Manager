package vn.bnh.oim.scheduledtasks;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcTaskNotFoundException;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.provisioning.exception.AccountNotFoundException;
import oracle.iam.provisioning.exception.GenericProvisioningException;
import oracle.iam.provisioning.vo.Account;
import oracle.iam.provisioning.vo.ChildTableRecord;
import oracle.iam.scheduler.vo.TaskSupport;
import vn.bnh.oim.utils.ApplicationInstanceUtil;
import vn.bnh.oim.utils.OIMUtil;

import java.util.*;

@SuppressWarnings("rawtypes")
public class UpdateAccountsRoles extends TaskSupport {
    private static final ODLLogger logger = ODLLogger.getODLLogger(UpdateAccountsRoles.class.getName());
    private HashMap scheduledTaskInputParams;
    private String APP_INST_NAME;
    private String PARENT_PROCESS_FORM_ROLE_FIELD;
    private String PARENT_PROCESS_FORM_ROLE_FIELD_FORMAT;
    private String CHILD_PROCESS_FORM_NAME;

    @Override
    public void execute(HashMap hashMap) throws Exception {
        this.scheduledTaskInputParams = hashMap;
        logger.log(ODLLevel.INFO, "Get all accounts in PROVISIONING states for Application Instance {0}", APP_INST_NAME);
        Set<Account> accountList = ApplicationInstanceUtil.getProvisioningAccount(APP_INST_NAME);
        accountList.forEach(account -> {
            Map<String, Object> accountData = account.getAccountData().getData();
            Set<String> processedRoleData = new HashSet<>();
            ArrayList<ChildTableRecord> childFormRows = account.getAccountData().getChildData().get(CHILD_PROCESS_FORM_NAME);
            childFormRows.forEach(row -> {
                Map<String, Object> childData = row.getChildData();
                processedRoleData.add(processChildData(PARENT_PROCESS_FORM_ROLE_FIELD_FORMAT, childData));
            });
            String updatedRoleData = String.join(",", processedRoleData);
            accountData.put(PARENT_PROCESS_FORM_ROLE_FIELD, updatedRoleData);
            try {
                Account modifiedAccount = ApplicationInstanceUtil.updateAccountData(account, accountData);
                ApplicationInstanceUtil.retryAccountProvision(modifiedAccount);
            } catch (GenericProvisioningException | AccountNotFoundException | tcAPIException |
                     tcColumnNotFoundException | tcTaskNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    protected static String processChildData(String format, Map<String, Object> childData) {
        String output = format;
        for (Map.Entry<String, Object> entry : childData.entrySet()) {
            String stringToReplace = String.format("\\$\\(%s\\)\\$", entry.getKey());
            output = output.replaceFirst(stringToReplace, String.valueOf(entry.getValue()));
        }
        return output;
    }

    @Override
    public HashMap getAttributes() {
        return null;
    }

    @Override
    public void setAttributes() {
        this.APP_INST_NAME = this.scheduledTaskInputParams.get("Application Instance Name").toString();
        this.PARENT_PROCESS_FORM_ROLE_FIELD = this.scheduledTaskInputParams.get("Parent Process Form's Role Field Name").toString();
        this.PARENT_PROCESS_FORM_ROLE_FIELD_FORMAT = this.scheduledTaskInputParams.get("Role Field's Format").toString();
        this.CHILD_PROCESS_FORM_NAME = this.scheduledTaskInputParams.get("Child Process Form's Name").toString();
//        initialize OIMUTil
        OIMUtil.initialize();
    }
}
