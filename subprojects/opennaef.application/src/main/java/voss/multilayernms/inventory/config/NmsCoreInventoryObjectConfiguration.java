package voss.multilayernms.inventory.config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration.Node;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.config.ServiceConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class NmsCoreInventoryObjectConfiguration extends ServiceConfiguration implements INmsCoreInventoryObjectConfiguration {
    private static final Logger log = LoggerFactory.getLogger(NmsCoreInventoryObjectConfiguration.class);

    public static final String KEY_ID_FIELD_NAME = "id-field-name";
    public static final String KEY_OPERATION_STATUS_FIELD_NAMES = "operation-status-field-names";

    public static final String KEY_FIELDS = "fields";
    public static final String KEY_PROPERTY_FIELDS = "property-fields";
    public static final String KEY_DISPLAY_NAME = "display-name";
    public static final String KEY_FILTERING_FIELDS = "filtering-fields";
    public static final String KEY_CONDITION_OF_FILTERING = "condition-of-filtering";


    public static final String PROP_ATTR_KEY = "key";

    private String idFieldName = null;
    private List<String> operationStatusFieldNames = null;
    private List<String> fields = null;
    private List<String> propertyFields = null;
    private Properties displayNames = null;
    private List<String> filteringFields = null;
    private Properties conditionOfFiltering = null;

    private String diffsetStoreDirectoryName = "./diffset";

    public NmsCoreInventoryObjectConfiguration(String fileName, String name, String description)
            throws IOException {
        super(fileName, name, description);
        reloadConfiguration();
        File dir = new File(this.diffsetStoreDirectoryName);
        if (!dir.exists()) {
            boolean result = dir.mkdirs();
            if (!result) {
                throw new IOException("failed to create diffset directory:" + this.diffsetStoreDirectoryName);
            }
        }
    }

    @Override
    protected void reloadConfigurationInner() throws IOException {
        try {
            XMLConfiguration config = new XMLConfiguration();
            config.setDelimiterParsingDisabled(true);
            config.load(getConfigFile());

            log.debug("loading config:" + getConfigName());

            for (Object obj : config.getRootNode().getChildren()) {
                Node node = (Node) obj;

                if (node.getName().equals(KEY_ID_FIELD_NAME)) {
                    setIdFieldName((String) node.getValue());
                } else if (node.getName().equals(KEY_OPERATION_STATUS_FIELD_NAMES)) {
                    List<String> values = new ArrayList<String>();

                    for (Object value : node.getChildren()) {
                        values.add((String) ((Node) value).getValue());
                    }
                    setOperationStatusFieldNames(values);
                } else if (node.getName().equals(KEY_FIELDS)) {
                    List<String> values = new ArrayList<String>();

                    for (Object value : node.getChildren()) {
                        values.add((String) ((Node) value).getValue());
                    }
                    setFields(values);
                } else if (node.getName().equals(KEY_PROPERTY_FIELDS)) {
                    List<String> values = new ArrayList<String>();

                    for (Object value : node.getChildren()) {
                        values.add((String) ((Node) value).getValue());
                    }
                    setPropertyFields(values);
                } else if (node.getName().equals(KEY_FILTERING_FIELDS)) {
                    List<String> values = new ArrayList<String>();

                    for (Object value : node.getChildren()) {
                        values.add((String) ((Node) value).getValue());
                    }
                    setFilteringFields(values);
                } else if (node.getName().equals(KEY_DISPLAY_NAME)) {
                    Properties props = new Properties();
                    for (Object prop : node.getChildren()) {
                        String key = null;
                        for (Object attr : ((Node) prop).getAttributes()) {
                            if (((Node) attr).getName().equals(PROP_ATTR_KEY)) {
                                key = (String) ((Node) attr).getValue();
                            }
                        }
                        if (key != null) {
                            props.put(key, (String) ((Node) prop).getValue());
                        }
                    }
                    setDisplayNames(props);
                } else if (node.getName().equals(KEY_CONDITION_OF_FILTERING)) {
                    Properties props = new Properties();
                    for (Object prop : node.getChildren()) {
                        String key = null;
                        for (Object attr : ((Node) prop).getAttributes()) {
                            if (((Node) attr).getName().equals(PROP_ATTR_KEY)) {
                                key = (String) ((Node) attr).getValue();
                            }
                        }
                        if (key != null) {
                            props.put(key, (String) ((Node) prop).getValue());
                        }
                    }
                    setConditionOfFiltering(props);
                } else if (!loadExtraConfigration()) {
                    log.info("unknown node:" + node.getName());
                }

            }
        } catch (ConfigurationException e) {
            log.error(e.getMessage(), e);
            throw new IOException("failed to reload config: " + getConfigFile());
        }

    }

    protected boolean loadExtraConfigration() throws IOException {
        return false;
    }

    public String getIdFieldName() {
        return idFieldName;
    }

    public void setIdFieldName(String idFieldName) {
        this.idFieldName = idFieldName;
    }

    public List<String> getOperationStatusFieldNames() {
        return operationStatusFieldNames;
    }

    public void setOperationStatusFieldNames(List<String> operationStatusFieldNames) {
        this.operationStatusFieldNames = operationStatusFieldNames;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public List<String> getPropertyFields() {
        return propertyFields;
    }

    public void setPropertyFields(List<String> fields) {
        this.propertyFields = fields;
    }

    public Properties getDisplayNames() {
        return displayNames;
    }

    public void setDisplayNames(Properties names) {
        this.displayNames = names;
    }

    public List<String> getFilteringFields() {
        return filteringFields;
    }

    public void setFilteringFields(List<String> fields) {
        this.filteringFields = fields;
    }

    public Properties getConditionOfFiltering() {
        return conditionOfFiltering;
    }

    public void setConditionOfFiltering(Properties conditions) {
        this.conditionOfFiltering = conditions;
    }

}

