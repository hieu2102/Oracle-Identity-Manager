package vn.bnh.oim.utils;

import Thor.API.Operations.*;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.orgmgmt.api.OrganizationManager;
import oracle.iam.identity.rolemgmt.api.RoleManager;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.Platform;
import oracle.iam.platformservice.api.PlatformService;
import oracle.iam.platformservice.api.PlatformUtilsService;
import oracle.iam.provisioning.api.ApplicationInstanceService;
import oracle.iam.provisioning.api.ProvisioningService;
import weblogic.security.auth.login.UsernamePasswordLoginModule;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;
import java.util.Collections;
import java.util.Hashtable;


public class OIMUtil {
    private static final ODLLogger logger = ODLLogger.getODLLogger(OIMUtil.class.getName());
    static tcUserOperationsIntf userOperationsintf = null;
    static tcFormInstanceOperationsIntf formInstanceOperationsIntf = null;
    static tcProvisioningOperationsIntf provisioningOperationsIntf = null;
    static tcFormDefinitionOperationsIntf formDefOperationIntf = null;
    static tcFormInstanceOperationsIntf formInstanceIntf = null;
    static tcITResourceInstanceOperationsIntf itResDefOperationIntf = null;
    static UserManager userService = null;
    static PlatformService platformService = null;
    static RoleManager roleService = null;
    static OrganizationManager orgService = null;
    static ProvisioningService provisioningService = null;
    static TaskDefinitionOperationsIntf taskDefOps = null;
    static PlatformUtilsService platformUtilsService = null;
    static tcLookupOperationsIntf tcLookupOperationsIntf = null;
    static ApplicationInstanceService applicationInstanceService = null;
    private static OIMClient oimClient = null;

    public static <T> T getService(Class<T> serviceClass) {
        if (oimClient != null) {
            return oimClient.getService(serviceClass);
        } else {
            return Platform.getService(serviceClass);
        }
    }

    public static void localInitialize(String hostname, String port, String username, String password) throws LoginException {
        String url = String.format("t3://%s:%s", hostname, port);
        if (null == formInstanceOperationsIntf) {
            Configuration.setConfiguration(new Configuration() {
                @Override
                public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                    return new AppConfigurationEntry[]{new AppConfigurationEntry(UsernamePasswordLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, Collections.singletonMap("debug", "true"))};
                }
            });
            System.setProperty("APPSERVER_TYPE", "wls");
            Hashtable<String, String> env = new Hashtable<>();
            env.put(OIMClient.JAVA_NAMING_FACTORY_INITIAL, "weblogic.jndi.WLInitialContextFactory");
            env.put(OIMClient.JAVA_NAMING_PROVIDER_URL, url);
            oimClient = new OIMClient(env);
            oimClient.login(username, password.toCharArray(), env);
            initialize();
        }
    }

    public static void initialize() {
        if (null == formDefOperationIntf) {
            logger.log(ODLLevel.INFO, "Initialize OIM Services, Initializer: {0}", null == oimClient ? Platform.class : OIMClient.class);
            formInstanceOperationsIntf = getService(tcFormInstanceOperationsIntf.class);
            userOperationsintf = getService(tcUserOperationsIntf.class);
            provisioningOperationsIntf = getService(tcProvisioningOperationsIntf.class);
            formDefOperationIntf = getService(tcFormDefinitionOperationsIntf.class);
            itResDefOperationIntf = getService(tcITResourceInstanceOperationsIntf.class);
            userService = getService(UserManager.class);
            platformService = getService(PlatformService.class);
            roleService = getService(RoleManager.class);
            orgService = getService(OrganizationManager.class);
            formInstanceIntf = getService(tcFormInstanceOperationsIntf.class);
            provisioningService = getService(ProvisioningService.class);
            taskDefOps = getService(TaskDefinitionOperationsIntf.class);
            platformUtilsService = getService(PlatformUtilsService.class);
            tcLookupOperationsIntf = getService(tcLookupOperationsIntf.class);
            applicationInstanceService = getService(ApplicationInstanceService.class);
        }
    }

}
