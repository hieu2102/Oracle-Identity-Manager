import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcTaskNotFoundException;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.provisioning.exception.GenericProvisioningException;
import oracle.iam.provisioning.exception.UserNotFoundException;
import org.junit.Test;
import vn.bnh.oim.utils.ApplicationInstanceUtil;
import vn.bnh.oim.utils.OIMUtil;
import vn.bnh.oim.utils.UserUtil;

import javax.security.auth.login.LoginException;

public class OIMUtilTest {
    private final ODLLogger logger = ODLLogger.getODLLogger(OIMUtil.class.getName());

    @Test
    public void testConnect() throws LoginException {
        OIMUtil oim = new OIMUtil("10.10.11.54", "14000", "xelsysadm", "oracle_4U");
    }

    @Test
    public void testGetUser() throws UserLookupException, NoSuchUserException, LoginException, UserNotFoundException, GenericProvisioningException {
        OIMUtil oim = new OIMUtil("10.10.11.54", "14000", "xelsysadm", "oracle_4U");
        User user = UserUtil.getUser("USER@ORACLE.COM");
        assert "USER@ORACLE.COM".equals(user.getLogin());
        ApplicationInstanceUtil.getProvisioningAccountsForUser(user, "TRM").forEach(System.out::println);
    }

    @Test
    public void testGetUserAccounts() throws UserLookupException, NoSuchUserException, LoginException, UserNotFoundException, GenericProvisioningException {
        OIMUtil oim = new OIMUtil("10.10.11.54", "14000", "xelsysadm", "oracle_4U");
        User user = UserUtil.getUser("USER@ORACLE.COM");
        ApplicationInstanceUtil.getProvisioningAccountsForUser(user, "TRM").forEach(x -> {
            try {
                ApplicationInstanceUtil.retryAccountProvision(x);
            } catch (tcAPIException | tcColumnNotFoundException | tcTaskNotFoundException e) {
                throw new RuntimeException(e);
            }
            assert "TRM".equals(x.getAppInstance().getApplicationInstanceName());
        });

    }
}
