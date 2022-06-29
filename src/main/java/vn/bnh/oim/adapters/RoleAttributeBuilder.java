package vn.bnh.oim.adapters;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.EmbeddedObject;
import org.identityconnectors.framework.common.objects.EmbeddedObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

public class RoleAttributeBuilder {
    public static EmbeddedObject generateRoleComponent(Map<String, String> objectData, String roleFormat) {
        EmbeddedObjectBuilder builder = new EmbeddedObjectBuilder();
        Pattern pattern = Pattern.compile("\"(.*?)\"");
        builder.setObjectClass(ObjectClass.GROUP);
        Arrays.stream(roleFormat.split(",")).forEach(kvPair -> {
            String[] keyAndValue = kvPair.split(":");
            builder.addAttribute(pattern.matcher(keyAndValue[0]).group(1), objectData.get(pattern.matcher(keyAndValue[1]).group(1)));
        });
        return builder.build();
    }

    public static Attribute generateAttribute(Map<String, String> attributeData, String attributeFormat) {
//        AttributeBuilder builder = new AttributeBuilder();
        return null;
    }
}
