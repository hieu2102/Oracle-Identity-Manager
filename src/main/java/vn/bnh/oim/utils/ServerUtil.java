package vn.bnh.oim.utils;

import oracle.iam.platformservice.api.PlatformUtilsService;
import oracle.iam.platformservice.exception.PlatformServiceException;
import oracle.iam.platformservice.vo.JarElement;

import java.util.HashSet;
import java.util.Set;

public class ServerUtil {
    static PlatformUtilsService platformUtilsService = OIMUtil.platformUtilsService;

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

    public static void uploadJar(String jarDir, JarType jarType) throws PlatformServiceException {
        JarElement jarToUpload = new JarElement();
        jarToUpload.setPath(jarDir);
        jarToUpload.setType(jarType.getId());
        Set<JarElement> jarSet = new HashSet<>();
        jarSet.add(jarToUpload);
        platformUtilsService.uploadJars(jarSet);
    }
}
