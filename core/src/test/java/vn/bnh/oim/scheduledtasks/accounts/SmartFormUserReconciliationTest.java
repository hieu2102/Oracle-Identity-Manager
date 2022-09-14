package vn.bnh.oim.scheduledtasks.accounts;

import oracle.iam.provisioning.vo.ApplicationInstance;
import org.junit.Test;
import vn.bnh.oim.utils.ApplicationInstanceUtils;
import vn.bnh.oim.utils.OIMUtils;

import java.util.HashMap;

public class SmartFormUserReconciliationTest {
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String password = "oracle_4U";

    @Test
    public void abstractRecon() throws Exception {
        OIMUtils.localInitialize(hostname, port, username, password);
        ApplicationInstance applicationInstance = ApplicationInstanceUtils.getApplicationInstance("SmartForm");
        applicationInstance.getAccountForm().getFormFields().forEach(ff -> {
            System.out.println(ff.getLabel());
        });
    }

    @Test
    public void recon() throws Exception {
        OIMUtils.localInitialize(hostname, port, username, password);
        SmartFormUserReconciliation task = new SmartFormUserReconciliation();
        HashMap<String, Object> params = new HashMap<>();
        params.put("Application Name", "SmartForm");
        task.execute(params);
    }

}