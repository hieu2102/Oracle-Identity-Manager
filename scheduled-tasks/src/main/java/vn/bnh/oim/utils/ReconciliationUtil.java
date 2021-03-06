package vn.bnh.oim.utils;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcInvalidLookupException;
import Thor.API.Exceptions.tcInvalidValueException;
import oracle.iam.reconciliation.api.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ReconciliationUtil {
    private static final ReconOperationsService reconOperationsService = OIMUtil.getService(ReconOperationsService.class);

    public static void reconcileRoles(
            String lookupTableName,
            JSONObject inputData,
            String jsonResourceTag,
            String displayNameField
    ) throws tcInvalidLookupException, tcInvalidValueException, tcAPIException, tcColumnNotFoundException {
        JSONArray payload = inputData.getJSONArray(jsonResourceTag);
        HashMap<String, String> reconData = new HashMap<>();
        for (int i = 0; i < payload.length(); i++) {
            JSONObject roleData = payload.getJSONObject(i);
            String key = null;
            String value = null;
            for (String fieldName : roleData.keySet()) {
                if (fieldName.equals(displayNameField)) {
                    value = roleData.getString(fieldName);
                } else {
                    key = roleData.getString(fieldName);
                }
            }
            reconData.put(key, value);
        }
        LookupUtil.updateLookupTable(lookupTableName, reconData);
    }

    public static ReconciliationResult batchReconcileAccount(
            String resourceObjName,
            JSONObject inputData,
            String jsonResourceTag,
            String childFieldName
    ) {
        JSONArray payload = inputData.getJSONArray(jsonResourceTag);

        InputData[] input = new InputData[payload.length()];
        for (int i = 0; i < input.length; i++) {
            JSONObject accountData = payload.getJSONObject(i);
            JSONArray childDatas = accountData.getJSONArray(childFieldName);
            HashMap reconData = new HashMap();
            accountData.keySet().forEach(fieldName -> {
                if (!fieldName.equals(childFieldName)) {
                    reconData.put(fieldName, accountData.get(fieldName));
                }
            });
            List childFormRows = new ArrayList();
            Map childFormsReconData = new HashMap();
            childFormsReconData.put(childFieldName, childFormRows);
            for (int j = 0; j < childDatas.length(); j++) {
                Map row = new HashMap();
                JSONObject childData = childDatas.getJSONObject(j);
                childData.keySet().forEach(x -> {
                    row.put(x, childData.get(x));
                });
                childFormRows.add(row);
            }
            input[i] = new InputData(reconData, childFormsReconData, true, ChangeType.CHANGELOG, null);
        }
        BatchAttributes batchAttributes = new BatchAttributes(resourceObjName, "yyyy/MM/dd hh:mm:ss z");
//
        return reconOperationsService.createReconciliationEvents(batchAttributes, input);

    }
}
