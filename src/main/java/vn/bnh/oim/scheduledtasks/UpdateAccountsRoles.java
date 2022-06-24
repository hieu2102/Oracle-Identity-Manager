package vn.bnh.oim.scheduledtasks;

import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.scheduler.vo.TaskSupport;

import java.util.HashMap;

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
    }
}
