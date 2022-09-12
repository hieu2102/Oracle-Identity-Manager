package vn.bnh.oim.adapters;

import com.thortech.xl.dataaccess.tcDataProvider;
import oracle.iam.connectors.icfcommon.*;
import oracle.iam.connectors.icfcommon.extension.ResourceExclusion;
import oracle.iam.connectors.icfcommon.extension.Validation;
import oracle.iam.connectors.icfcommon.prov.ProvEvent;
import oracle.iam.connectors.icfcommon.prov.Template;
import oracle.iam.connectors.icfcommon.service.ProvisioningService;
import oracle.iam.connectors.icfcommon.service.ServiceFactory;
import oracle.iam.connectors.icfcommon.util.ExceptionUtil;
import oracle.iam.connectors.icfcommon.util.TypeUtil;
import oracle.iam.provisioning.vo.Account;
import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.SchemaApiOp;
import org.identityconnectors.framework.api.operations.ScriptOnConnectorApiOp;
import org.identityconnectors.framework.api.operations.UpdateApiOp;
import org.identityconnectors.framework.common.objects.*;
import vn.bnh.oim.utils.ApplicationInstanceUtils;
import vn.bnh.oim.utils.LookupUtils;

import java.util.*;
import java.util.stream.Collectors;

public class SmartFormProvisioningAdapter {
    private static final Log LOG = Log.getLog(oracle.iam.connectors.icfcommon.prov.ICProvisioningManager.class);
    private static final String PROVISIONING_LOOKUP_PROPERTY_NAME = "Provisioning Attribute Map";
    private static final String OPTIONS_MAP_LOOKUP_NAME = "Operation Options Map";
    private static final String COMPOUND_VALUE_DELIMITER = "~";
    private final String itResourceFieldName;
    private final long processInstanceKey;
    private final tcDataProvider dataProvider;
    private ProvisioningService provisioningService;
    private Form form;
    private Form currentForm;
    private IResourceConfig resourceConfig;
    private Lookup provisioningLookup;
    private Lookup optionsLookup;
    private ConnectorFacade connectorFacade;
    private ConnectorOpHelper connectorOpHelper;
    private String effectiveITResourceName;
    private Schema schema;
    private OperationOptions operationOptions = (new OperationOptionsBuilder()).build();
    private ChildFormQuery formQuery;
    private final EmbeddedObject PLACEHOLDER_VALUE = new EmbeddedObjectBuilder().setObjectClass(ObjectClass.GROUP).addAttribute("PLACEHOLDER", "").build();


    public SmartFormProvisioningAdapter(String itResourceFieldName, long processInstanceKey, tcDataProvider dataProvider) {
        this.itResourceFieldName = itResourceFieldName;
        this.processInstanceKey = processInstanceKey;
        this.dataProvider = dataProvider;
    }

