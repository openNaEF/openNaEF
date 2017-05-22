package voss.multilayernms.inventory.config;

import java.util.List;
import java.util.Properties;

public interface INmsCoreInventoryObjectConfiguration {

    public String getIdFieldName();

    public void setIdFieldName(String idFieldName);

    public List<String> getOperationStatusFieldNames();

    public void setOperationStatusFieldNames(List<String> operationStatusFieldNames);

    public List<String> getFields();

    public void setFields(List<String> fields);

    public List<String> getPropertyFields();

    public void setPropertyFields(List<String> fields);

    public Properties getDisplayNames();

    public void setDisplayNames(Properties names);

    public List<String> getFilteringFields();

    public void setFilteringFields(List<String> fields);

    public Properties getConditionOfFiltering();

    public void setConditionOfFiltering(Properties conditions);

}