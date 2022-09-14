package vn.bnh.oim.scheduledtasks.lookups;

import org.junit.Test;
import vn.bnh.oim.utils.OIMUtils;

import java.util.HashMap;

public class ReconcileLookupsTest {
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String passwd = "oracle_4U";

    @Test
    public void test() throws Exception {
        OIMUtils.localInitialize(hostname, port, username, passwd);
        HashMap<String, Object> params = new HashMap<>();
        params.put("Lookup Table", "123.abc");
        params.put("File Directory", "/Users/hieunguyen/test.csv");
        params.put("Value Delimiter", ",");
        ReconcileLookups task = new ReconcileLookups();
        task.execute(params);
    }

}