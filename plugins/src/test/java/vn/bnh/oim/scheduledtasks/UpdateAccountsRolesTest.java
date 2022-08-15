package vn.bnh.oim.scheduledtasks;


import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class UpdateAccountsRolesTest {
    UpdateAccountsRoles scheduledTask = new UpdateAccountsRoles();

    @Test
    public void testStringSubstitution() {
        String expectedOutput = "{\"username\":\"hieund\", \"password\":\"123456a@\"}";
        String template = "{\"username\":\"$(username)$\", \"password\":\"$(password)$\"}";
        Map<String, Object> map = new HashMap<>();
        map.put("username", "hieund");
        map.put("password", "123456a@");
        String output = UpdateAccountsRoles.processChildData(template, map);
        assert expectedOutput.equals(output);
    }
}