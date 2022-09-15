package vn.bnh.oim.scheduledtasks.accounts;

import org.junit.Test;
import vn.bnh.oim.utils.OIMUtils;

import java.util.HashMap;

public class GrantO365LicenseByTitleTest {
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String password = "oracle_4U";

    @Test
    public void execute() throws Exception {
        OIMUtils.localInitialize(hostname, port, username, password);
        HashMap<String, Object> params = new HashMap<>();
        params.put("License Matrix Lookup Table Prefix", "Lookup.ABB.Office365.License");
        params.put("Application Instance", "O365AppIns");
        params.put("Resource Object Name", "Office365 User");
        params.put("From Date", "20220915");
        params.put("Default License", "121~84a661c4-e949-4bd2-a560-ed7766fcaf2b");
        GrantO365LicenseByTitle task = new GrantO365LicenseByTitle();
        task.execute(params);
    }
}