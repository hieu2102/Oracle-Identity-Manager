package vn.bnh.oim.adapters;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import com.thortech.xl.dataaccess.tcDataProvider;
import com.thortech.xl.ejb.beansimpl.tcFormInstanceOperationsBean;
import com.thortech.xl.orb.dataaccess.tcDataAccessException;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.connectors.icfcommon.Action.Timing;
import oracle.iam.connectors.icfcommon.*;
import oracle.iam.connectors.icfcommon.ChildFormQuery.Type;
import oracle.iam.connectors.icfcommon.FieldMapping.FieldFlag;
import oracle.iam.connectors.icfcommon.extension.ResourceExclusion;
import oracle.iam.connectors.icfcommon.extension.Validation;
import oracle.iam.connectors.icfcommon.prov.ProvEvent;
import oracle.iam.connectors.icfcommon.prov.ProvisioningManager;
import oracle.iam.connectors.icfcommon.prov.Template;
import oracle.iam.connectors.icfcommon.service.ProvisioningService;
import oracle.iam.connectors.icfcommon.service.ServiceFactory;
import oracle.iam.connectors.icfcommon.util.ExceptionUtil;
import oracle.iam.connectors.icfcommon.util.TypeUtil;
import oracle.iam.provisioning.exception.AccountNotFoundException;
import oracle.iam.provisioning.exception.GenericProvisioningException;
import oracle.iam.provisioning.exception.UserNotFoundException;
import oracle.iam.provisioning.vo.Account;
import oracle.iam.provisioning.vo.ApplicationInstance;
import oracle.iam.provisioning.vo.ChildTableRecord;
import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.operations.*;
import org.identityconnectors.framework.common.objects.*;
import vn.bnh.oim.utils.AdapterUtil;
import vn.bnh.oim.utils.ApplicationInstanceUtil;
import vn.bnh.oim.utils.OIMUtil;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"rawtypes", "unused"})
public final class CustomProvisioningAdapter implements ProvisioningManager {
    private static final Log LOG = Log.getLog(oracle.iam.connectors.icfcommon.prov.ICProvisioningManager.class);
    private static final ODLLogger logger = ODLLogger.getODLLogger(CustomProvisioningAdapter.class.getName());
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
    private String roleFormatLookupTable;
    private String parentRoleFieldLabel;
    private String childTableName;

    public CustomProvisioningAdapter(
            String itResourceFieldName,
            String roleFormatLookupTable,
            String childTableName,
            String parentRoleFieldLabel,
            long processInstanceKey,
            tcDataProvider dataProvider
    ) {
        this.itResourceFieldName = itResourceFieldName;
        this.roleFormatLookupTable = roleFormatLookupTable;
        this.parentRoleFieldLabel = parentRoleFieldLabel;
        this.childTableName = childTableName;
        this.processInstanceKey = processInstanceKey;
        this.dataProvider = dataProvider;
        OIMUtil.initialize();
    }

    public CustomProvisioningAdapter(
            String itResourceFieldName,
            long processInstanceKey,
            tcDataProvider dataProvider
    ) {
        this.itResourceFieldName = itResourceFieldName;
        this.processInstanceKey = processInstanceKey;
        this.dataProvider = dataProvider;
        OIMUtil.initialize();
    }

    private void init(
            String objectType,
            ChildFormQuery formQuery
    ) {
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
        ChildFormQuery formQuery = new ChildFormQuery(Type.ALL, null);
        this.init(ObjectType, formQuery);
    }

    /**
     * addChildTableValue adapter
     *
     * @param objectType      "User"
     * @param childTableName
     * @param childPrimaryKey
     * @return success code
     */
    public String addChildTableValue(
            String objectType,
            String childTableName,
            long childPrimaryKey
    ) {
        LOG.ok("Enter [{0}, {1}, {2}]", objectType, childTableName, childPrimaryKey);
        String responseCode = "SUCCESS";
        try {
            ChildFormQuery childFormQuery = new ChildFormQuery(Type.ADD, childPrimaryKey);
            this.doUpdateChildTableValue(objectType, childTableName, childFormQuery);
        } catch (RuntimeException var7) {
            LOG.error(var7, "Error while updating user");
            responseCode = ExceptionUtil.getResponse(var7);
        }

        LOG.ok("Return [{0}]", responseCode);
        return responseCode;
    }