    /**
     * TODO: implements
     *
     * @param objectType
     * @param provisioningFieldMappingLookupTable
     * @return
     */
    public String updateAccount(String objectType, String provisioningFieldMappingLookupTable) {
        LOG.ok("Enter [{0}, {1}]", objectType, provisioningFieldMappingLookupTable);
        LOG.info("Enter updateAccount: [{0}, {1}]", objectType, provisioningFieldMappingLookupTable);
        System.out.printf("[%s] Enter updateAccount: [%s,%s]%n", this.getClass().getCanonicalName(), objectType, provisioningFieldMappingLookupTable);
        try {
            Account acc = ApplicationInstanceUtils.getAccountByProcessInstKey(this.processInstanceKey);
            assert acc != null;
            this.init(objectType);
            Validation validator = Validation.newInstance(objectType, this.resourceConfig);
            validator.validate(this.form);
            ResourceExclusion.newInstance(objectType, this.resourceConfig).processExclusions(this.form);
            ObjectClass objectClass = TypeUtil.convertObjectType(objectType);
            ProvEvent provEvent = new ProvEvent(this.form, this.provisioningLookup, objectClass, null);
            Set<Attribute> attributes = provEvent.buildAttributes();
            List<Attribute> childAttributes = new ArrayList<>();
            acc.getAccountData().getChildData().forEach((key1, value) -> {
                AttributeBuilder builder = new AttributeBuilder();
                Set<FieldMapping> fieldMappings = provEvent.getFieldMappings().stream().filter(fm -> key1.equals(fm.getChildForm())).collect(Collectors.toSet());
                String attributeName = LookupUtils.getLookupValue(provisioningFieldMappingLookupTable, key1);
                builder.setName(attributeName);
                ArrayList<EmbeddedObject> attributeValue = new ArrayList<>();
                value.forEach(ctr -> {
                    EmbeddedObjectBuilder eoBuilder = new EmbeddedObjectBuilder();
                    eoBuilder.setObjectClass(ObjectClass.GROUP);
                    Map<String, Object> rowData = ctr.getChildData();
                    fieldMappings.forEach(fm -> {
                        String key = fm.getChildForm() + "_" + fm.getFieldLabel().replaceAll("\\s", "_").toUpperCase();
                        eoBuilder.addAttribute(fm.getAttributeName(), rowData.get(key));
                    });
                    attributeValue.add(eoBuilder.build());
                });
                if (attributeValue.size() == 0) {
                    builder.addValue(Arrays.asList(PLACEHOLDER_VALUE));
                } else {
                    builder.addValue(attributeValue);
                }
                childAttributes.add(builder.build());
            });
            LOG.info("Child Attributes: {0}", childAttributes);
            System.out.printf("[%s] Child Attributes: %s%n", this.getClass().getCanonicalName(), childAttributes);
            attributes.addAll(childAttributes);
//            add enabled/disabled attribute
            Attribute isEnabled = new AttributeBuilder().setName("__ENABLE__").addValue(!acc.getAccountStatus().equalsIgnoreCase("Disabled")).build();
            attributes.add(isEnabled);
            LOG.info("Attributes: {0}", attributes);
            System.out.printf("[%s] Attributes: %s%n", this.getClass().getCanonicalName(), attributes);

            return this.doUpdate(objectType, provEvent, attributes);
        } catch (Exception e) {
            LOG.error("Error: {0}", e.getCause());
            LOG.error("{0}", e.getStackTrace());
            return null;

        }
    }

    private String doUpdate(String objectType, ProvEvent provEvent, Set<Attribute> attributes) {
        LOG.ok("Enter [{0}, {1}]", objectType, attributes);
        Uid uid = provEvent.getUid();
        String uidFieldLabel = provEvent.getUidFieldLabel();
        LOG.ok("Uid: [{0}]", uid);
        String responseCode = "SUCCESS";
        try {
            ObjectClass objectClass = TypeUtil.convertObjectType(objectType);
            OperationOptions scriptOptions = this.createOperationOptionsBuilder(ScriptOnConnectorApiOp.class).build();
            attributes.add(uid);
            this.connectorOpHelper.execute(this.resourceConfig.getAction(objectType, Action.Timing.BEFORE_UPDATE), attributes, scriptOptions);
            OperationOptions operationOptions = this.createOperationOptionsBuilder(UpdateApiOp.class).build();
            attributes.remove(uid);
            Uid retUid = this.connectorFacade.update(objectClass, uid, attributes, operationOptions);
            this.provisioningService.setFormField(this.processInstanceKey, uidFieldLabel, retUid.getUidValue());
            this.writeBack(objectClass, retUid, provEvent);
            attributes.add(uid);
            this.connectorOpHelper.execute(this.resourceConfig.getAction(objectType, Action.Timing.AFTER_UPDATE), attributes, scriptOptions);
        } catch (RuntimeException var11) {
            LOG.error(var11, "Error while updating user");
            responseCode = ExceptionUtil.getResponse(var11);
        }
        LOG.ok("Return [{0}]", responseCode);
        return responseCode;
    }

