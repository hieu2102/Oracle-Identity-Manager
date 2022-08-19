package vn.bnh.oim.utils;

import oracle.iam.platformservice.api.PlatformService;
import oracle.iam.platformservice.api.PlatformUtilsService;
import oracle.iam.platformservice.exception.PlatformServiceException;
import oracle.iam.platformservice.vo.JarElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ServerUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerUtil.class);
    static PlatformUtilsService platformUtilsService = OIMUtil.getService(PlatformUtilsService.class);
    static PlatformService platformService = OIMUtil.getService(PlatformService.class);

    public enum JarType {
        JavaTasks("JavaTasks"), ScheduleTask("ScheduleTask"), ThirdParty("ThirdParty"), ICFBundle("ICFBundle");
        private final String id;

        JarType(String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }
    }

    /**
     * Upload Jar to OIM server
     *
     * @param jarDir  JAR file directory <u><b>on</b></u> OIM server
     * @param jarType JAR file type (Scheduled Task, Java Task/Adapter, Third-party libs, Connector Bundle)
     */
    public static void uploadJar(
            String jarDir,
            JarType jarType
    ) throws PlatformServiceException {
        JarElement jarToUpload = new JarElement();
        jarToUpload.setPath(jarDir);
        jarToUpload.setType(jarType.getId());
        Set<JarElement> jarSet = new HashSet<>();
        jarSet.add(jarToUpload);
        platformUtilsService.uploadJars(jarSet);
    }

    public static void deleteJar(
            String jarName,
            JarType jarType
    ) throws PlatformServiceException {
        JarElement jarToDelete = new JarElement();
        jarToDelete.setType(jarType.getId());
        jarToDelete.setName(jarName);
        Set<JarElement> jarSet = new HashSet<>();
        jarSet.add(jarToDelete);
        platformUtilsService.deleteJars(jarSet);
    }

    public static void updateJar(
            String jarDir,
            String jarName,
            JarType jarType
    ) {
        JarElement jarToUpload = new JarElement();
        jarToUpload.setPath(jarDir);
        jarToUpload.setName(jarName);
        jarToUpload.setType(jarType.getId());
        Set<JarElement> jarSet = new HashSet<>();
        jarSet.add(jarToUpload);
        platformUtilsService.updateJars(jarSet);

    }

    public static void registerPlugin(String pluginFilePath) {
        File zipFile = new File(pluginFilePath);
        FileInputStream fis;
        try {
            fis = new FileInputStream(zipFile);
            int size = (int) zipFile.length();
            byte[] b = new byte[size];
            int bytesRead = fis.read(b, 0, size);
            while (bytesRead < size) {
                bytesRead += fis.read(b, bytesRead, size - bytesRead);
            }
            fis.close();
            platformService.registerPluginAndReturnStatus(b).forEach((k, v) -> {
                LOGGER.info("Register plugin {} result: {}", k, Arrays.asList(v));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("plugin registered");
    }
}
