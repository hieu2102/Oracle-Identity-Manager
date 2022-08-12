package vn.bnh.oim.utils;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcITResourceNotFoundException;
import Thor.API.Operations.tcITResourceInstanceOperationsIntf;
import Thor.API.tcResultSet;

import java.util.HashMap;

public class ITResourceUtil {
    static tcITResourceInstanceOperationsIntf tcITResourceInstanceOperationsIntf = OIMUtil.getService(tcITResourceInstanceOperationsIntf.class);
    private static final String ITRES_KEY_FIELD = "IT Resources Type Parameter.Name";
    private static final String ITRES_VALUE_FIELD = "IT Resources Type Parameter Value.Value";

    public static HashMap<String, String> getITResource(String itResName) throws tcAPIException, tcColumnNotFoundException, tcITResourceNotFoundException {
        HashMap<String, String> output = new HashMap<>();
        HashMap<String, String> searchcriteria = new HashMap<>();
        searchcriteria.put("IT Resources.Name", itResName);
        tcResultSet resultSet = tcITResourceInstanceOperationsIntf.findITResourceInstances(searchcriteria);
        resultSet = tcITResourceInstanceOperationsIntf.getITResourceInstanceParameters(resultSet.getLongValue("IT Resources.Key"));
        for (int i = 0; i < resultSet.getRowCount(); i++) {
            resultSet.goToRow(i);
            output.put(resultSet.getStringValue(ITRES_KEY_FIELD), resultSet.getStringValue(ITRES_VALUE_FIELD));
        }
        return output;
    }
}
