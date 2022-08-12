package vn.bnh.oim.utils;

import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.exception.UserSearchException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class UserUtil {
    private static final UserManager userService = OIMUtil.getService(UserManager.class);

    public static User getUser(String userLogin) throws UserLookupException, NoSuchUserException {
        return userService.getDetails(userLogin, new HashSet<>(), true);
    }

    public static String generateUserLogin(String prefix) throws UserSearchException {
        SearchCriteria sc = new SearchCriteria("User Login", prefix, SearchCriteria.Operator.BEGINS_WITH);
        List<User> users = userService.search(sc, new HashSet<>(), null);
        List<String> userLoginsList = users.stream().map(User::getLogin).collect(Collectors.toList());
        String userLoginWithNumber = prefix.concat("\\d+");
        int userWithPrefixCount = userLoginsList.stream().filter(x -> x.equalsIgnoreCase(prefix) || x.matches(userLoginWithNumber)).collect(Collectors.toList()).size();
        return prefix + userWithPrefixCount;
    }
}
