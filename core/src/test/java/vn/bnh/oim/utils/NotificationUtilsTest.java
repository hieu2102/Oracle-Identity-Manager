package vn.bnh.oim.utils;

import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.notification.exception.*;
import oracle.iam.platformservice.exception.InvalidCacheCategoryException;
import org.junit.Test;

public class NotificationUtilsTest {
    public NotificationUtilsTest() {
        try {
            OIMUtils.localInitialize(hostname, port, username, passwd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String passwd = "oracle_4U";


    @Test
    public void sendNotification() throws ProviderNotEnabledException, ProviderNotFoundException, EventException, UnresolvedNotificationDataException, UserDetailsNotFoundException, MultipleTemplateException, NotificationException, NotificationResolverNotFoundException, TemplateNotFoundException, NotificationProviderException, UserLookupException, NoSuchUserException, InvalidCacheCategoryException {
        ServerUtils.purgeCache();
        User user = UserUtils.getUserByUserLogin("TAMNTH@ABBANK.VN");
        NotificationUtils.sendUserCreatedNotification(user, "123abc");
    }

}