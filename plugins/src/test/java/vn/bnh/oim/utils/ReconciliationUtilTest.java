package vn.bnh.oim.utils;


import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.reconciliation.vo.Account;
import org.junit.jupiter.api.Test;
import vn.bnh.oim.scheduledtasks.GrantO365LicenseByTitle;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Set;

public class ReconciliationUtilTest {
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String passwd = "oracle_4U";
    String resourceObjName = "Office365 User";

    @Test
    public void testGrantLicense() throws Exception {
        OIMUtil.localInitialize(hostname, port, username, passwd);
        HashMap<String, Object> params = new HashMap<>();
        params.put("License Matrix Lookup Table", "Lookup.ABB.Office365.Lisence.Matrix");
        params.put("Resource Object Name", "Office365 User");
        GrantO365LicenseByTitle st = new GrantO365LicenseByTitle();
        st.execute(params);
    }

    @Test
    public void listReconEvents() throws LoginException {
        OIMUtil.localInitialize(hostname, port, username, passwd);
        Set<Account> accs = ReconciliationUtil.getReconciliationEvents(resourceObjName);
        accs.stream().map(x -> {
            try {
                return UserUtil.getUser(x.getOwnerName());
            } catch (UserLookupException |
                     NoSuchUserException e) {
                throw new RuntimeException(e);
            }
        }).forEach(System.out::println);
//        eventsCreate.addAll(eventsUpdate);
    }
}