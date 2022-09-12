package vn.bnh.oim.utils;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcInvalidLookupException;
import Thor.API.Exceptions.tcInvalidValueException;
import Thor.API.Operations.tcLookupOperationsIntf;
import Thor.API.tcResultSet;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;

import java.util.HashMap;
import java.util.Map;


public class LookupUtils {
    static tcLookupOperationsIntf tcLookupOperationsIntf = OIMUtils.getService(tcLookupOperationsIntf.class);
    private static final ODLLogger logger = ODLLogger.getODLLogger(LookupUtils.class.getName());

    public static void setLookupValue(
            String lookupTable,
            String code,
            String meaning
    ) throws tcInvalidLookupException, tcInvalidValueException, tcAPIException {
        tcLookupOperationsIntf.addLookupValue(lookupTable, code, meaning, "en", "US");
    }

    public static void removeLookupEntryByKey(
            String lookupTable,
            String code
    ) throws tcInvalidLookupException, tcInvalidValueException, tcAPIException {
        tcLookupOperationsIntf.removeLookupValue(lookupTable, code);
    }

    public static void removeLookupEntryByValue(
            String lookupTable,
            String meaning
    ) throws tcInvalidLookupException, tcAPIException, tcColumnNotFoundException, tcInvalidValueException {
        String lookupCode = getLookupCode(lookupTable, meaning);
        removeLookupEntryByKey(lookupTable, lookupCode);
    }

    public static tcResultSet getLookupValues(String lookupTable) throws tcInvalidLookupException, tcAPIException {
        return tcLookupOperationsIntf.getLookupValues(lookupTable);
    }

    public static void updateLookupValue(
            String lookupTable,
            String lookupKey,
            String newValue
    ) throws tcInvalidLookupException, tcInvalidValueException, tcAPIException {
        tcLookupOperationsIntf.updateLookupValue(lookupTable, lookupKey, "", newValue, "", "");
    }

    public static void updateLookupTable(
            String lookupTable,
            HashMap<String, String> entries
    ) throws tcInvalidLookupException, tcAPIException, tcColumnNotFoundException, tcInvalidValueException {
//        get existing lookup entries
        tcResultSet lookupCodeSet = getLookupValues(lookupTable);
        HashMap<String, String> existingEntries = new HashMap<>();
        if (lookupCodeSet.getTotalRowCount() > 0) {
            for (int i = 0; i < lookupCodeSet.getTotalRowCount(); i++) {
                lookupCodeSet.goToRow(i);
                String lookupKey = lookupCodeSet.getStringValue("Lookup Definition.Lookup Code Information.Code Key");
                String lookupValue = lookupCodeSet.getStringValue("Lookup Definition.Lookup Code Information.Decode");
                existingEntries.put(lookupKey, lookupValue);
            }
        }
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (existingEntries.containsKey(key) && !existingEntries.get(key).equals(value)) {
//                update
                updateLookupValue(lookupTable, key, value);
            } else {
//                insert
                setLookupValue(lookupTable, key, value);
            }

        }
    }

    public static String getLookupCode(String lookupTable, String meaning) throws tcInvalidLookupException, tcAPIException, tcColumnNotFoundException {
        tcResultSet lookupCodeSet = getLookupValues(lookupTable);
        if (lookupCodeSet.getTotalRowCount() > 0) {
            for (int i = 0; i < lookupCodeSet.getTotalRowCount(); i++) {
                lookupCodeSet.goToRow(i);
                logger.log(ODLLevel.INFO, "Lookup Entry: {0}", lookupCodeSet.getStringValue("Lookup Definition.Lookup Code Information.Decode"));
                if (meaning.equals(lookupCodeSet.getStringValue("Lookup Definition.Lookup Code Information.Decode"))) {
                    return lookupCodeSet.getStringValue("Lookup Definition.Lookup Code Information.Code Key");
                }
            }
        }
        return null;
    }

    public static String getLookupValue(
            String lookupTable,
            String code
    ) {
        String output = null;
        try {
            output = tcLookupOperationsIntf.getDecodedValueForEncodedValue(lookupTable, code);
        } catch (tcAPIException e) {
            logger.log(ODLLevel.WARNING, "Lookup Code {0} not found in lookup table {1}", new Object[]{code, lookupTable});
        }
        return output;
    }
}
