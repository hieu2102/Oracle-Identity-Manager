package vn.bnh.oim.utils;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcInvalidLookupException;
import Thor.API.Exceptions.tcInvalidValueException;
import org.junit.Test;

import javax.security.auth.login.LoginException;

public class LookupUtilTest {
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String passwd = "oracle_4U";
    String lookupTable = "Lookup.GenericRest.Groups";
    String lookupKey = "key";
    String lookupValue = "value";

    @Test
    public void addLookupValue() throws Exception {
        OIMUtil.localInitialize(hostname, port, username, passwd);
        LookupUtil.setLookupValue("Lookup.GenericRest.Groups", "key", "value");
    }

    @Test
    public void getLookupValue() throws LoginException {
        OIMUtil.localInitialize(hostname, port, username, passwd);
        String output = LookupUtil.getLookupValue(lookupTable, lookupKey);
    }

    @Test
    public void removeLookupValue() throws LoginException, tcInvalidLookupException, tcInvalidValueException, tcAPIException {
        OIMUtil.localInitialize(hostname, port, username, passwd);
        LookupUtil.removeLookupEntryByKey("Lookup.GenericRest.Groups", "key");
    }

    @Test
    public void removeLookupValueByMeaning() throws LoginException, tcInvalidLookupException, tcAPIException, tcColumnNotFoundException, tcInvalidValueException {
        OIMUtil.localInitialize(hostname, port, username, passwd);
        LookupUtil.removeLookupEntryByValue(lookupTable, lookupValue);
    }
}