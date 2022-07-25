package vn.bnh.oim.utils;

import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.connectors.icfcommon.FieldMapping;
import oracle.iam.provisioning.vo.ApplicationInstance;
import oracle.iam.provisioning.vo.ChildTableRecord;
import org.identityconnectors.framework.common.objects.*;
import vn.bnh.oim.adapters.CustomProvisioningAdapter;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class AdapterUtil {
    private static final ODLLogger logger = ODLLogger.getODLLogger(CustomProvisioningAdapter.class.getName());

    private static final String[] HEADER_FIELDS = new String[]{"MessageId", "FromId", "FromName", "ToId", "ToName", "DateTime", "Signature"};
    private static final SimpleDateFormat ID_FORMAT = new SimpleDateFormat("yyyyMMddHH24mmss");

    public static Set<Attribute> generateRequestBody(
            ApplicationInstance applicationInstance,
            Set<Attribute> payload
    ) {
        Set<Attribute> requestBody = new HashSet<>();
        requestBody.add(generateHeader(applicationInstance));
        requestBody.add(wrapPayload(payload));
        return requestBody;
    }

    public static Attribute generateHeader(ApplicationInstance appInst) {
        String targetAppId = appInst.getDisplayName() + "_API";
        AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setName("Header");
        EmbeddedObjectBuilder eoBuilder = new EmbeddedObjectBuilder();
        Arrays.stream(HEADER_FIELDS).forEach(field -> {
            eoBuilder.setObjectClass(ObjectClass.GROUP);
            if (field.equals("MessageId")) {
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                eoBuilder.addAttribute(field, ID_FORMAT.format(timestamp));
            }
            if (field.startsWith("From")) {
                eoBuilder.addAttribute(field, "IDM");
            }
            if (field.startsWith("To")) {
                eoBuilder.addAttribute(field, targetAppId);
            }
            if (field.equals("DateTime") || field.equals("Signature")) {
                eoBuilder.addAttribute(field, "");
            }
        });
        attributeBuilder.addValue(eoBuilder.build());
        return attributeBuilder.build();
    }

    public static Attribute wrapPayload(Set<Attribute> attributeSet) {
        AttributeBuilder attributeBuilder = new AttributeBuilder();
        attributeBuilder.setName("Payload");
        attributeBuilder.addValue(attributeSet);
        return attributeBuilder.build();
    }

    public static Attribute parseRoleFieldFromChildRecord(
            String outputAttributeName,
            List<FieldMapping> childFieldMappings,
            List<ChildTableRecord> childDataRecords
    ) {
        logger.log(ODLLevel.INFO, "Enter parseRoleFieldFromChildRecord [{0},{1},{2}]", new Object[]{outputAttributeName, childFieldMappings, childDataRecords});
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
