package vn.bnh.oim.adapters;

import org.identityconnectors.framework.common.objects.EmbeddedObject;
import org.identityconnectors.framework.common.objects.EmbeddedObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoleAttributeBuilder {
    public static EmbeddedObject generateRoleComponent(
            Map<String, String> objectData,
            String roleFormat
    ) {
        EmbeddedObjectBuilder builder = new EmbeddedObjectBuilder();
        Pattern pattern = Pattern.compile(".*\"(.*?)\".*:.*\"(.*?)\".*");
        builder.setObjectClass(ObjectClass.GROUP);
        String[] roleObjAttributes = roleFormat.split(",");
        for (String roleObjAttr : roleObjAttributes) {
            Matcher match = pattern.matcher(roleObjAttr);
            if (match.matches()) {
                builder.addAttribute(match.group(1), objectData.get(match.group(2)));
            }
        }
        return builder.build();
    }
}
