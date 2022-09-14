package vn.bnh.oim.utils;

import oracle.iam.platformservice.exception.PlatformServiceException;
import org.junit.Test;

import javax.security.auth.login.LoginException;

public class ImportTest {
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String passwd = "oracle_4U";
    String jarDir = "/tmp/core-1.0.jar";
    String jarName = "core-1.0.jar";



    @Test
    public void importJar() throws LoginException, PlatformServiceException {
        OIMUtils.localInitialize(hostname, port, username, passwd);
        ServerUtils.uploadJar(jarDir, ServerUtils.JarType.JavaTasks);
    }

    @Test
    public void deleteJar() throws LoginException, PlatformServiceException {
        OIMUtils.localInitialize(hostname, port, username, passwd);
        ServerUtils.deleteJar(jarName, ServerUtils.JarType.JavaTasks);
    }

    @Test
    public void register() throws LoginException {
        OIMUtils.localInitialize(hostname, port, username, passwd);
        ServerUtils.registerPlugin("/Users/hieunguyen/work/code/java/OIM/ABB/lib.zip");
    }

    @Test
    public void updateJar() throws LoginException {
        OIMUtils.localInitialize(hostname, port, username, passwd);
        ServerUtils.updateJar(jarDir, jarName, ServerUtils.JarType.JavaTasks);
    }

}
