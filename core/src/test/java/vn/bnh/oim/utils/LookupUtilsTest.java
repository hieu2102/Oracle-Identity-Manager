package vn.bnh.oim.utils;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcInvalidLookupException;
import Thor.API.Exceptions.tcInvalidValueException;
import org.junit.Test;

import javax.security.auth.login.LoginException;

public class LookupUtilsTest {
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String passwd = "oracle_4U";
    String lookupTable = "Lookup.GenericRest.Groups";
    String lookupKey = "key";
    String lookupValue = "value";

    @Test
    public void addLookupValue() throws Exception {
        OIMUtils.localInitialize(hostname, port, username, passwd);
        LookupUtils.setLookupValue("Lookup.GenericRest.Groups", "key", "value");
    }

    @Test
    public void getLookupValue() throws LoginException {
        OIMUtils.localInitialize(hostname, port, username, passwd);
        String output = LookupUtils.getLookupValue(lookupTable, lookupKey);
    }

    @Test
    public void removeLookupValue() throws LoginException, tcInvalidLookupException, tcInvalidValueException, tcAPIException {
        OIMUtils.localInitialize(hostname, port, username, passwd);
        LookupUtils.removeLookupEntryByKey("Lookup.GenericRest.Groups", "key");
    }

    @Test
    public void removeLookupValueByMeaning() throws LoginException, tcInvalidLookupException, tcAPIException, tcColumnNotFoundException, tcInvalidValueException {
        OIMUtils.localInitialize(hostname, port, username, passwd);
        LookupUtils.removeLookupEntryByValue(lookupTable, lookupValue);
    }

    @Test
    public void testParseDate() {
        String date = "20220819";
        System.out.println(date.substring(0, 4));
        System.out.println(date.substring(4, 6));
        System.out.println(date.substring(6));
    }
}