package vn.bnh.oim.utils;

import oracle.iam.identity.exception.UserManagerException;
import oracle.iam.identity.usermgmt.vo.User;
import org.junit.Test;
import javax.security.auth.login.LoginException;

public class UserUtilsTest {
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String passwd = "oracle_4U";

    @Test
    public void resetPassword() throws LoginException, UserManagerException {
//        OIMUtils.localInitialize(hostname, port, username, passwd);
//        String password = UserUtils.setUserPassword("TAMNTH@ABBANK.VN");
//        User user = UserUtils.getUserByUserLogin("TAMNTH@ABBANK.VN");
        System.out.println(passwd.replaceAll(".+(.{3})", "$1"));
//        System.out.println(user.getPasswordCreationDate());
//        NotificationUtils.sendUserCreatedNotification(UserUtils.getUserByUserLogin("TAMNTH@ABBANK.VN"), password);
    }

}