package vn.bnh.oim.utils;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.exception.UserSearchException;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.provisioning.exception.GenericProvisioningException;
import oracle.iam.provisioning.exception.UserNotFoundException;
import org.junit.Test;

import javax.security.auth.login.LoginException;
import java.util.HashMap;

public class OIMUtilTest {
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String passwd = "oracle_4U";

    @Test
    public void testUserLoginGeneration() throws LoginException, UserSearchException {
        OIMUtil.localInitialize(hostname, port, username, passwd);
        String userLogins = UserUtil.generateUserLogin("hieund");
        System.out.println(userLogins);
    }

    @Test
    public void testGetUser() throws UserLookupException, NoSuchUserException, UserNotFoundException, GenericProvisioningException, LoginException {
        OIMUtil.localInitialize(hostname, port, username, passwd);
        User user = UserUtil.getUser("XELSYSADM");
        assert "XELSYSADM".equals(user.getLogin());
        ApplicationInstanceUtil.getProvisioningAccountsForUser(user, "TRM").forEach(System.out::println);
    }

    @Test
    public void testGetUserAccounts() throws UserLookupException, NoSuchUserException, UserNotFoundException, GenericProvisioningException, LoginException {
        OIMUtil.localInitialize(hostname, port, username, passwd);
        User user = UserUtil.getUser("USER@ORACLE.COM");
        ApplicationInstanceUtil.getProvisioningAccountsForUser(user, "TRM").forEach(x -> {
            assert user.getId().equals(x.getUserKey());
            assert "TRM".equals(x.getAppInstance().getApplicationInstanceName());
            x.getAppInstance().getChildForms().stream().forEach(cf -> {
                System.out.println(cf.getDescription());
            });
        });
    }

    @Test
    public void testGetProvisioningAccount() throws tcAPIException, UserNotFoundException, GenericProvisioningException, UserLookupException, NoSuchUserException, tcColumnNotFoundException, LoginException {
        OIMUtil.localInitialize(hostname, port, username, passwd);
        int outputSize = ApplicationInstanceUtil.getProvisioningAccount("TRM").size();
        System.out.println(outputSize);
    }

    @Test
    public void testProvisionAccount() throws Exception {
        OIMUtil.localInitialize(hostname, port, username, passwd);
        HashMap<String, Object> childData = new HashMap<>();
        childData.put("UD_GROUPS_GROUP_NAME", "group1");
        ApplicationInstanceUtil.provisionAccount("USER@ORACLE.COM", "FlexCash", new HashMap<>(), childData);
    }
}
