package vn.bnh.oim.scheduledtasks.lookups;


import org.junit.Test;
import vn.bnh.oim.utils.OIMUtils;

import java.util.HashMap;

public class ReconcileGenericLookupTest {
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String password = "oracle_4U";

    @Test
    public void execute() throws Exception {
        OIMUtils.localInitialize(hostname, port, username, password);
        HashMap params = new HashMap<String, Object>();
        params.put("Lookup Table", "Lookup.ABB.SmartForm.BranchId");
        params.put("File Dir", "/Users/hieund/Downloads/smf.branchid.txt");
        params.put("Value Delimiter", ",");
        ReconcileGenericLookup task = new ReconcileGenericLookup();
        task.execute(params);

    }

}