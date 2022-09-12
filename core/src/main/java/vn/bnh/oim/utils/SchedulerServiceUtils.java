package vn.bnh.oim.utils;

import oracle.iam.scheduler.api.SchedulerService;
import oracle.iam.scheduler.exception.SchedulerAccessDeniedException;
import oracle.iam.scheduler.exception.SchedulerException;

public class SchedulerServiceUtils {
    private static SchedulerService schService = OIMUtils.getService(SchedulerService.class);
    
    public static String[] listJobs() throws SchedulerException {
        return schService.getAllJobs();
    }

    public static void executeJob(String jobName) throws SchedulerException, SchedulerAccessDeniedException {
        schService.triggerNow(jobName);
    }
}
