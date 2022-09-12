package vn.bnh.oim.utils;

import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.platformservice.exception.PlatformServiceException;
import oracle.iam.provisioning.api.ProvisioningConstants;
import oracle.iam.provisioning.exception.GenericProvisioningException;
import oracle.iam.provisioning.exception.UserNotFoundException;
import oracle.iam.provisioning.vo.Account;
import org.junit.Test;

import javax.security.auth.login.LoginException;
import java.util.Set;

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
        OIMUtils.localInitialize(hostname,port,username,passwd);
        ServerUtils.registerPlugin("/Users/hieunguyen/work/code/java/OIM/ABB/core/deploy/lib.zip");
    }
    @Test
    public void updateJar() throws LoginException {
        OIMUtils.localInitialize(hostname, port, username, passwd);
        ServerUtils.updateJar(jarDir, jarName, ServerUtils.JarType.JavaTasks);
    }

}