    private OperationOptionsBuilder createOperationOptionsBuilder(Class<? extends APIOperation> operation) {
        LOG.ok("Enter [{0}]", operation);
        OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder(this.operationOptions);
        Map<String, String> templates = new HashMap<>();
        if (this.optionsLookup != null) {
            Schema schema = this.getConnectorSchema();
            Iterator<Map.Entry<String, String>> var5 = this.optionsLookup.toMap().entrySet().iterator();

            label41:
            while (true) {
                OptionMapping mapping;
                Set<Class<? extends APIOperation>> ops;
                do {
                    Map.Entry<String, String> entry;
                    if (!var5.hasNext()) {
                        var5 = templates.entrySet().iterator();

                        while (var5.hasNext()) {
                            entry = var5.next();
                            Object val = Template.getInstance().evaluate(optionsBuilder.getOptions(), entry.getValue());
                            optionsBuilder.setOption(entry.getKey(), val);
                        }
                        break label41;
                    }

                    entry = var5.next();
                    mapping = new OptionMapping(entry.getKey(), entry.getValue());
                    ops = mapping.getOperations();
                } while (!ops.isEmpty() && operation != null && !ops.contains(operation));

                if (mapping.isTemplate()) {
                    templates.put(mapping.getOptionName(), mapping.getOptionValueTemplate());
                } else {
                    Class<?> optionType = String.class;
                    if (schema != null) {
                        OperationOptionInfo operationOptionInfo = schema.findOperationOptionInfo(mapping.getOptionName());
                        if (operationOptionInfo != null) {
                            optionType = operationOptionInfo.getType();
                        }
                    }

                    Object optionValue = this.form.getFieldValueByLabel(mapping.getFieldLabel(), optionType);
                    optionsBuilder.setOption(mapping.getOptionName(), optionValue);
                }
            }
        }

        LOG.ok("Return");
        return optionsBuilder;
    }

    private void writeBack(ObjectClass objectClass, Uid uid, ProvEvent provEvent) {
        LOG.ok("Enter [{0}, {1}]", objectClass, uid);
        LOG.ok(" FieldMappings [{0}]", provEvent.getFieldMappingByFlag(FieldMapping.FieldFlag.WRITEBACK));
        Set<FieldMapping> writeBackFieldMappings = provEvent.getFieldMappingByFlag(FieldMapping.FieldFlag.WRITEBACK);
        if (!writeBackFieldMappings.isEmpty()) {
            Set<FieldMapping> parentWriteBackFieldMappings = new HashSet<>();
            Set<FieldMapping> childWriteBackFieldMappings = new HashSet<>();
            new HashMap();
            for (FieldMapping fieldMapping : writeBackFieldMappings) {
                if (!fieldMapping.isChildForm()) {
                    LOG.ok("adding parent field map [{0}]", fieldMapping);
                    parentWriteBackFieldMappings.add(fieldMapping);
                } else if (fieldMapping.isEmbeddedObject() && this.formQuery.getType().equals(ChildFormQuery.Type.ADD)) {
                    List<Map<String, String>> addedChildRows = this.form.getChildFormFieldValuesByColumn(fieldMapping.getChildForm());
                    if (!addedChildRows.isEmpty()) {
                        Map<String, String> addedChildRow = addedChildRows.get(0);
                        String writeBackColName = this.getWriteBackColumnNameByLabel(fieldMapping.getFieldLabel());
                        String writeBackColValue = addedChildRow.get(writeBackColName);
                        if (StringUtil.isNotBlank(writeBackColValue) && !writeBackColValue.equals("0")) {
                            LOG.warn("Write back child column: {0} has a non-zero value: {1}. Assuming that write back has already happened for this row, ignoring write back now", writeBackColName, writeBackColValue);
                        } else {
                            childWriteBackFieldMappings.add(fieldMapping);
                        }
                    }
                }
            }

            Set<String> attributesToGet = getAttributesToGet(parentWriteBackFieldMappings);
            LOG.ok(" Parent write back filed mappings are [{0}]", parentWriteBackFieldMappings);
            attributesToGet.addAll(getAttributesToGet(childWriteBackFieldMappings));
            if (attributesToGet.isEmpty()) {
                LOG.warn("No write back attributes to be fetched from the connector. Exiting write back");
                LOG.ok("Return");
                return;
            }

            OperationOptionsBuilder oOptionsBuilder = this.createOperationOptionsBuilder(UpdateApiOp.class);
            oOptionsBuilder.setAttributesToGet(attributesToGet);
            ConnectorObject connectorObject = this.connectorFacade.getObject(objectClass, uid, oOptionsBuilder.build());
            Assertions.nullCheck(connectorObject, "connectorObject");
            if (!parentWriteBackFieldMappings.isEmpty()) {
                this.writeBackParentForm(parentWriteBackFieldMappings, connectorObject);
            }

            if (!childWriteBackFieldMappings.isEmpty()) {
                this.writeBackChildForm(childWriteBackFieldMappings, connectorObject, provEvent);
            }
        }

        LOG.ok("Return");
    }