    public String customizedUpdateChildTableValue(
            String objectType,
            String childTableName,
            long childTablePK
    ) {
        try {
            logger.log(ODLLevel.INFO, "Enter customizedUpdateChildTableValue [{0}, {1}, {2}, {3}]", new Object[]{objectType, childTableName, childTablePK, this.processInstanceKey});
            Account acc = ApplicationInstanceUtil.getAccountByProcessInstKey(this.processInstanceKey);
//            child record can only be added or deleted -> if child data contains childTablePK -> DELETE OP else CREATE OP
            List<ChildTableRecord> childDataRecords = acc.getAccountData().getChildData().get(childTableName).stream().filter(childTableRecord -> !childTableRecord.getRowKey().equals(String.valueOf(childTablePK))).collect(Collectors.toList());
            this.init(objectType);
            Validation validator = Validation.newInstance(objectType, this.resourceConfig);
            validator.validate(this.form);
            ResourceExclusion.newInstance(objectType, this.resourceConfig).processExclusions(this.form);
            ObjectClass objectClass = TypeUtil.convertObjectType(objectType);
            ProvEvent provEvent = new ProvEvent(this.form, this.provisioningLookup, objectClass, null);
            Set<Attribute> attributes = provEvent.buildAttributes();
            List<FieldMapping> childFieldMappings = provEvent.getFieldMappings().stream().filter(fm -> childTableName.equals(fm.getChildForm())).collect(Collectors.toList());
            attributes.add(AdapterUtil.parseRoleFieldFromChildRecord(this.parentRoleFieldLabel, childFieldMappings, childDataRecords));
            return this.doUpdate(objectType, objectClass, provEvent, attributes);
        } catch (UserNotFoundException | GenericProvisioningException | tcAPIException | tcDataAccessException |
                 AccountNotFoundException | tcColumnNotFoundException | RuntimeException rte) {
            logger.severe(rte.getMessage());
            logger.log(ODLLevel.TRACE, "{0}", Arrays.toString(rte.getStackTrace()));
            return ExceptionUtil.getResponse(rte);
        }
    }

    public String removeChildTableValue(
            String objectType,
            String childTableName,
            Integer taskInstanceKey
    ) {
        LOG.ok("Enter [{0}, {1}, {2}]", objectType, childTableName, taskInstanceKey);
        String responseCode = "SUCCESS";

        try {
            ChildFormQuery childFormQuery = new ChildFormQuery(Type.DELETE, taskInstanceKey);
            this.doUpdateChildTableValue(objectType, childTableName, childFormQuery);
        } catch (RuntimeException var6) {
            LOG.error(var6, "Error while updating user");
            responseCode = ExceptionUtil.getResponse(var6);
        }
        LOG.ok("Return [{0}]", responseCode);
        return responseCode;
    }

    public String updateChildTableValue(
            String objectType,
            String childTableName,
            Integer taskInstanceKey,
            long childPrimaryKey
    ) {
        LOG.ok("Enter [{0},{1},{2},{3}]", objectType, childTableName, taskInstanceKey, childPrimaryKey);
        String responseCode;

        try {
            this.removeChildTableValue(objectType, childTableName, taskInstanceKey);
            responseCode = this.addChildTableValue(objectType, childTableName, childPrimaryKey);
        } catch (RuntimeException var8) {
            LOG.error(var8, "Error while updating user");
            responseCode = ExceptionUtil.getResponse(var8);
        }

        LOG.ok("Return [{0}]", responseCode);
        return responseCode;
    }

    private void doUpdateChildTableValue(
            String objectType,
            String childTableName,
            ChildFormQuery childFormQuery
    ) {
        this.init(objectType, childFormQuery);
        Validation validator = Validation.newInstance(objectType, this.resourceConfig);
        validator.validate(this.form);
        ResourceExclusion.newInstance(objectType, this.resourceConfig).processExclusions(this.form);
        ObjectClass objectClass = TypeUtil.convertObjectType(objectType);
        ProvEvent provEvent = new ProvEvent(this.form, this.provisioningLookup, objectClass, null);
        Set<Attribute> attributes = provEvent.buildChildFormAttributes(childTableName);
        attributes.addAll(this.getCurrentAttributes(objectClass, new HashMap<>()));
        this.doUpdateChildTable(objectType, objectClass, provEvent, attributes, childFormQuery);
    }


