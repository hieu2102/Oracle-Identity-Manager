package vn.bnh.oim.utils;

import Thor.API.Base.tcBaseUtility;
import Thor.API.Security.XLClientSecurityAssociation;
import com.thortech.xl.dataaccess.tcDataBaseClient;
import com.thortech.xl.dataaccess.tcDataProvider;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.Platform;
import weblogic.security.auth.login.UsernamePasswordLoginModule;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;
import java.util.Collections;
import java.util.Hashtable;


public class OIMUtils {
    private static final ODLLogger logger = ODLLogger.getODLLogger(OIMUtils.class.getName());
    private static OIMClient oimClient = null;

    public static <T> T getService(Class<T> serviceClass) {
        if (oimClient != null) {
            return oimClient.getService(serviceClass);
        } else {
            return Platform.getService(serviceClass);
        }
    }

    public static tcDataProvider getTcDataProvider() {
        if (oimClient != null) {
            XLClientSecurityAssociation.setClientHandle(oimClient);
            return new tcDataBaseClient();
        } else {
            tcBaseUtility tcBaseUtil = Platform.getService(tcBaseUtility.class);
            return tcBaseUtil.getDataBase();
        }
    }

    public static void localInitialize(
            String hostname,
            String port,
            String username,
            String password
    ) throws LoginException {
        String url = String.format("t3://%s:%s", hostname, port);
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
    }

}
