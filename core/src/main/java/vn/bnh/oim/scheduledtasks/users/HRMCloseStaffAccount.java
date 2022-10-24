package vn.bnh.oim.scheduledtasks.users;

import com.thortech.xl.dataaccess.tcDataProvider;
import oracle.iam.connectors.icfcommon.*;
import oracle.iam.connectors.icfcommon.recon.ReconEvent;
import oracle.iam.connectors.icfcommon.service.ReconciliationService;
import oracle.iam.connectors.icfcommon.service.ServiceFactory;
import oracle.iam.connectors.icfcommon.util.TypeUtil;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.provisioning.vo.Account;
import oracle.iam.provisioning.vo.ApplicationInstance;
import oracle.iam.scheduler.vo.TaskSupport;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.*;
import vn.bnh.oim.utils.ApplicationInstanceUtils;
import vn.bnh.oim.utils.OIMUtils;
import vn.bnh.oim.utils.UserUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class HRMCloseStaffAccount extends TaskSupport {
    private ConnectorFacade connectorFacade;
    private ApplicationInstance applicationInstance;
    private HashMap<String, Object> params;
    private ResultsHandler handler;
    private final tcDataProvider dataProvider = OIMUtils.getTcDataProvider();
    private final ObjectClass objectClass = TypeUtil.convertObjectType("__ACCOUNT__~Close");
    private Lookup reconFieldMapping;
    private IResourceConfig resourceConfig;
    private ReconciliationService reconService;
    private ReconciliationService.BatchReconciliationService batchReconService;
    private List<String> ignoredApplicationInstances;

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

    @Override
    public void execute(HashMap hashMap) throws Exception {
        this.params = hashMap;
        this.setAttributes();
        System.out.printf("[%s] execute __ACCOUNT__~Close.SEARCHOP%n", this.getClass().getCanonicalName());
        connectorFacade.search(this.objectClass, null, this.handler, this.buildAttributesToGet());
        System.out.printf("[%s] end __ACCOUNT__~Close.SEARCHOP%n", this.getClass().getCanonicalName());
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
            this.ignoredApplicationInstances = Arrays.asList(this.params.get("Ignored Application Names").toString().split(";"));
            this.applicationInstance = ApplicationInstanceUtils.getApplicationInstance(applicationName);
            this.resourceConfig = ResourceConfigFactory.getResourceConfig(this.applicationInstance.getItResourceName(), this.applicationInstance.getObjectName(), applicationName, this.dataProvider);
            this.connectorFacade = ConnectorFactory.createConnectorFacade(resourceConfig);
            this.reconFieldMapping = resourceConfig.getObjectTypeLookup("User", "Recon Attribute Map", null);
            this.reconService = ServiceFactory.getService(ReconciliationService.class, this.dataProvider);
            this.handler = new HRMCloseStaffAccount.UserResultsHandler();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public class UserResultsHandler implements ResultsHandler {

        public UserResultsHandler() {
        }

        @Override
        public boolean handle(ConnectorObject connectorObject) {
            try {
                Set<Attribute> attributes = new HashSet<>(connectorObject.getAttributes());
                ConnectorObject cobject = new ConnectorObject(connectorObject.getObjectClass(), attributes);
                ReconEvent reconEvent = new ReconEvent(cobject, reconFieldMapping, resourceConfig.getITResource(), reconService.getDefaultDateFormat());
                Uid uid = connectorObject.getUid();
//                TODO: check EmployeeId field name
                User oimUser = UserUtils.getUser("Employee Number", uid.getUidValue());
                List<Account> userAccounts = ApplicationInstanceUtils.getUserPrimaryAccounts(oimUser.getId()).stream().filter(acc -> !ignoredApplicationInstances.contains(acc.getAppInstance().getApplicationInstanceName())).collect(Collectors.toList());
                for (Account acc : userAccounts) {
//                    TODO: add handling for application instances with special disable method (e.g: LOS)
                    ApplicationInstanceUtils.disableAccount(acc);
                }
//                set application instances enable date
                oimUser.setStartDate(new SimpleDateFormat("yyyy-MM-dd").parse(reconEvent.getSingleFields().get("ReturnToWorkDate")));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

    }
}
