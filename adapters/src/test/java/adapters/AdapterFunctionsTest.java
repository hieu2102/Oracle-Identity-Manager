package adapters;

import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.provisioning.api.ProvisioningConstants;
import oracle.iam.provisioning.exception.GenericProvisioningException;
import oracle.iam.provisioning.exception.UserNotFoundException;
import oracle.iam.provisioning.vo.Account;
import org.junit.Test;
import vn.bnh.oim.utils.ApplicationInstanceUtil;
import vn.bnh.oim.utils.OIMUtil;

import javax.security.auth.login.LoginException;
import java.util.Set;

public class AdapterFunctionsTest {
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String passwd = "oracle_4U";

    public AdapterFunctionsTest() {
        try {
            OIMUtil.localInitialize(this.hostname, this.port, this.username, this.passwd);
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getAccount() throws UserNotFoundException, UserLookupException, GenericProvisioningException, NoSuchUserException {
        String userLogin = "MINHDUCTEST01";
        String appInstName = "FlexCash";
        Set<Account> accounts = ApplicationInstanceUtil.getAccountsForUser(userLogin, appInstName, ProvisioningConstants.ObjectStatus.PROVISIONED);
        accounts.forEach(x -> {
            System.out.println(x.getAccountData().getChildData());
            System.out.println(x.getProcessInstanceKey());
            System.out.println(x.getAccountID());
            System.out.println(x.getAccountData().getData());
        });

    }
}
