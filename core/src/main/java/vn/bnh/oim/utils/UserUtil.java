package vn.bnh.oim.utils;

import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.vo.User;

import java.util.HashSet;
import java.util.List;

public class UserUtil {
    private static final UserManager userService = OIMUtil.getService(UserManager.class);

    public static User getUser(String userLogin) throws UserLookupException, NoSuchUserException {
        return userService.getDetails(userLogin, new HashSet<>(), true);
    }

    public static List<String> getUserLoginsByPrefix(String prefix) {
        return null;
    }
}
