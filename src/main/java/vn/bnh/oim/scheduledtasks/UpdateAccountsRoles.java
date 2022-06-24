package vn.bnh.oim.scheduledtasks;

import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.provisioning.vo.Account;
import oracle.iam.scheduler.vo.TaskSupport;
import vn.bnh.oim.utils.ApplicationInstanceUtil;
import vn.bnh.oim.utils.OIMUtil;

import java.util.HashMap;
import java.util.Set;

@SuppressWarnings("rawtypes")
public class UpdateAccountsRoles extends TaskSupport {
    private final ODLLogger logger = ODLLogger.getODLLogger(UpdateAccountsRoles.class.getName());
    private HashMap ScheduledTaskInputParams;
    private String APP_INST_NAME;
    private String PARENT_PROCESS_FORM_ROLE_FIELD;
    private String PARENT_PROCESS_FORM_ROLE_FIELD_FORMAT;
    private String CHILD_PROCESS_FORM_NAME;

    @Override
    public void execute(HashMap hashMap) throws Exception {
        this.ScheduledTaskInputParams = hashMap;
        logger.log(ODLLevel.INFO, "Get all accounts in PROVISIONING states for Application Instance {0}", APP_INST_NAME);
        Set<Account> accountList = ApplicationInstanceUtil.getProvisioningAccount(APP_INST_NAME);
        accountList.forEach(account -> {
            
        });
    }

    @Override
    public HashMap getAttributes() {
        return null;
    }

    @Override
    public void setAttributes() {
        this.APP_INST_NAME = this.ScheduledTaskInputParams.get("Application Instance Name").toString();
        this.PARENT_PROCESS_FORM_ROLE_FIELD = this.ScheduledTaskInputParams.get("Parent Process Form's Role Field Name").toString();
        this.PARENT_PROCESS_FORM_ROLE_FIELD_FORMAT = this.ScheduledTaskInputParams.get("Role Field's Format").toString();
        this.CHILD_PROCESS_FORM_NAME = this.ScheduledTaskInputParams.get("Child Process Form's Name").toString();
//        initialize OIMUTil
        OIMUtil oimUtil = new OIMUtil();
    }
}
