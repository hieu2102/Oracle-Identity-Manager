package vn.bnh.oim.adapters;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.EmbeddedObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class RoleAttributeBuilderTest {
    @Test
    public void testGenObj() {
        String format = "{\"User Group\":\"UserGroup\", \"User Role\":\"UserRole\"}";
        Map<String, String> objData = new HashMap<>();
        objData.put("UserGroup", "group1");
        objData.put("UserRole", "r1");
        EmbeddedObject attr = RoleAttributeBuilder.generateRoleComponent(objData, format);
        System.out.println(attr);
    }
}