    private void doUpdateChildTable(
            String objectType,
            ObjectClass objectClass,
            ProvEvent provEvent,
            Set<Attribute> attributes,
            ChildFormQuery childFormQuery
    ) {
        Uid uid = provEvent.getUid();
        logger.log(ODLLevel.INFO, "Enter doUpdateChildTable: {0},{1}", new Object[]{uid, attributes});
//        ensures method will not fail if account is in PROVISIONING state
        if (null != uid) {
            String uidFieldLabel = provEvent.getUidFieldLabel();
            Uid retUid;
            OperationOptions scriptOptions = this.createOperationOptionsBuilder(ScriptOnConnectorApiOp.class).build();
            this.connectorOpHelper.execute(this.resourceConfig.getAction(objectType, Timing.BEFORE_UPDATE), attributes, scriptOptions);
            OperationOptions operationOptions = this.createOperationOptionsBuilder(UpdateApiOp.class).build();
//            ChildFormQuery.Type type = childFormQuery.getType();
            retUid = this.connectorFacade.update(objectClass, uid, attributes, operationOptions);
            this.provisioningService.setFormField(this.processInstanceKey, uidFieldLabel, retUid.getUidValue());
            this.writeBack(objectClass, retUid, provEvent);
            this.connectorOpHelper.execute(this.resourceConfig.getAction(objectType, Timing.AFTER_UPDATE), attributes, scriptOptions);
        }
    }

    public void setEffectiveITResourceName(String itResourceName) {
        this.effectiveITResourceName = itResourceName;
    }

    public void setOperationOptions(Map<String, Object> options) {
        OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();
        optionsBuilder.getOptions().putAll(options);
        this.operationOptions = optionsBuilder.build();
    }

    private void updateFormData(LinkedHashMap transformedAttrMap) {
        Iterator<Map.Entry<String, String>> var2 = this.form.getFieldValuesByLabel().entrySet().iterator();
        Map.Entry<String, String> entry;
        String key;
        do {
            if (!var2.hasNext()) {
                return;
            }

            entry = var2.next();
            key = entry.getKey();
        } while (!transformedAttrMap.containsKey(key));

        if (null != transformedAttrMap.get(key) && transformedAttrMap.get(key) instanceof Date) {
            Timestamp timestamp = new Timestamp(((Date) transformedAttrMap.get(key)).getTime());
            this.form.setFieldValueByLabel(key, timestamp.toString());
        } else if (null != entry.getValue() && transformedAttrMap.get(key) instanceof Boolean) {
            this.form.setFieldValueByLabel(key, Boolean.parseBoolean((String) transformedAttrMap.get(key)) ? String.valueOf(1) : String.valueOf(0));
        } else {
            this.form.setFieldValueByLabel(key, String.valueOf(transformedAttrMap.get(key)));
        }
    }


    /**
     * createUser adapter
     *
     * @param objectType "User"
     * @return success code
     */
    public String createObject(String objectType) {
        OIMUtil.initialize();
        logger.log(ODLLevel.INFO, "Enter Create Object: {0}", objectType);
        LOG.ok("Enter");
        String responseCode = "SUCCESS";
        try {
            this.init(objectType);
            List<Map<String, String>> childTableData = this.form.getChildFormFieldValues().get(childTableName);
            if (childTableData.size() == 0) {
                logger.log(ODLLevel.ERROR, "Child Table Data is not set");
                return "ERROR";
            }
            Validation validator = Validation.newInstance(objectType, this.resourceConfig);
            validator.validate(this.form);
            ResourceExclusion.newInstance(objectType, this.resourceConfig).processExclusions(this.form);
            ObjectClass objectClass = TypeUtil.convertObjectType(objectType);
            ProvEvent provEvent = new ProvEvent(this.form, this.provisioningLookup, objectClass, this.getConnectorSchema());
//            add role attribute
            Set<Attribute> attributes = provEvent.buildAttributes();
            List<FieldMapping> childFieldMappings = provEvent.getFieldMappings().stream().filter(fm -> childTableName.equals(fm.getChildForm())).collect(Collectors.toList());
            attributes.add(AdapterUtil.populateRoleField(this.parentRoleFieldLabel, childFieldMappings, childTableData));
//            end add role attribute
            OperationOptions operationOptions = this.createOperationOptionsBuilder(CreateApiOp.class).build();
            OperationOptions scriptOptions = this.createOperationOptionsBuilder(ScriptOnConnectorApiOp.class).build();
            this.connectorOpHelper.execute(this.resourceConfig.getAction(objectType, Timing.BEFORE_CREATE), attributes, scriptOptions);
            Uid uid = this.connectorFacade.create(objectClass, attributes, operationOptions);
            this.provisioningService.setFormField(this.processInstanceKey, provEvent.getUidFieldLabel(), uid.getUidValue());
            this.writeBack(objectClass, uid, provEvent);
            this.connectorOpHelper.execute(this.resourceConfig.getAction(objectType, Timing.AFTER_CREATE), attributes, scriptOptions);
        } catch (RuntimeException rte) {
            logger.log(ODLLevel.ERROR, "{0}", rte.getCause());
            logger.log(ODLLevel.TRACE, "{0}", rte.getStackTrace());
            responseCode = ExceptionUtil.getResponse(rte);
        }
        LOG.ok("Return [{0}]", responseCode);
        return responseCode;
    }

