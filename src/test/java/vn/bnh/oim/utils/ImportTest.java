package vn.bnh.oim.utils;

import oracle.iam.platformservice.exception.PlatformServiceException;
import org.junit.Test;

import javax.security.auth.login.LoginException;

public class ImportTest {
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String passwd = "oracle_4U";
    String jarDir = "/tmp/ScheduledTask-1.0.jar";

    @Test
    public void importJar() throws LoginException, PlatformServiceException {
        OIMUtil.localInitialize(hostname, port, username, passwd);
        ServerUtil.uploadJar(jarDir, ServerUtil.JarType.JavaTasks);
    }
}
