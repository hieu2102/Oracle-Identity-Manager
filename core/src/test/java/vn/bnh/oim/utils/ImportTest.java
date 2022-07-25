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
    String jarDir = "/tmp/adapters-1.0.jar";
    String jarName = "adapters-1.0.jar";

    @Test
    public void importJar() throws LoginException, PlatformServiceException {
        OIMUtil.localInitialize(hostname, port, username, passwd);
        ServerUtil.uploadJar(jarDir, ServerUtil.JarType.JavaTasks);
    }

    @Test
    public void deleteJar() throws LoginException, PlatformServiceException {
        OIMUtil.localInitialize(hostname, port, username, passwd);
        ServerUtil.deleteJar(jarName, ServerUtil.JarType.JavaTasks);
    }

    @Test
    public void updateJar() throws LoginException {
        OIMUtil.localInitialize(hostname, port, username, passwd);
        ServerUtil.updateJar(jarDir, jarName, ServerUtil.JarType.JavaTasks);
    }

    @Test
    public void getAccounts() throws LoginException, UserNotFoundException, UserLookupException, GenericProvisioningException, NoSuchUserException {
        OIMUtil.localInitialize(hostname, port, username, passwd);
        String userLogin = "MINHDUCTEST01";
        String appInstName = "FlexCash";
        Set<Account> accounts = ApplicationInstanceUtil.getAccountsForUser(userLogin, appInstName, ProvisioningConstants.ObjectStatus.PROVISIONED);
        accounts.forEach(x -> {
            System.out.println(x.getProcessInstanceKey());
            System.out.println(x.getAccountID());
            System.out.println(x.getAccountData().getData());
        });
    }

    @Test
    public void getAccountByProcInstKey() throws Exception {
        OIMUtil.localInitialize(hostname, port, username, passwd);
        long procInstKey = 163;
        System.out.println(ApplicationInstanceUtil.getAccountByProcessInstKey(procInstKey).getAccountData().getData());
    }
}
