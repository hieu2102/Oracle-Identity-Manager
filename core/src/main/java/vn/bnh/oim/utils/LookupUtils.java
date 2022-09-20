package vn.bnh.oim.utils;

import Thor.API.Exceptions.*;
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

    public static void createLookupTable(String lookupTable) throws tcAPIException, tcDuplicateLookupCodeException {
        try {
            getLookupValues(lookupTable);
        } catch (tcInvalidLookupException e) {
            tcLookupOperationsIntf.addLookupCode(lookupTable);
        } catch (tcColumnNotFoundException e) {
            throw new RuntimeException(e);
        }
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

    public static HashMap<String, String> getLookupValues(String lookupTable) throws tcInvalidLookupException, tcAPIException, tcColumnNotFoundException {
        tcResultSet lookupCodeSet = tcLookupOperationsIntf.getLookupValues(lookupTable);
        HashMap<String, String> existingEntries = new HashMap<>();
        if (lookupCodeSet.getTotalRowCount() > 0) {
            for (int i = 0; i < lookupCodeSet.getTotalRowCount(); i++) {
                lookupCodeSet.goToRow(i);
                String lookupKey = lookupCodeSet.getStringValue("Lookup Definition.Lookup Code Information.Code Key");
                String lookupValue = lookupCodeSet.getStringValue("Lookup Definition.Lookup Code Information.Decode");
                existingEntries.put(lookupKey, lookupValue);

            }
        }
        return existingEntries;
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
        HashMap<String, String> existingEntries = getLookupValues(lookupTable);
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

    public static String getLookupCode(
            String lookupTable,
            String meaning
    ) throws tcInvalidLookupException, tcAPIException, tcColumnNotFoundException {
        for (Map.Entry<String, String> et : getLookupValues(lookupTable).entrySet()) {
            if (et.getValue().equals(meaning)) {
                return et.getKey();
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
