package vn.bnh.oim.scheduledtasks.accounts;

import com.thortech.xl.dataaccess.tcDataProvider;
import oracle.iam.connectors.icfcommon.*;
import oracle.iam.connectors.icfcommon.recon.ReconEvent;
import oracle.iam.connectors.icfcommon.service.ReconciliationService;
import oracle.iam.connectors.icfcommon.service.ServiceFactory;
import oracle.iam.connectors.icfcommon.util.TypeUtil;
import oracle.iam.provisioning.vo.ApplicationInstance;
import oracle.iam.scheduler.vo.TaskSupport;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.*;
import vn.bnh.oim.utils.ApplicationInstanceUtils;
import vn.bnh.oim.utils.OIMUtils;

import java.util.*;
import java.util.stream.Collectors;

public class SmartFormUserReconciliation extends TaskSupport {
    private ConnectorFacade connectorFacade;
    private final ObjectClass objectClass = TypeUtil.convertObjectType("User");
    private ApplicationInstance applicationInstance;
    private HashMap<String, Object> params;
    private ResultsHandler handler;
    private final tcDataProvider dataProvider = OIMUtils.getTcDataProvider();
    private final ObjectClass userRoleObjectClass = TypeUtil.convertObjectType("__ACCOUNT__~RoleIds");
    private Lookup reconFieldMapping;
    private IResourceConfig resourceConfig;
    private ReconciliationService reconService;
    private ReconciliationService.BatchReconciliationService batchReconService;

    @Override
    public void execute(HashMap params) throws Exception {
        this.params = params;
        this.setAttributes();
        System.out.printf("[%s] execute __ACCOUNT__.SEARCHOP%n", this.getClass().getCanonicalName());
        connectorFacade.search(this.objectClass, null, this.handler, this.buildAttributesToGet());
        System.out.printf("[%s] end __ACCOUNT__.SEARCHOP%n", this.getClass().getCanonicalName());
        System.out.printf("[%s] execute reconciliation%n", this.getClass().getCanonicalName());
        this.batchReconService.finish();

    }

    @Override
    public HashMap getAttributes() {
        return null;
    }

    @Override
    public void setAttributes() {
        try {
            String applicationName = this.params.get("Application Name").toString();
            this.applicationInstance = ApplicationInstanceUtils.getApplicationInstance(applicationName);
            this.resourceConfig = ResourceConfigFactory.getResourceConfig(this.applicationInstance.getItResourceName(), this.applicationInstance.getObjectName(), applicationName, this.dataProvider);
            this.connectorFacade = ConnectorFactory.createConnectorFacade(resourceConfig);
            this.reconFieldMapping = resourceConfig.getObjectTypeLookup("User", "Recon Attribute Map", null);
            this.reconService = ServiceFactory.getService(ReconciliationService.class, this.dataProvider);
            this.batchReconService = this.reconService.createBatchReconciliationService(applicationName, resourceConfig.getReconBatchSize(), resourceConfig.isIgnoreEventDisabled(), resourceConfig.getReconThreadPoolConfig(), resourceConfig.getProcessReconEventTimeOut());
            this.handler = new UserResultsHandler(batchReconService);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private OperationOptions buildAttributesToGet() {
        OperationOptionsBuilder oobuilder = new OperationOptionsBuilder();
        Collection<String> fullValues = this.reconFieldMapping.toMap().values();
        Collection<String> atts = new HashSet();
        Iterator var5 = fullValues.iterator();

        while (var5.hasNext()) {
            String decode = (String) var5.next();
            FieldMapping mapping = new FieldMapping("", decode);
            atts.add(mapping.getAttributeName());
        }
        oobuilder.setAttributesToGet(atts);
        this.addCustomParams("Attributes List", new HashSet(atts));
        return oobuilder.build();
    }

    private class UserResultsHandler implements ResultsHandler {
        private final ReconciliationService.BatchReconciliationService batchReconService;

        private UserResultsHandler(ReconciliationService.BatchReconciliationService batchReconService) {
            this.batchReconService = batchReconService;

        }

        @Override
        public boolean handle(ConnectorObject connectorObject) {
            try {
                Uid uid = connectorObject.getUid();
                Set<Attribute> attributes = new HashSet<>(connectorObject.getAttributes());
                attributes.remove(uid);
                System.out.printf("[%s] execute __ACCOUNT__~RoleIds.SEARCHOP%n", this.getClass().getCanonicalName());
                attributes.forEach(attribute -> {
                });
                Uid rolesList = connectorFacade.create(userRoleObjectClass, attributes.stream().filter(attr -> attr.getName().equalsIgnoreCase("__NAME__")).collect(Collectors.toSet()), buildAttributesToGet());
                System.out.printf("[%s] end __ACCOUNT__~RoleIds.SEARCHOP%n", this.getClass().getCanonicalName());
                Attribute roleAttributes = parseResponse(rolesList.getUidValue());
                attributes.add(roleAttributes);
                attributes.add(uid);
                ConnectorObject cobject = new ConnectorObject(connectorObject.getObjectClass(), attributes);
                System.out.printf("[%s] User connector object %s: %s%n", this.getClass().getCanonicalName(), cobject.getObjectClass().getObjectClassValue(), cobject);
                ReconEvent reconEvent = new ReconEvent(cobject, reconFieldMapping, resourceConfig.getITResource(), reconService.getDefaultDateFormat());
                System.out.printf("[%s] reconciliation multivalued fields: %s", this.getClass().getCanonicalName(), reconEvent.getMultiFields());
                this.batchReconService.addEvent(reconEvent);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        private Attribute parseResponse(String jsonString) {
            jsonString = jsonString.replaceAll("^\\[\\{(.+)}]$", "$1");
            AttributeBuilder roles = new AttributeBuilder();
            roles.setName("RoleId");
            Arrays.stream(jsonString.split("}\\s?,\\s?\\{")).forEach(x -> {
                String value = Arrays.stream(x.split(",\\s?")).filter(attributeValue -> attributeValue.toUpperCase().contains("ID")).collect(Collectors.toList()).get(0).replaceAll(".+=", "");
                roles.addValue(value);
            });
            return roles.build();
        }
    }
}
