package vn.bnh.oim.utils;

import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.exception.UserSearchException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;

import java.util.HashSet;
import java.util.List;

public class UserUtil {
    private static final UserManager userService = OIMUtil.getService(UserManager.class);

    public static User getUser(String userLogin) throws UserLookupException, NoSuchUserException {
        return userService.getDetails(userLogin, new HashSet<>(), true);
    }

    public static String generateUserLogin(String prefix) throws UserSearchException {
        String userLoginWithNumber = prefix.concat("\\d+");
        SearchCriteria sc = new SearchCriteria("User Login", prefix, SearchCriteria.Operator.BEGINS_WITH);
        List<User> users = userService.search(sc, new HashSet<>(), null);
        int userWithPrefixCount = (int) users.stream().map(User::getLogin).filter(x -> x.equalsIgnoreCase(prefix) || x.matches(userLoginWithNumber)).count();
        return userWithPrefixCount == 0 ? prefix : prefix + userWithPrefixCount;
    }
}
