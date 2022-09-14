package vn.bnh.oim.utils;

import oracle.iam.scheduler.exception.SchedulerAccessDeniedException;
import oracle.iam.scheduler.exception.SchedulerException;
import org.junit.Test;

import javax.security.auth.login.LoginException;
import java.util.Arrays;

public class SchedulerServiceUtilsTest {
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String passwd = "oracle_4U";

    @Test
    public void execute() throws LoginException, SchedulerException, SchedulerAccessDeniedException {
        OIMUtils.localInitialize(hostname, port, username, passwd);
        SchedulerServiceUtils.executeJob("SmartForm User Recon");
    }

    @Test
    public void listJobs() throws LoginException, SchedulerException {
        OIMUtils.localInitialize(hostname, port, username, passwd);
        String[] a = SchedulerServiceUtils.listJobs();
        Arrays.asList(a).forEach(x -> {
            System.out.println(x);
            try {
                SchedulerServiceUtils.executeJob(x);
            } catch (SchedulerException | SchedulerAccessDeniedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}