    private static Set<String> getAttributesToGet(Set<FieldMapping> fieldMappings) {
        Set<String> attributeNames = new HashSet<>();

        for (FieldMapping fieldMapping : fieldMappings) {
            attributeNames.add(fieldMapping.getAttributeName());
        }

        return attributeNames;
    }

    private void writeBackParentForm(Set<FieldMapping> writeBackFieldMappings, ConnectorObject connectorObject) {

        for (FieldMapping fieldMapping : writeBackFieldMappings) {
            Attribute attribute = connectorObject.getAttributeByName(fieldMapping.getAttributeName());
            if (attribute == null) {
                LOG.warn("Attribute [{0}] not found in Connector Object", fieldMapping.getAttributeName());
            } else {
                String attributeValue = AttributeUtil.getStringValue(attribute);
                this.provisioningService.setFormField(this.processInstanceKey, fieldMapping.getFieldLabel(), attributeValue);
            }
        }

    }

    private void writeBackChildForm(Set<FieldMapping> writeBackFieldMappings, ConnectorObject connectorObject, ProvEvent provEvent) {
        Map<String, Set<FieldMapping>> wbFieldMappingsByChild = this.splitWriteBackFieldMappingsByChildForm(writeBackFieldMappings);
        Map<String, Map<String, List<Object>>> conToOimChildFieldNameMap = this.getConToOimChildFieldNameMap(provEvent.getFieldMappings(), wbFieldMappingsByChild.keySet());

        for (String childFormName : wbFieldMappingsByChild.keySet()) {
            Map<String, List<Object>> conToOimChildFieldNameMapPerChild = conToOimChildFieldNameMap.get(childFormName);
            String childAttrName = ((FieldMapping) ((Set) wbFieldMappingsByChild.get(childFormName)).iterator().next()).getAttributeName();
            Map<String, String> addedChildRow = this.form.getChildFormFieldValuesByColumn(childFormName).get(0);
            Attribute attribute = connectorObject.getAttributeByName(childAttrName);
            if (attribute == null) {
                LOG.warn("Attribute [{0}] not found in Connector Object", childAttrName);
            } else {
                List<Object> embeddedObjects = attribute.getValue();

                for (Object embeddedObject : embeddedObjects) {
                    boolean isMatchingEmbObj = true;
                    Iterator<Attribute> var16 = ((EmbeddedObject) embeddedObject).getAttributes().iterator();

                    String childColName;
                    label126:
                    do {
                        Attribute attr;
                        String childColumnValue;
                        EnumSet childFieldFlags;
                        do {
                            do {
                                if (!var16.hasNext()) {
                                    break label126;
                                }

                                attr = var16.next();
                                List<Object> childFieldInfo = conToOimChildFieldNameMapPerChild.get(attr.getName());
                                childColName = String.valueOf(childFieldInfo.get(0));
                                childColumnValue = addedChildRow.get(childColName);
                                childFieldFlags = (EnumSet) childFieldInfo.get(1);
                            } while (this.isWriteBackAttribute(attr.getName(), wbFieldMappingsByChild.get(childFormName)));
                        } while (StringUtil.isEmpty(childColumnValue) && (attr.getValue() == null || attr.getValue().isEmpty() || attr.getValue().get(0) == null || StringUtil.isEmpty(String.valueOf(attr.getValue().get(0)))));

                        if (childFieldFlags.contains(FieldMapping.FieldFlag.DATE) || StringUtil.isNotEmpty(childColumnValue) && attr.getValue() != null && !attr.getValue().isEmpty() && attr.getValue().get(0) != null && StringUtil.isNotEmpty(String.valueOf(attr.getValue().get(0)))) {
                            if (childFieldFlags.contains(FieldMapping.FieldFlag.LOOKUP)) {
                                String childColumnLookupValue = childColumnValue.split("~", 2)[1];
                                if (!childColumnLookupValue.equals(AttributeUtil.getAsStringValue(attr))) {
                                    isMatchingEmbObj = false;
                                }
                            } else if (childFieldFlags.contains(FieldMapping.FieldFlag.DATE)) {
                                Object targetValue = AttributeUtil.getSingleValue(attr);
                                if (StringUtil.isEmpty(childColumnValue)) {
                                    if (targetValue != null && (Long) targetValue != 0L) {
                                        isMatchingEmbObj = false;
                                    }
                                } else {
                                    long childColumnLongValue = TypeUtil.convertValueType(childColumnValue, Date.class).getTime();
                                    if (childColumnLongValue == 0L) {
                                        if (targetValue != null && (Long) targetValue != 0L) {
                                            isMatchingEmbObj = false;
                                        }
                                    } else if (targetValue == null || (Long) targetValue == 0L || childColumnLongValue != AttributeUtil.getLongValue(attr)) {
                                        isMatchingEmbObj = false;
                                    }
                                }
                            } else if (!childColumnValue.equals(AttributeUtil.getAsStringValue(attr))) {
                                isMatchingEmbObj = false;
                            }
                        } else {
                            isMatchingEmbObj = false;
                        }
                    } while (isMatchingEmbObj);

                    if (isMatchingEmbObj) {
                        Map<String, Object> updatedChildTableValues = new HashMap<>();

                        for (FieldMapping o : wbFieldMappingsByChild.get(childFormName)) {
                            childColName = String.valueOf(conToOimChildFieldNameMapPerChild.get(o.getEmbeddedAttributeName()).get(0));
                            Object valueToWriteBack = AttributeUtil.getSingleValue(((EmbeddedObject) embeddedObject).getAttributeByName(o.getEmbeddedAttributeName()));
                            updatedChildTableValues.put(childColName, valueToWriteBack);
                        }

                        this.provisioningService.setChildFormField(childFormName, (Long) this.formQuery.getValue(), updatedChildTableValues);
                        break;
                    }
                }
            }
            LOG.ok("Return");
            return;
        }
    }

