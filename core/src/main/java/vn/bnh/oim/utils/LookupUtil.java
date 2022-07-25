package vn.bnh.oim.utils;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcInvalidLookupException;
import Thor.API.Exceptions.tcInvalidValueException;
import Thor.API.Operations.tcLookupOperationsIntf;
import Thor.API.tcResultSet;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;


public class LookupUtil {
    static tcLookupOperationsIntf tcLookupOperationsIntf = OIMUtil.tcLookupOperationsIntf;
    private static final ODLLogger logger = ODLLogger.getODLLogger(LookupUtil.class.getName());

    public static void setLookupValue(String lookupTable, String code, String meaning) throws tcInvalidLookupException, tcInvalidValueException, tcAPIException {
        tcLookupOperationsIntf.addLookupValue(lookupTable, code, meaning, "en", "US");
    }

    public static void removeLookupEntryByKey(String lookupTable, String code) throws tcInvalidLookupException, tcInvalidValueException, tcAPIException {
        tcLookupOperationsIntf.removeLookupValue(lookupTable, code);
    }

    public static void removeLookupEntryByValue(String lookupTable, String meaning) throws tcInvalidLookupException, tcAPIException, tcColumnNotFoundException, tcInvalidValueException {
        tcResultSet lookupCodeSet = tcLookupOperationsIntf.getLookupValues(lookupTable);
        String lookupCode = null;
        if (lookupCodeSet.getTotalRowCount() > 0) {
            for (int i = 0; i < lookupCodeSet.getTotalRowCount(); i++) {
                lookupCodeSet.goToRow(i);
                if (meaning.equals(lookupCodeSet.getStringValue("Lookup Definition.Lookup Code Information.Decode"))) {
                    lookupCode = lookupCodeSet.getStringValue("Lookup Definition.Lookup Code Information.Code Key");
                    break;
                }
            }
        }
        removeLookupEntryByKey(lookupTable, lookupCode);
    }

    public static String getLookupValue(String lookupTable, String code) {
        String output = null;
        try {
            output = tcLookupOperationsIntf.getDecodedValueForEncodedValue(lookupTable, code);
        } catch (tcAPIException e) {
            logger.log(ODLLevel.WARNING, "Lookup Code {0} not found in lookup table {1}", new Object[]{code, lookupTable});
        }
        return output;
    }
}
