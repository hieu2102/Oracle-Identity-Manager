package vn.bnh.oim.utils;

import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.provisioning.exception.GenericProvisioningException;
import oracle.iam.provisioning.exception.UserNotFoundException;
import oracle.iam.provisioning.vo.Account;
import org.junit.Test;

import javax.security.auth.login.LoginException;

public class ApplicationInstanceUtilsTest {

    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String password = "oracle_4U";

    @Test
    public void getPrimaryAccount() throws LoginException, UserLookupException, NoSuchUserException, UserNotFoundException, GenericProvisioningException {
        OIMUtils.localInitialize(hostname, port, username, password);
        User user = UserUtils.getUserByUserLogin("LYLTT");
        Account account = ApplicationInstanceUtils.getUserPrimaryAccount(user.getId(), "SmartForm");
        System.out.println(account);

    }

}