    private boolean isWriteBackAttribute(String attrName, Set<FieldMapping> wbFieldMappingsPerChild) {
        Iterator<FieldMapping> var3 = wbFieldMappingsPerChild.iterator();

        FieldMapping fieldMapping;
        do {
            if (!var3.hasNext()) {
                return false;
            }

            fieldMapping = var3.next();
        } while (!attrName.equalsIgnoreCase(fieldMapping.getEmbeddedAttributeName()));

        return true;
    }

    private Map<String, Set<FieldMapping>> splitWriteBackFieldMappingsByChildForm(Set<FieldMapping> writeBackFieldMappings) {
        Map<String, Set<FieldMapping>> wbFieldMappingsByChild = new HashMap<>();

        for (FieldMapping fieldMapping : writeBackFieldMappings) {
            String attributeName = fieldMapping.getChildForm();
            if (wbFieldMappingsByChild.containsKey(attributeName)) {
                wbFieldMappingsByChild.get(attributeName).add(fieldMapping);
            } else {
                Set<FieldMapping> fieldMappings = new HashSet<>();
                fieldMappings.add(fieldMapping);
                wbFieldMappingsByChild.put(attributeName, fieldMappings);
            }
        }

        return wbFieldMappingsByChild;
    }

    private Map<String, Map<String, List<Object>>> getConToOimChildFieldNameMap(Set<FieldMapping> fieldMappings, Set<String> childNames) {
        Map<String, Map<String, List<Object>>> conToOimChildFieldNameMap = new HashMap<>();
        Iterator<FieldMapping> var4 = fieldMappings.iterator();

        while (true) {
            FieldMapping fieldMapping;
            do {
                if (!var4.hasNext()) {
                    return conToOimChildFieldNameMap;
                }

                fieldMapping = var4.next();
            } while (!childNames.contains(fieldMapping.getChildForm()));

            for (Form.FieldInfo childFieldInfo : this.form.getChildFieldInfo()) {
                if (childFieldInfo.getLabel().equalsIgnoreCase(fieldMapping.getFieldLabel()) && childFieldInfo.getName().startsWith(fieldMapping.getChildForm())) {
                    List<Object> oimChildFieldInfo = new ArrayList<>();
                    oimChildFieldInfo.add(childFieldInfo.getName());
                    oimChildFieldInfo.add(fieldMapping.getFieldFlags());
                    if (conToOimChildFieldNameMap.containsKey(fieldMapping.getChildForm())) {
                        (conToOimChildFieldNameMap.get(fieldMapping.getChildForm())).put(fieldMapping.getEmbeddedAttributeName(), oimChildFieldInfo);
                    } else {
                        Map<String, List<Object>> conToOimChildFieldNameMapPerChild = new HashMap<>();
                        conToOimChildFieldNameMapPerChild.put(fieldMapping.getEmbeddedAttributeName(), oimChildFieldInfo);
                        conToOimChildFieldNameMap.put(fieldMapping.getChildForm(), conToOimChildFieldNameMapPerChild);
                    }
                }
            }
        }
    }