    public String deleteObject(String objectType) {
        LOG.ok("Enter");
        String responseCode = "SUCCESS";

        try {
            this.init(objectType);
            Validation validator = Validation.newInstance(objectType, this.resourceConfig);
            validator.validate(this.form);
            ResourceExclusion.newInstance(objectType, this.resourceConfig).processExclusions(this.form);
            ObjectClass objectClass = TypeUtil.convertObjectType(objectType);
            ProvEvent provEvent = new ProvEvent(this.form, this.provisioningLookup, objectClass, this.getConnectorSchema());
            Uid uid = provEvent.getUid();
            OperationOptions operationOptions = this.createOperationOptionsBuilder(DeleteApiOp.class).build();
            Set<Attribute> attributes = new HashSet<>();
            attributes.add(uid);
            OperationOptions scriptOptions = this.createOperationOptionsBuilder(ScriptOnConnectorApiOp.class).build();
            this.connectorOpHelper.execute(this.resourceConfig.getAction(objectType, Timing.BEFORE_DELETE), attributes, scriptOptions);
            this.connectorFacade.delete(objectClass, uid, operationOptions);
            this.connectorOpHelper.execute(this.resourceConfig.getAction(objectType, Timing.AFTER_DELETE), attributes, scriptOptions);
        } catch (RuntimeException var10) {
            LOG.error(var10, "Error while deleting user");
            responseCode = ExceptionUtil.getResponse(var10);
        }

        LOG.ok("Return [{0}]", responseCode);
        return responseCode;
    }

    public String updateAttributeValue(
            String objectType,
            String attrFieldName
    ) {
        LOG.ok("Enter");

        try {
            this.init(objectType);
            Validation validator = Validation.newInstance(objectType, this.resourceConfig);
            Map<String, String> transformedData = validator.transform(this.form, "modify");
            validator.validate(this.form);
            ResourceExclusion.newInstance(objectType, this.resourceConfig).processExclusions(this.form);
            ObjectClass objectClass = TypeUtil.convertObjectType(objectType);
            ProvEvent provEvent = new ProvEvent(this.form, this.provisioningLookup, objectClass, this.getConnectorSchema());
            Set<Attribute> attributes = provEvent.buildAttributes();
            String status = this.doUpdate(objectType, objectClass, provEvent, attributes);
            if (status != null && status.equals("SUCCESS") && transformedData != null && transformedData.size() > 0) {
                this.writeBackTransformedParentForm(transformedData);
            }
            return status;
        } catch (RuntimeException var9) {
            LOG.error(var9, "Error in updateAttributeValue");
            return ExceptionUtil.getResponse(var9);
        }
    }

    public String updateAttributeValues(
            String objectType,
            String[] labels
    ) {
        LOG.ok("Enter");

        try {
            this.init(objectType);
            Validation validator = Validation.newInstance(objectType, this.resourceConfig);
            Map<String, String> transformedData = validator.transform(this.form, "modify");
            validator.validate(this.form);
            ResourceExclusion.newInstance(objectType, this.resourceConfig).processExclusions(this.form);
            ObjectClass objectClass = TypeUtil.convertObjectType(objectType);
            ProvEvent provEvent = new ProvEvent(this.form, this.provisioningLookup, objectClass, this.getConnectorSchema());
//            Set<Attribute> attributes = provEvent.buildSingleAttributes(labels);
            Set<Attribute> attributes = provEvent.buildAttributes();
            String status = this.doUpdate(objectType, objectClass, provEvent, attributes);
            if (status != null && status.equals("SUCCESS") && transformedData != null && transformedData.size() > 0) {
                this.writeBackTransformedParentForm(transformedData);
            }

            return status;
        } catch (RuntimeException var9) {
            LOG.error(var9, "Error in updateAttributeValue");
            return ExceptionUtil.getResponse(var9);
        }
    }

