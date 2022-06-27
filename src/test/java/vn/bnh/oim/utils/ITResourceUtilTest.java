package vn.bnh.oim.utils;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcITResourceNotFoundException;
import org.junit.Test;

import javax.security.auth.login.LoginException;
import java.util.HashMap;

public class ITResourceUtilTest {
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String passwd = "oracle_4U";

    @Test
    public void getITRes() throws LoginException, tcAPIException, tcColumnNotFoundException, tcITResourceNotFoundException {
        OIMUtil.localInitialize(hostname, port, username, passwd);
        HashMap<String, String> itres = ITResourceUtil.getITResource("FlexCash");
        itres.forEach((key, value) -> System.out.printf("%s=%s%n", key, value));

    }
}