package vn.bnh.oim.utils;

import oracle.iam.identity.exception.*;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.passwordmgmt.api.PasswordMgmtService;
import oracle.iam.passwordmgmt.vo.PasswordPolicyInfo;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.idm.common.ipf.api.password.RandomPasswordGenerator;
import oracle.idm.common.ipf.api.password.RandomPasswordGeneratorImpl;
import oracle.idm.common.ipf.api.vo.PasswordPolicy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class UserUtils {
    private static final UserManager userService = OIMUtils.getService(UserManager.class);
    private static final PasswordMgmtService passwordService = OIMUtils.getService(PasswordMgmtService.class);
    private static final RandomPasswordGenerator randomPasswordGenerator = new RandomPasswordGeneratorImpl();

    private static PasswordPolicy getPasswordPolicy(PasswordPolicyInfo passwordPolicyInfo) {
        PasswordPolicy policy = new PasswordPolicy();
        policy.setName(passwordPolicyInfo.getName());
        policy.setId(passwordPolicyInfo.getId());
        policy.setMaxLength(passwordPolicyInfo.getMaxLength());
        policy.setMinLength(passwordPolicyInfo.getMinLength());
        policy.setMinAlphabets(passwordPolicyInfo.getMinAlphabets());
        policy.setMinNumerals(passwordPolicyInfo.getMinNumerals());
        policy.setMinAlphaNumerals(passwordPolicyInfo.getMinAlphaNumerals());
        policy.setMinSpecialChars(passwordPolicyInfo.getMinSpecialChars());
        policy.setMaxSpecialChars(passwordPolicyInfo.getMaxSpecialChars());
        policy.setMinUpperCase(passwordPolicyInfo.getMinUpperCase());
        policy.setMinLowerCase(passwordPolicyInfo.getMinLowerCase());
        policy.setMinUniqueChars(passwordPolicyInfo.getMinUniqueChars());
        policy.setMaxRepeatedChars(passwordPolicyInfo.getMaxRepeatedChars());
        policy.setStartsWithAlphabet(passwordPolicyInfo.getStartsWithAlphabet());
        policy.setMinUnicodeChars(passwordPolicyInfo.getMinUnicodeChars());
        policy.setMaxUnicodeChars(passwordPolicyInfo.getMaxUnicodeChars());
        policy.setMinPasswordAgeInDays(passwordPolicyInfo.getMinPasswordAgeInDays());
        policy.setPasswordExpiresAfterInDays(passwordPolicyInfo.getPasswordExpiresAfterInDays());
        policy.setPasswordWarningAfterInDays(passwordPolicyInfo.getPasswordWarningAfterInDays());
        policy.setAllowedChars(passwordPolicyInfo.getAllowedChars());
        policy.setDictionaryLocation(passwordPolicyInfo.getDictionaryLocation());
        policy.setNumPasswordsInHistory(passwordPolicyInfo.getNumPasswordsInHistory());
        policy.setLockoutDuration(passwordPolicyInfo.getLockoutDuration());
        policy.setMaxIncorrectAttempts(passwordPolicyInfo.getMaxIncorrectAttempts());
        policy.setChSource(passwordPolicyInfo.getChSource());
        policy.setChDefaultQuestions(passwordPolicyInfo.getChDefaultQuestions());
        policy.setChMinQuestions(passwordPolicyInfo.getChMinQuestions());
        policy.setChMinAnswers(passwordPolicyInfo.getChMinAnswers());
        policy.setChResponseMinLength(passwordPolicyInfo.getChResponseMinLength());
        policy.setChMaxIncorrectAttempts(passwordPolicyInfo.getChMaxIncorrectAttempts());
        policy.setDesc(passwordPolicyInfo.getDesc());
        return policy;
    }

    public static String setUserPassword(String userLogin) throws UserManagerException, SearchKeyNotUniqueException {
        PasswordPolicyInfo passwordPolicyInfo = passwordService.getApplicablePasswordPolicy(userLogin, true);
        User user = getUserByUserLogin(userLogin);
        char[] password = randomPasswordGenerator.generatePassword(getPasswordPolicy(passwordPolicyInfo), null);
        System.out.println(new String(password));
        userService.changePassword("User Login", user.getLogin(), password, false);
        return new String(password);
    }

    public static User getUser(
            String fieldName,
            String fieldValue
    ) {
        try {
            return userService.getDetails(fieldName, fieldValue, new HashSet<>());
        } catch (NoSuchUserException | UserLookupException | SearchKeyNotUniqueException e) {
            throw new RuntimeException(e);
        }
    }

    public static User getUserByUserLogin(String userLogin) {
        try {
            return userService.getDetails(userLogin, new HashSet<>(), true);
        } catch (NoSuchUserException | UserLookupException e) {
            throw new RuntimeException(e);
        }
    }

    public static User getUserByKey(String userKey) throws UserLookupException, NoSuchUserException {
        return userService.getDetails(userKey, new HashSet<>(), false);
    }

    public static String getUserPassword(String userLogin) throws UserLookupException, NoSuchUserException {
        User user = getUserByUserLogin(userLogin);
        return user.getPasswordGenerated();
    }

    public static void resetUserPassword(String userLogin) throws UserManagerException {
        User user = getUserByUserLogin(userLogin);
        User manager = getUserByKey(user.getManagerKey());
        String managerEmail = manager.getEmail();
        HashMap<String, Object> control = new HashMap<>();
        control.put("SentNotification", true);
        control.put("SendNotificationTo", managerEmail);
        userService.resetPassword(userLogin, true, control);

    }

    public static String generateUserLogin(String prefix) throws UserSearchException {
        SearchCriteria sc = new SearchCriteria("User Login", prefix, SearchCriteria.Operator.BEGINS_WITH);
        List<User> users = userService.search(sc, new HashSet<>(), null);
        String userLoginWithNumber = prefix.concat("\\d+");
        List<String> userWithPrefix = users.stream().map(User::getLogin).filter(x -> x.equalsIgnoreCase(prefix) || x.matches(userLoginWithNumber)).collect(Collectors.toList());
        int userWithPrefixCount = userWithPrefix.size();
        System.out.printf("[%s] User Login matches Prefix: %s, %s", UserUtils.class.getName(), userWithPrefix, userWithPrefixCount);
        return userWithPrefixCount == 0 ? prefix : prefix + userWithPrefixCount;
    }
}