    public String updateAttributeValues(
            String objectType,
            Map<String, String> fields
    ) {
        LOG.ok("Enter");

        try {
            this.init(objectType);
            Validation validator = Validation.newInstance(objectType, this.resourceConfig);
            validator.validate(this.form);
            ResourceExclusion.newInstance(objectType, this.resourceConfig).processExclusions(this.form);
            Set<String> labels = this.fieldNameToLabel(fields.keySet());
            ObjectClass objectClass = TypeUtil.convertObjectType(objectType);
            ProvEvent provEvent = new ProvEvent(this.form, this.provisioningLookup, objectClass, this.getConnectorSchema());
//            Set<Attribute> attributes = provEvent.buildSingleAttributes(labels.toArray(new String[labels.size()]));
            Set<Attribute> attributes = provEvent.buildAttributes();
            return this.doUpdate(objectType, objectClass, provEvent, attributes);
        } catch (RuntimeException var8) {
            LOG.error(var8, "Error in updateAttributeValue");
            return ExceptionUtil.getResponse(var8);
        }
    }

    public String updateAttributeValues(
            String objectType,
            Map<String, String> fields,
            Map<String, String> oldFields
    ) {
        LOG.ok("Enter");

        try {
            if (fields.size() == 0 && oldFields.size() == 0) {
                LOG.warn("Error in updateAttributeValue - fields and oldFields are empty");
                return "SUCCESS";
            } else {
                this.init(objectType);
                Validation validator = Validation.newInstance(objectType, this.resourceConfig);
                Map<String, String> transformedData = validator.transform(this.form, "modify");
                validator.validate(this.form);
                ResourceExclusion.newInstance(objectType, this.resourceConfig).processExclusions(this.form);
                Set<String> labels = this.fieldNameToLabel(fields.keySet());
                ObjectClass objectClass = TypeUtil.convertObjectType(objectType);
                Schema schema = this.getConnectorSchema();
                ProvEvent provEvent = new ProvEvent(this.form, this.provisioningLookup, objectClass, schema);
//                Set<Attribute> attributes = provEvent.buildSingleAttributes(labels.toArray(new String[0]));
                Set<Attribute> attributes = provEvent.buildAttributes();
                attributes.addAll(this.getCurrentAttributes(objectClass, oldFields));
                attributes.addAll(this.getTemplateAttributes(objectClass, provEvent, oldFields));
                String status = this.doUpdate(objectType, objectClass, provEvent, attributes);
                if (status != null && status.equals("SUCCESS") && transformedData != null && transformedData.size() > 0) {
                    this.writeBackTransformedParentForm(transformedData);
                }

                return status;
            }
        } catch (RuntimeException var12) {
            LOG.error(var12, "Error in updateAttributeValue");
            return ExceptionUtil.getResponse(var12);
        }
    }

    private Set<Attribute> getCurrentAttributes(
            ObjectClass objectClass,
            Map<String, String> oldFields
    ) {
        LOG.ok("Enter");
        Schema schema = this.getConnectorSchema();
        ObjectClassInfo ocInfo = schema == null ? null : schema.findObjectClassInfo(objectClass.getObjectClassValue());
        if (ocInfo != null) {
            AttributeInfo attributeInfo = AttributeInfoUtil.find(OperationalAttributes.CURRENT_ATTRIBUTES, ocInfo.getAttributeInfo());
            if (attributeInfo == null) {
                LOG.ok("CURRENT_ATTRIBUTES not added");
                return new HashSet<>();
            }
        }

        Form currentForm = this.getCurrentForm(this.form, oldFields);
        ProvEvent currentProvEvent = new ProvEvent(currentForm, this.provisioningLookup, objectClass, schema);
        Set<Attribute> currentAttributes = currentProvEvent.buildAttributes();
        Set<Attribute> completeAttributes = new HashSet<>();
        completeAttributes.add(AttributeBuilder.buildCurrentAttributes(objectClass, currentAttributes));
        LOG.ok("CURRENT_ATTRIBUTES added");
        return completeAttributes;
    }

    private Set<Attribute> getTemplateAttributes(
            ObjectClass objectClass,
            ProvEvent provEvent,
            Map<String, String> oldFields
    ) {
        LOG.ok("Enter");
        Set<Attribute> templateAttributes = provEvent.buildTemplateAttributes();
        if (templateAttributes != null && templateAttributes.size() > 0) {
            Form currentForm = this.getCurrentForm(this.form, oldFields);
            ProvEvent currentProvEvent = new ProvEvent(currentForm, this.provisioningLookup, objectClass, this.getConnectorSchema());
            Set<Attribute> currentTemplateAttributes = currentProvEvent.buildTemplateAttributes();
            if (!CollectionUtil.equals(templateAttributes, currentTemplateAttributes)) {
                return templateAttributes;
            }
        }

        return new HashSet<>();
    }

