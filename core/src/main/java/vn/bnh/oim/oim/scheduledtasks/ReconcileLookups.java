package vn.bnh.oim.oim.scheduledtasks;

import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.scheduler.vo.TaskSupport;

import java.util.HashMap;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ReconcileLookups extends TaskSupport {
    private final ODLLogger logger = ODLLogger.getODLLogger(ReconcileLookups.class.getName());
    private HashMap<String, Object> scheduledTaskInputParams;


    @Override
    public void execute(HashMap hashMap) throws Exception {
        this.scheduledTaskInputParams = hashMap;

    }

    @Override
    public HashMap getAttributes() {
        return null;
    }

    @Override
    public void setAttributes() {

    }
}
