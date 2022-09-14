package vn.bnh.oim.scheduledtasks.accounts;

import com.thortech.xl.dataaccess.tcDataProvider;
import oracle.iam.connectors.icfcommon.FieldMapping;
import oracle.iam.connectors.icfcommon.ITResource;
import oracle.iam.connectors.icfcommon.service.ConfigurationService;
import oracle.iam.connectors.icfcommon.service.ServiceFactory;
import oracle.iam.connectors.icfcommon.util.MapUtil;
import oracle.iam.connectors.icfcommon.util.TypeUtil;
import oracle.iam.provisioning.vo.ApplicationInstance;
import oracle.iam.provisioning.vo.FormField;
import oracle.iam.scheduler.vo.TaskSupport;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.*;
import org.identityconnectors.framework.common.objects.*;
import vn.bnh.oim.utils.ApplicationInstanceUtils;
import vn.bnh.oim.utils.OIMUtils;

import java.util.*;
import java.util.stream.Collectors;

public class SmartFormUserReconciliation extends TaskSupport {
    private ConnectorFacade connectorFacade;
    private final ObjectClass objectClass = TypeUtil.convertObjectType("User");
    //    private ReconciliationService reconService;
    private ApplicationInstance applicationInstance;
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
            String applicationName = this.params.get("Application Name").toString();
            this.applicationInstance = ApplicationInstanceUtils.getApplicationInstance(applicationName);
            tcDataProvider dbProvider = OIMUtils.getTcDataProvider();
            ConfigurationService configService = ServiceFactory.getService(ConfigurationService.class, dbProvider);
            ITResource objITResource = configService.getITResource(applicationName);
            ITResource conServer = configService.getITResource(objITResource.getValue("Connector Server Name"));
            Map<String, String> resourceDetails = conServer.toMap();
            String bundleName = resourceDetails.get("Host");
            int port = Integer.parseInt(resourceDetails.get("Port"));
            GuardedString key = new GuardedString(MapUtil.getRequiredValue(resourceDetails, "Key").toCharArray());
            boolean useSSL = Boolean.parseBoolean(resourceDetails.get("UseSSL"));
            int timeout = TypeUtil.convertValueType(MapUtil.getValue(resourceDetails, "Timeout", "0"), Integer.class);
            RemoteFrameworkConnectionInfo remoteInfo = new RemoteFrameworkConnectionInfo(bundleName, port, key, useSSL, null, timeout);
            ConnectorInfoManager infoManager = ConnectorInfoManagerFactory.getInstance().getRemoteManager(remoteInfo);
            ConnectorKey connectorKey = new ConnectorKey("org.identityconnectors.genericrest", "12.3.0", "org.identityconnectors.genericrest.GenericRESTConnector");
            ConnectorInfo connectorInfo = infoManager.findConnectorInfo(connectorKey);
            APIConfiguration config = connectorInfo.createDefaultAPIConfiguration();
            this.connectorFacade = ConnectorFacadeFactory.getInstance().newInstance(config);
            this.handler = new CustomResultsHandler();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private OperationOptions buildAttributesToGet() {
        OperationOptionsBuilder oobuilder = new OperationOptionsBuilder();
        Set<String> fullValues = this.applicationInstance.getAccountForm().getFormFields().stream().map(FormField::getLabel).collect(Collectors.toSet());
        Collection<String> atts = new HashSet();
//
        for (String decode : fullValues) {
            FieldMapping mapping = new FieldMapping("", decode.replaceAll("\\s", ""));
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
