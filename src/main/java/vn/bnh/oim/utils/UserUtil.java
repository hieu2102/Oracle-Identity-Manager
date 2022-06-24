package vn.bnh.oim.utils;

import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.vo.User;

import java.util.HashSet;

public class UserUtil {
    private static final UserManager userService = OIMUtil.userService;

    public static User getUser(String userLogin) throws UserLookupException, NoSuchUserException {
        return userService.getDetails(userLogin, new HashSet<>(), true);
    }
}
