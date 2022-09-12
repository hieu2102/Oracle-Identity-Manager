package vn.bnh.oim.utils;

import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.notification.api.NotificationService;
import oracle.iam.notification.exception.*;
import oracle.iam.notification.vo.NotificationEvent;

import java.util.HashMap;

public class NotificationUtils {
    private static final NotificationService notificationService = OIMUtils.getService(NotificationService.class);

    public static void sendUserCreatedNotification(User user, String userPassword) {
        NotificationEvent notificationEvent = new NotificationEvent();
        notificationEvent.setUserKeys(new String[]{user.getManagerKey()});
        notificationEvent.setTemplateName("ABB New User Created");
        HashMap<String, Object> templateParams = new HashMap<>();
        templateParams.put("userLoginId", user.getLogin());
        templateParams.put("UserId", user.getEmail());
        templateParams.put("password", userPassword);
        templateParams.put("FirstName", user.getFirstName());
        templateParams.put("LastName", user.getLastName());
        notificationEvent.setParams(templateParams);
        try {
            notificationService.notify(notificationEvent, "UMSEmailServiceProvider");
        } catch (EventException | UnresolvedNotificationDataException | TemplateNotFoundException |
                 MultipleTemplateException | NotificationResolverNotFoundException | UserDetailsNotFoundException |
                 NotificationException | ProviderNotFoundException | ProviderNotEnabledException |
                 NotificationProviderException e) {
            throw new RuntimeException(e);
        }
    }
}