    private Form getCurrentForm(
            Form form,
            Map<String, String> oldFields
    ) {
        if (this.currentForm == null) {
            Set<Form.FieldInfo> fieldInfo = form.getFieldInfo();
            Map<String, String> fieldValues = new HashMap<>(form.getFieldValues());
            Map<String, String> fieldValuesByLabel = new HashMap<>(form.getFieldValuesByLabel());

            for (Map.Entry<String, String> oldField : oldFields.entrySet()) {
                String fieldName = oldField.getKey();
                String fieldLabel = getFieldInfoByName(fieldInfo, fieldName).getLabel();
                fieldValues.put(fieldName, oldField.getValue());
                fieldValuesByLabel.put(fieldLabel, oldField.getValue());
            }

            this.currentForm = new Form(fieldValues, fieldValuesByLabel, fieldInfo, form.getChildFormFieldValues());
        }

        return this.currentForm;
    }

    private static Form.FieldInfo getFieldInfoByName(
            Set<Form.FieldInfo> fieldInfoSet,
            String fieldName
    ) {
        Iterator<Form.FieldInfo> fieldInfoIterator = fieldInfoSet.iterator();

        Form.FieldInfo fieldInfo;
        do {
            if (!fieldInfoIterator.hasNext()) {
                throw new IllegalArgumentException("Invalid field name [" + fieldName + "] provided");
            }

            fieldInfo = fieldInfoIterator.next();
        } while (!fieldName.equals(fieldInfo.getName()));

        return fieldInfo;
    }

    public String updateChildTableValues(
            String objectType,
            String childTableName
    ) {
        LOG.ok("Enter");

        try {
            this.init(objectType);
            Validation validator = Validation.newInstance(objectType, this.resourceConfig);
            validator.validate(this.form);
            ResourceExclusion.newInstance(objectType, this.resourceConfig).processExclusions(this.form);
            ObjectClass objectClass = TypeUtil.convertObjectType(objectType);
            ProvEvent provEvent = new ProvEvent(this.form, this.provisioningLookup, objectClass, null);
            Set<Attribute> attributes = provEvent.buildChildFormAttributes(childTableName);
            attributes.addAll(this.getCurrentAttributes(objectClass, new HashMap<>()));
            return this.doUpdate(objectType, objectClass, provEvent, attributes);
        } catch (RuntimeException var7) {
            LOG.error(var7, "Error in updateAttributeValue");
            return ExceptionUtil.getResponse(var7);
        }
    }

    /**
     * @deprecated
     */
    @Override
    public String enableUser() {
        return null;
    }

    /**
     * @deprecated
     */
    @Override
    public String disableUser() {
        return null;
    }

    public String updatePassword(
            String objectType,
            String passwordFieldLabel,
            String oldPassword
    ) {
        LOG.ok("Enter");

        try {
            this.init(objectType);
            Validation validator = Validation.newInstance(objectType, this.resourceConfig);
            validator.validate(this.form);
            ResourceExclusion.newInstance(objectType, this.resourceConfig).processExclusions(this.form);
            ObjectClass objectClass = TypeUtil.convertObjectType(objectType);
            ProvEvent provEvent = new ProvEvent(this.form, this.provisioningLookup, objectClass, this.getConnectorSchema());
//            Set<Attribute> attributes = provEvent.buildSingleAttributes(passwordFieldLabel);
            Set<Attribute> attributes = provEvent.buildAttributes();
            this.addProvidedOnPasswordChange(provEvent, attributes);
            if (oldPassword != null) {
                GuardedString currentPassword = new GuardedString(oldPassword.toCharArray());
                attributes.add(AttributeBuilder.buildCurrentPassword(currentPassword));
            }

            return this.doUpdate(objectType, objectClass, provEvent, attributes);
        } catch (RuntimeException var9) {
            LOG.error(var9, "Error in updateAttributeValue");
            return ExceptionUtil.getResponse(var9);
        }
    }

    public String enableObject(String objectType) {
        return this.doEnable(objectType, true);
    }

    public String disableObject(String objectType) {
        return this.doEnable(objectType, false);
    }

    private Set<String> fieldNameToLabel(Set<String> names) {
        assert names != null;

        Set<String> labels = new HashSet<>();
        Set<Form.FieldInfo> fieldInfoSet = this.form.getFieldInfo();

        for (Form.FieldInfo fieldInfo : fieldInfoSet) {
            if (names.contains(fieldInfo.getName())) {
                labels.add(fieldInfo.getLabel());
            }
        }

        return labels;
    }

