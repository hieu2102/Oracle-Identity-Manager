package vn.bnh.oim.oim.policy;

import oracle.iam.identity.exception.UserNameGenerationException;
import oracle.iam.identity.exception.UserSearchException;
import oracle.iam.identity.usermgmt.api.AbstractUserNameGenerationPolicy;
import oracle.iam.identity.usermgmt.api.UserNameGenerationPolicy;
import vn.bnh.oim.utils.UserUtil;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

public class UsernameGenerationPolicy extends AbstractUserNameGenerationPolicy implements UserNameGenerationPolicy {
    @Override
    public String getUserName(Map<String, Object> map) throws UserNameGenerationException {
        try {
            return UserUtil.generateUserLogin(generatePrefix(map));
        } catch (UserSearchException e) {
            throw new RuntimeException(e);
        }
    }

    private String generatePrefix(Map<String, Object> map) {
        String firstName = map.get("First Name").toString();
        String lastName = map.get("Last Name").toString();
        StringBuilder userLoginPrefixBuilder = new StringBuilder(firstName);
        Arrays.asList(lastName.split("\\s")).forEach(s -> {
            userLoginPrefixBuilder.append(s.charAt(0));
        });
        return userLoginPrefixBuilder.toString().toUpperCase();

    }

    @Override
    public boolean isGivenUserNameValid(
            String s,
            Map<String, Object> map
    ) {
        String prefix = generatePrefix(map);
        return (s.equalsIgnoreCase(prefix) || s.matches(prefix.concat("\\d+")));
    }

    @Override
    public String getDescription(Locale locale) {
        return "Generate Username from 'First Name' and 'Last Name' attributes";
    }
}
