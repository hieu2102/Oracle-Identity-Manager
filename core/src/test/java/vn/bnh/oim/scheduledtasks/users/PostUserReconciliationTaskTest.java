package vn.bnh.oim.scheduledtasks.users;


import oracle.iam.identity.usermgmt.vo.User;
import org.junit.Test;
import vn.bnh.oim.utils.OIMUtils;
import vn.bnh.oim.utils.ReconciliationUtils;

import java.util.Set;

public class PostUserReconciliationTaskTest {
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String passwd = "oracle_4U";

    @Test
    public void getAttribute() throws Exception {
        OIMUtils.localInitialize(hostname, port, username, passwd);
        Set<User> userList = ReconciliationUtils.getReconciledUsers("FlatFile", "20220921");
//                .filter(user -> user.getPasswordCreationDate() == null).collect(Collectors.toSet());
        userList.forEach(user -> {
            System.out.println(user.getAttributes());
        });
    }
}