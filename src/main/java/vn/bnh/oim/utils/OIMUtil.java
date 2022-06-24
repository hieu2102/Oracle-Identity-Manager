package vn.bnh.oim.utils;

import Thor.API.Operations.*;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.orgmgmt.api.OrganizationManager;
import oracle.iam.identity.rolemgmt.api.RoleManager;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.Platform;
import oracle.iam.platformservice.api.PlatformService;
import oracle.iam.provisioning.api.ProvisioningService;
import weblogic.security.auth.login.UsernamePasswordLoginModule;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;
import java.util.Collections;
import java.util.Hashtable;


public class OIMUtil {
    private final ODLLogger logger = ODLLogger.getODLLogger(OIMUtil.class.getName());
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

    public OIMUtil() {
        this.initialize();
    }

    /**
     * Constructor for local development
     *
     * @param hostname
     * @param port
     * @param username
     * @param password
     */
    public OIMUtil(String hostname, String port, String username, String password) throws LoginException {
        this.localInitialize(String.format("t3://%s:%s", hostname, port), username, password);
    }

    private void localInitialize(String url, String username, String password) throws LoginException {
        logger.info("Initialize OIM Services ");
        Configuration.setConfiguration(new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
//                Preconditions.checkArgument("xellerate".equals(name));
                return new AppConfigurationEntry[]{new AppConfigurationEntry(UsernamePasswordLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, Collections.singletonMap("debug", "true"))};
            }
        });
        System.setProperty("APPSERVER_TYPE", "wls");
        Hashtable<String, String> env = new Hashtable<>();
        env.put(OIMClient.JAVA_NAMING_FACTORY_INITIAL, "weblogic.jndi.WLInitialContextFactory");
        env.put(OIMClient.JAVA_NAMING_PROVIDER_URL, url);
        OIMClient oimClient = new OIMClient(env);
        oimClient.login(username, password.toCharArray(), env);
        formInstanceOperationsIntf = oimClient.getService(tcFormInstanceOperationsIntf.class);
        userOperationsintf = oimClient.getService(tcUserOperationsIntf.class);
        provisioningOperationsIntf = oimClient.getService(tcProvisioningOperationsIntf.class);
        formDefOperationIntf = oimClient.getService(tcFormDefinitionOperationsIntf.class);
        itResDefOperationIntf = oimClient.getService(tcITResourceInstanceOperationsIntf.class);
        userService = oimClient.getService(UserManager.class);
        platformService = oimClient.getService(PlatformService.class);
        roleService = oimClient.getService(RoleManager.class);
        orgService = oimClient.getService(OrganizationManager.class);
        formInstanceIntf = oimClient.getService(tcFormInstanceOperationsIntf.class);
        provisioningService = oimClient.getService(ProvisioningService.class);

    }

    private void initialize() {
        logger.info("Initialize OIM Services ");
        if (null == formDefOperationIntf) {
            formInstanceOperationsIntf = Platform.getService(tcFormInstanceOperationsIntf.class);
            userOperationsintf = Platform.getService(tcUserOperationsIntf.class);
            provisioningOperationsIntf = Platform.getService(tcProvisioningOperationsIntf.class);
            formDefOperationIntf = Platform.getService(tcFormDefinitionOperationsIntf.class);
            itResDefOperationIntf = Platform.getService(tcITResourceInstanceOperationsIntf.class);
            userService = Platform.getService(UserManager.class);
            platformService = Platform.getService(PlatformService.class);
            roleService = Platform.getService(RoleManager.class);
            orgService = Platform.getService(OrganizationManager.class);
            formInstanceIntf = Platform.getService(tcFormInstanceOperationsIntf.class);
            provisioningService = Platform.getService(ProvisioningService.class);
        }
    }
}