    private String getWriteBackColumnNameByLabel(String fieldLabel) {
        Set<Form.FieldInfo> childFieldInfoSet = this.form.getChildFieldInfo();
        Iterator<Form.FieldInfo> var3 = childFieldInfoSet.iterator();

        Form.FieldInfo childFieldInfo;
        do {
            if (!var3.hasNext()) {
                return null;
            }

            childFieldInfo = var3.next();
        } while (!childFieldInfo.getLabel().equalsIgnoreCase(fieldLabel));

        return childFieldInfo.getName();
    }

    private Schema getConnectorSchema() {
        if (this.schema == null) {
            Set<Class<? extends APIOperation>> supportedOperations = this.connectorFacade.getSupportedOperations();
            if (supportedOperations.contains(SchemaApiOp.class)) {
                this.schema = this.connectorFacade.schema();
            }
        }

        return this.schema;
    }

    private void init(String objectType, ChildFormQuery formQuery) {
        this.provisioningService = ServiceFactory.getService(ProvisioningService.class, this.dataProvider);
        this.formQuery = formQuery;
        this.form = this.provisioningService.getForm(this.processInstanceKey, this.formQuery);
        if (this.effectiveITResourceName == null) {
            String itResourceKey = this.form.getFieldValue(this.itResourceFieldName);
            this.resourceConfig = ResourceConfigFactory.getResourceConfig(Long.parseLong(itResourceKey), this.processInstanceKey, this.dataProvider);
        } else {
            LOG.ok("Using effective IT Resource [{0}]", this.effectiveITResourceName);
            this.resourceConfig = ResourceConfigFactory.getResourceConfig(this.effectiveITResourceName, this.dataProvider);
        }

        this.provisioningLookup = this.resourceConfig.getObjectTypeLookup(objectType, "Provisioning Attribute Map");
        Lookup objTypeLookup = this.resourceConfig.getObjectTypeConfigLookup(objectType);
        if (StringUtil.isNotBlank(objTypeLookup.getValue("Operation Options Map"))) {
            this.optionsLookup = this.resourceConfig.getObjectTypeLookup(objectType, "Operation Options Map");
        }

        this.connectorFacade = ConnectorFactory.createConnectorFacade(this.resourceConfig);
        this.connectorOpHelper = new ConnectorOpHelper(this.connectorFacade);
    }

    private void init(String ObjectType) {
        ChildFormQuery formQuery = new ChildFormQuery(ChildFormQuery.Type.ALL, null);
        this.init(ObjectType, formQuery);
    }
}
