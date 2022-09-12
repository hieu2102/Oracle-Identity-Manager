package vn.bnh.oim.utils;

import com.fasterxml.jackson.databind.JsonNode;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.reconciliation.api.*;
import oracle.iam.reconciliation.vo.*;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ReconciliationUtils {
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(ReconciliationUtils.class.getName());
    private static final ReconOperationsService reconOperationsService = OIMUtils.getService(ReconOperationsService.class);
    private static final EventMgmtService eventMgmtService = OIMUtils.getService(EventMgmtService.class);

    public static ReconSearchCriteria generateSearchCriteria(
            String resourceObjName,
            String eventStatus,
            String fromDate
    ) {

        Calendar date = new GregorianCalendar();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        if (fromDate != null) {
            date.set(Calendar.YEAR, Integer.parseInt(fromDate.substring(0, 4)));
            date.set(Calendar.MONTH, Integer.parseInt(fromDate.substring(4, 6)));
            date.set(Calendar.DATE, Integer.parseInt(fromDate.substring(6)));
        }
        ReconSearchCriteria sc = new ReconSearchCriteria(ReconSearchCriteria.Operator.AND);
        sc.addExpression(EventConstants.RECON_EVENT_RSRC_NAME, resourceObjName, ReconSearchCriteria.Operator.EQUAL);
        sc.addExpression(EventConstants.RECON_EVENT_STATUS, eventStatus, ReconSearchCriteria.Operator.EQUAL);
        sc.addExpression(EventConstants.RECON_EVENT_CREATETIMESTMP, date.getTime(), ReconSearchCriteria.Operator.GREATER_THAN);
        LOGGER.log(ODLLevel.INFO, "Generate Search Criteria for {0}, event status: {1}, time: {2}", new Object[]{resourceObjName, eventStatus, date.getTime()});
        return sc;
    }

    public static Set<User> getReconciledUsers(String resourceName, String fromDate) {
        Vector resultOrder = new Vector<>();
        resultOrder.add(EventConstants.RECON_EVENT_KEY);
        ReconSearchCriteria searchCreated = generateSearchCriteria(resourceName, EventConstants.STATUS_CREATE_SUCCESS, fromDate);
        List<ReconEvent> createdEvents = eventMgmtService.search(searchCreated, resultOrder, false, 0, 100);
        return createdEvents.stream().map(event -> eventMgmtService.getLinkedUserForEvent(event.getReKey())).collect(Collectors.toSet());
    }

    public static Set<Account> getReconciliationEvents(String resourceObjName, String fromDate) {
        LOGGER.log(ODLLevel.INFO, "Get Reconciliation Events for {0}", resourceObjName);
        Vector resultOrder = new Vector<>();
        resultOrder.add(EventConstants.RECON_EVENT_KEY);
        ReconSearchCriteria searchCreated = generateSearchCriteria(resourceObjName, EventConstants.STATUS_CREATE_SUCCESS, fromDate);
        List<ReconEvent> createdEvents = eventMgmtService.search(searchCreated, resultOrder, false, 0, 100);
        LOGGER.log(ODLLevel.INFO, "Reconciliation CREATE Events: {0}", createdEvents.size());
        ReconSearchCriteria searchUpdated = generateSearchCriteria(resourceObjName, EventConstants.STATUS_UPDATE_SUCCESS, fromDate);
        List<ReconEvent> updatedEvents = eventMgmtService.search(searchUpdated, resultOrder, false, 0, 100);
        LOGGER.log(ODLLevel.INFO, "Reconciliation UPDATE Events: {0}", updatedEvents.size());

//        merge list
        createdEvents.addAll(updatedEvents);
        return createdEvents.stream().map(event -> eventMgmtService.getLinkedAccountForEvent(event.getReKey())).collect(Collectors.toSet());
    }

    public static void reconcileAccount(String resourceObjName, JsonNode accountData) {
        EventAttributes attrs = new EventAttributes();
        Map<String, Object> reconData = new HashMap<>();
        accountData.fieldNames().forEachRemaining(field -> {
            reconData.put(field, accountData.get(field).toString().replaceAll("\"", ""));
        });

        reconOperationsService.createReconciliationEvent(resourceObjName, reconData, attrs);
    }

    public static ReconciliationResult batchReconcileAccount(String resourceObjName, JsonNode data) {
        InputData[] input = new InputData[data.size()];
        for (int i = 0; i < input.length; i++) {
            JsonNode accountData = data.get(i);
            Map reconData = new HashMap();
            accountData.fieldNames().forEachRemaining(field -> {
                reconData.put(field, accountData.get(field).toString().replaceAll("\"", ""));
            });
            reconData.put("OrgName", "Xellerate Users");
            LOGGER.log(ODLLevel.INFO, "Reconciliation Data: {0}", reconData);
            input[i] = new InputData(reconData, ChangeType.CHANGELOG, null);
        }
        BatchAttributes batchAttributes = new BatchAttributes(resourceObjName, "yyyy/MM/dd hh:mm:ss z");
        return reconOperationsService.createReconciliationEvents(batchAttributes, input);
    }

}
