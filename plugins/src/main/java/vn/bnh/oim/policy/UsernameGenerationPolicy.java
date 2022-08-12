package vn.bnh.oim.policy;

import oracle.iam.identity.exception.UserNameGenerationException;
import oracle.iam.identity.usermgmt.api.AbstractUserNameGenerationPolicy;
import oracle.iam.identity.usermgmt.api.UserNameGenerationPolicy;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

public class UsernameGenerationPolicy extends AbstractUserNameGenerationPolicy implements UserNameGenerationPolicy {
    @Override
    public String getUserName(Map<String, Object> map) throws UserNameGenerationException {
        String firstName = map.get("First Name").toString();
        String lastName = map.get("Last Name").toString();
        StringBuilder userLoginPrefixBuilder = new StringBuilder(firstName);
        Arrays.asList(lastName.split("\\s")).forEach(s -> {
            userLoginPrefixBuilder.append(s.charAt(0));
        });
        return null;
    }

    @Override
    public boolean isGivenUserNameValid(
            String s,
            Map<String, Object> map
    ) {
        return false;
    }

    @Override
    public String getDescription(Locale locale) {
        return "Generate Username from 'First Name' and 'Last Name' attributes";
    }
}
