package vn.bnh.oim.scheduledtasks.lookups;


import org.junit.Test;
import vn.bnh.oim.utils.OIMUtils;

import javax.security.auth.login.LoginException;
import java.util.HashMap;

public class ReconcileLookupsTest {
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String password = "oracle_4U";

    @Test
    public void execute() throws Exception {
        OIMUtils.localInitialize(hostname, port, username, password);
        HashMap params = new HashMap<String, Object>();
        params.put("Lookup Prefix", "Lookup.ABB.Office365.License");
        params.put("File Dir", "/Users/hieund/Downloads/license.csv");
        params.put("Value Delimiter", ",");
        ReconcileLookups task = new ReconcileLookups();
        task.execute(params);

    }
}