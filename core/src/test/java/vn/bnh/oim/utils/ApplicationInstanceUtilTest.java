package vn.bnh.oim.utils;

import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.provisioning.exception.GenericProvisioningException;
import oracle.iam.provisioning.exception.UserNotFoundException;
import oracle.iam.provisioning.vo.Account;
import org.junit.Test;

import javax.security.auth.login.LoginException;
import java.util.List;

public class ApplicationInstanceUtilTest {

    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String password = "oracle_4U";

    @Test
    public void getPrimaryAccount() throws LoginException, UserLookupException, NoSuchUserException, UserNotFoundException, GenericProvisioningException {
        OIMUtil.localInitialize(hostname, port, username, password);
        User user = UserUtil.getUser("uyennt@bnh.vn");
        Account account = ApplicationInstanceUtil.getUserPrimaryAccount(user.getId(), "O365AppIns");
        System.out.println(account);
    }

}