    private void addProvidedOnPasswordChange(
            ProvEvent provEvent,
            Set<Attribute> attributes
    ) {
        Set<FieldMapping> onUpdateMappings = provEvent.getFieldMappingByFlag(FieldFlag.PROVIDEONPSWDCHANGE);
        if (onUpdateMappings.size() > 0) {
            List<String> fieldLabels = new ArrayList<>();

            for (FieldMapping mapping : onUpdateMappings) {
                fieldLabels.add(mapping.getFieldLabel());
            }

            Set<Attribute> attToAdd = provEvent.buildSingleAttributes(fieldLabels.toArray(new String[onUpdateMappings.size()]));
            attributes.addAll(attToAdd);
        }

    }

    public String doEnable(
            String objectType,
            Boolean enabled
    ) {
        LOG.ok("Enter [{0}]", enabled);
        logger.log(ODLLevel.INFO, "Enter [{0}, {1}]", new Object[]{objectType, enabled});
        String status;

        try {
            this.init(objectType);
            String operationType = enabled ? "enable" : "disable";

            Validation validator = Validation.newInstance(objectType, this.resourceConfig);
            validator.validate(this.form);
            Map<String, String> transformedData = validator.transform(this.form, operationType);
            validator.validate(this.form);
            ResourceExclusion.newInstance(objectType, this.resourceConfig).processExclusions(this.form);
            ObjectClass objectClass = TypeUtil.convertObjectType(objectType);
            Schema schema = this.getConnectorSchema();
            ProvEvent provEvent = new ProvEvent(this.form, this.provisioningLookup, objectClass, schema);
            Set<Attribute> attributes = provEvent.buildAttributes();
            List<FieldMapping> childFieldMappings = provEvent.getFieldMappings().stream().filter(fm -> childTableName.equals(fm.getChildForm())).collect(Collectors.toList());
            List<Map<String, String>> childTableData = this.form.getChildFormFieldValues().get(childTableName);
            attributes.add(AdapterUtil.populateRoleField(this.parentRoleFieldLabel, childFieldMappings, childTableData));
            attributes.add(AttributeBuilder.buildEnabled(enabled));
            attributes.addAll(this.getCurrentAttributes(objectClass, new HashMap<>()));
            status = this.doUpdate(objectType, objectClass, provEvent, attributes);
            if (status != null && status.equals("SUCCESS") && transformedData != null && transformedData.size() > 0) {
                this.writeBackTransformedParentForm(transformedData);
            }

            return status;
        } catch (RuntimeException var11) {
            LOG.error(var11, "Error in updateAttributeValue");
            return ExceptionUtil.getResponse(var11);
        }
    }

    private String doUpdate(
            String objectType,
            ObjectClass objectClass,
            ProvEvent provEvent,
            Set<Attribute> attributes
    ) {
        logger.log(ODLLevel.INFO, "Enter doUpdate: {0},{1},{2}", new Object[]{objectType, objectClass, attributes});
        LOG.ok("Enter [{0}, {1}]", objectType, attributes);
        Uid uid = provEvent.getUid();
        String uidFieldLabel = provEvent.getUidFieldLabel();
        LOG.ok("Uid: [{0}]", uid);
        String responseCode = "SUCCESS";
        try {
            //            add role field data
            boolean hasAttributeField = false;
            for (Attribute attr : attributes) {
                if (attr.getName().equals(this.parentRoleFieldLabel)) {
                    hasAttributeField = true;
                }
            }
            if (!hasAttributeField) {
                List<FieldMapping> childFieldMappings = provEvent.getFieldMappings().stream().filter(fm -> childTableName.equals(fm.getChildForm())).collect(Collectors.toList());
                List<Map<String, String>> childTableData = this.form.getChildFormFieldValues().get(childTableName);
                attributes.add(AdapterUtil.populateRoleField(this.parentRoleFieldLabel, childFieldMappings, childTableData));
            }
            //            end add role field data
            OperationOptions scriptOptions = this.createOperationOptionsBuilder(ScriptOnConnectorApiOp.class).build();
            attributes.add(uid);
            this.connectorOpHelper.execute(this.resourceConfig.getAction(objectType, Timing.BEFORE_UPDATE), attributes, scriptOptions);
            OperationOptions operationOptions = this.createOperationOptionsBuilder(UpdateApiOp.class).build();
            attributes.remove(uid);
            Uid retUid = this.connectorFacade.update(objectClass, uid, attributes, operationOptions);
            this.provisioningService.setFormField(this.processInstanceKey, uidFieldLabel, retUid.getUidValue());
            this.writeBack(objectClass, retUid, provEvent);
            attributes.add(uid);
            this.connectorOpHelper.execute(this.resourceConfig.getAction(objectType, Timing.AFTER_UPDATE), attributes, scriptOptions);
        } catch (RuntimeException var11) {
            LOG.error(var11, "Error while updating user");
            responseCode = ExceptionUtil.getResponse(var11);
        }

        LOG.ok("Return [{0}]", responseCode);
        return responseCode;
    }

