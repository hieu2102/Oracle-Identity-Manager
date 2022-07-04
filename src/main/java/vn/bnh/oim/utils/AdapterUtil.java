package vn.bnh.oim.utils;

import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.connectors.icfcommon.FieldMapping;
import oracle.iam.provisioning.vo.ChildTableRecord;
import org.identityconnectors.framework.common.objects.*;
import vn.bnh.oim.adapters.CustomProvisioningAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdapterUtil {
    private static final ODLLogger logger = ODLLogger.getODLLogger(CustomProvisioningAdapter.class.getName());

    public static Attribute parseRoleFieldFromChildRecord(
            String outputAttributeName,
            List<FieldMapping> childFieldMappings,
            List<ChildTableRecord> childDataRecords
    ) {
        AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setName(outputAttributeName);
        ArrayList<EmbeddedObject> attributeValue = new ArrayList<>();
//        loop through child tables rows
        childDataRecords.forEach(ctr -> {
            EmbeddedObjectBuilder eoBuilder = new EmbeddedObjectBuilder();
            eoBuilder.setObjectClass(ObjectClass.GROUP);
            childFieldMappings.forEach(fm -> {
                String childDataKey = fm.getChildForm().concat("_").concat(fm.getFieldLabel().replace(" ", "_")).toUpperCase();
                eoBuilder.addAttribute(fm.getAttributeName(), ctr.getChildData().get(childDataKey));
            });
            attributeValue.add(eoBuilder.build());

        });
        attributeBuilder.addValue(attributeValue);
        Attribute output = attributeBuilder.build();
        logger.log(ODLLevel.INFO, "output: {0}", output);
        return output;
    }

    public static Attribute populateRoleField(
            String parentRoleFieldLabel,
            List<FieldMapping> childFieldMappings,
            List<Map<String, String>> childTableData
    ) {
        logger.log(ODLLevel.INFO, "populate Role Field: {0}", parentRoleFieldLabel);
        AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setName(parentRoleFieldLabel);
        ArrayList<EmbeddedObject> attributeValue = new ArrayList<>();
        childTableData.forEach(childData -> {
            EmbeddedObjectBuilder eoBuilder = new EmbeddedObjectBuilder();
            eoBuilder.setObjectClass(ObjectClass.GROUP);
            childFieldMappings.forEach(fieldMapping -> eoBuilder.addAttribute(fieldMapping.getAttributeName(), childData.get(fieldMapping.getFieldLabel())));
            attributeValue.add(eoBuilder.build());
        });
        attributeBuilder.addValue(attributeValue);
        Attribute output = attributeBuilder.build();
        logger.log(ODLLevel.INFO, "output: {0}", output);
        return output;
    }

}
