package vn.bnh.oim.scheduledtasks.accounts;

import com.thortech.xl.dataaccess.tcDataProvider;
import oracle.iam.connectors.icfcommon.*;
import oracle.iam.connectors.icfcommon.util.TypeUtil;
import oracle.iam.provisioning.exception.GenericAppInstanceServiceException;
import oracle.iam.provisioning.vo.ApplicationInstance;
import oracle.iam.scheduler.vo.TaskSupport;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.*;
import vn.bnh.oim.utils.ApplicationInstanceUtils;
import vn.bnh.oim.utils.OIMUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class SmartFormUserReconciliation extends TaskSupport {
    private ConnectorFacade connectorFacade;
    private final ObjectClass objectClass = TypeUtil.convertObjectType("User");
    //    private ReconciliationService reconService;
    private ApplicationInstance applicationInstance;
    IResourceConfig resourceConfig;
    private HashMap<String, Object> params;
    private ResultsHandler handler;

    @Override
    public void execute(HashMap params) throws Exception {
        this.params = params;
        this.setAttributes();
        connectorFacade.search(this.objectClass, null, this.handler, this.buildAttributesToGet());

    }

    @Override
    public HashMap getAttributes() {
        return null;
    }

    @Override
    public void setAttributes() {
        try {
            tcDataProvider dbProvider = OIMUtils.getTcDataProvider();
            this.resourceConfig = ResourceConfigFactory.getResourceConfig(this.params.get("Application Name").toString(), dbProvider);
            this.applicationInstance = ApplicationInstanceUtils.getApplicationInstance(this.params.get("Application Name").toString());
            this.connectorFacade = ConnectorFactory.createConnectorFacade(resourceConfig);
            this.handler = new CustomResultsHandler();
        } catch (GenericAppInstanceServiceException e) {
            throw new RuntimeException(e);
        }
//        this.reconService = (ReconciliationService) ServiceFactory.getService(ReconciliationService.class, new Object[]{this.getDataBase()});

    }

    private OperationOptions buildAttributesToGet() {
        Lookup reconMapping = this.resourceConfig.getObjectTypeLookup("User", "Recon Attribute Map", null);
        OperationOptionsBuilder oobuilder = new OperationOptionsBuilder();
        Collection<String> fullValues = reconMapping.toMap().values();
        Collection<String> atts = new HashSet();

        for (String decode : fullValues) {
            FieldMapping mapping = new FieldMapping("", decode);
            atts.add(mapping.getAttributeName());
        }
        oobuilder.setAttributesToGet(atts);
        this.addCustomParams("Attributes List", new HashSet(atts));
        return oobuilder.build();
    }

    private class CustomResultsHandler implements ResultsHandler {
        @Override
        public boolean handle(ConnectorObject connectorObject) {
            System.out.printf("[%s] connector object: %s%n", this.getClass().getCanonicalName(), connectorObject);
            return false;
        }
    }
}
