package vn.bnh.oim.scheduledtasks.users;

import com.thortech.xl.dataaccess.tcDataProvider;
import oracle.iam.connectors.icfcommon.*;
import oracle.iam.connectors.icfcommon.extension.Transformation;
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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class HRMAllUserReconTask extends TaskSupport {
    private HashMap<String, Object> params;
    private ConnectorFacade connectorFacade;
    private final ObjectClass objectClass = TypeUtil.convertObjectType("User");
    private final ObjectClass searchOpObjectClass = TypeUtil.convertObjectType("User~GetAll");
    private ApplicationInstance applicationInstance;
    private ResultsHandler handler;
    private final tcDataProvider dataProvider = OIMUtils.getTcDataProvider();
    private Lookup reconFieldMapping;
    private IResourceConfig resourceConfig;
    private ReconciliationService reconService;
    private ReconciliationService.BatchReconciliationService batchReconService;

    @Override
    public void execute(HashMap hashMap) throws Exception {
        this.params = hashMap;
        this.setAttributes();
        connectorFacade.search(this.searchOpObjectClass, null, this.handler, this.buildAttributesToGet());
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
            this.reconFieldMapping = resourceConfig.getObjectTypeLookup(objectClass.getObjectClassValue(), "Recon Attribute Map", null);
            this.reconService = ServiceFactory.getService(ReconciliationService.class, this.dataProvider);
            this.batchReconService = this.reconService.createBatchReconciliationService(applicationName, resourceConfig.getReconBatchSize(), resourceConfig.isIgnoreEventDisabled(), resourceConfig.getReconThreadPoolConfig(), resourceConfig.getProcessReconEventTimeOut());
            this.handler = new TargetResultHandler(batchReconService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private OperationOptions buildAttributesToGet() {
        OperationOptionsBuilder oOBuilder = new OperationOptionsBuilder();
        Collection<String> fullValues = this.reconFieldMapping.toMap().values();
        Collection<String> attrs = new HashSet();
        for (String decode : fullValues) {
            FieldMapping mapping = new FieldMapping("", decode);
            attrs.add(mapping.getAttributeName());
        }
        oOBuilder.setAttributesToGet(attrs);
        this.addCustomParams("Attributes List", new HashSet(attrs));
        return oOBuilder.build();
    }

    private class TargetResultHandler implements ResultsHandler {
        private final ReconciliationService.BatchReconciliationService batchReconService;

        public TargetResultHandler(ReconciliationService.BatchReconciliationService batchReconService) {
            this.batchReconService = batchReconService;
        }

        public boolean handle(ConnectorObject obj) {
            try {
                this.doHandle(obj);
            } catch (Exception var3) {
                return false;
            }
            return true;
        }

        private void doHandle(ConnectorObject cobject) {
            Map<String, Serializable> params = new HashMap();
            params.put("UID", cobject.getUid().getUidValue());
            ReconEvent reconEvent = new ReconEvent(cobject, HRMAllUserReconTask.this.reconFieldMapping, HRMAllUserReconTask.this.resourceConfig.getITResource(), HRMAllUserReconTask.this.reconService.getDefaultDateFormat());
            params.put("Parent Fields", new HashMap(reconEvent.getSingleFields()));
            params.put("MultiValued Data", new HashMap(reconEvent.getMultiFields()));
            Transformation.newInstance(HRMAllUserReconTask.this.objectClass.getObjectClassValue(), HRMAllUserReconTask.this.resourceConfig).transform(reconEvent);
            this.batchReconService.addEvent(reconEvent);
        }

    }
}
