package vn.bnh.oim.scheduledtasks;

import org.junit.Test;
import vn.bnh.oim.utils.OIMUtil;

import java.util.HashMap;
import java.util.Map;

public class UpdateAccountsRolesTest {
    UpdateAccountsRoles scheduledTask = new UpdateAccountsRoles();
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String password = "oracle_4U";

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

    @Test
    public void testGrantLicense() throws Exception {
        OIMUtil.localInitialize(hostname, port, username, password);
        HashMap<String, Object> params = new HashMap<>();
        params.put("License Matrix Lookup Table", "Lookup.ABB.Office365.Lisence.Matrix");
        params.put("Resource Object Name", "Office365 User");
        params.put("Application Instance", "O365AppIns");
        GrantO365LicenseByTitle st = new GrantO365LicenseByTitle();
        st.execute(params);
    }
}