    private void writeBack(
            ObjectClass objectClass,
            Uid uid,
            ProvEvent provEvent
    ) {
        LOG.ok("Enter [{0}, {1}]", objectClass, uid);
        LOG.ok(" FieldMappings [{0}]", provEvent.getFieldMappingByFlag(FieldFlag.WRITEBACK));
        Set<FieldMapping> writeBackFieldMappings = provEvent.getFieldMappingByFlag(FieldFlag.WRITEBACK);
        if (!writeBackFieldMappings.isEmpty()) {
            Set<FieldMapping> parentWriteBackFieldMappings = new HashSet<>();
            Set<FieldMapping> childWriteBackFieldMappings = new HashSet<>();
            new HashMap();
            for (FieldMapping fieldMapping : writeBackFieldMappings) {
                if (!fieldMapping.isChildForm()) {
                    LOG.ok("adding parent field map [{0}]", fieldMapping);
                    parentWriteBackFieldMappings.add(fieldMapping);
                } else if (fieldMapping.isEmbeddedObject() && this.formQuery.getType().equals(Type.ADD)) {
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

    private void writeBackParentForm(
            Set<FieldMapping> writeBackFieldMappings,
            ConnectorObject connectorObject
    ) {

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

    private void writeBackTransformedParentForm(Map<String, String> transformedDataMap) {
        Map<String, String> attrs = new HashMap<>();
        Map<String, String> fieldNameMap = new HashMap<>();
        Set<Form.FieldInfo> fieldInfoSet = this.form.getFieldInfo();

        for (Form.FieldInfo fieldInfo : fieldInfoSet) {
            fieldNameMap.put(fieldInfo.getLabel(), fieldInfo.getName());
        }

        for (Map.Entry<String, String> stringStringEntry : transformedDataMap.entrySet()) {
            attrs.put(fieldNameMap.get(stringStringEntry.getKey()), stringStringEntry.getValue());
        }

        if (attrs.size() > 0) {
            tcFormInstanceOperationsBean serviceBean = new tcFormInstanceOperationsBean();

            try {
                serviceBean.setProcessFormData(this.processInstanceKey, attrs);
            } catch (Exception var8) {
                LOG.error("Failed to update transformed data into OIM DB for processInstanceKey :" + this.processInstanceKey + "\n");
                LOG.error("-----" + var8);
            }
        }

    }

    private void writeBackChildForm(
            Set<FieldMapping> writeBackFieldMappings,
            ConnectorObject connectorObject,
            ProvEvent provEvent
    ) {
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

                        if (childFieldFlags.contains(FieldFlag.DATE) || StringUtil.isNotEmpty(childColumnValue) && attr.getValue() != null && !attr.getValue().isEmpty() && attr.getValue().get(0) != null && StringUtil.isNotEmpty(String.valueOf(attr.getValue().get(0)))) {
                            if (childFieldFlags.contains(FieldFlag.LOOKUP)) {
                                String childColumnLookupValue = childColumnValue.split("~", 2)[1];
                                if (!childColumnLookupValue.equals(AttributeUtil.getAsStringValue(attr))) {
                                    isMatchingEmbObj = false;
                                }
                            } else if (childFieldFlags.contains(FieldFlag.DATE)) {
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

    private boolean isWriteBackAttribute(
            String attrName,
            Set<FieldMapping> wbFieldMappingsPerChild
    ) {
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

    private Map<String, Map<String, List<Object>>> getConToOimChildFieldNameMap(
            Set<FieldMapping> fieldMappings,
            Set<String> childNames
    ) {
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

    private static Set<String> getAttributesToGet(Set<FieldMapping> fieldMappings) {
        Set<String> attributeNames = new HashSet<>();

        for (FieldMapping fieldMapping : fieldMappings) {
            attributeNames.add(fieldMapping.getAttributeName());
        }

        return attributeNames;
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
}

