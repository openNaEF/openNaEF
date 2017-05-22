package voss.core.server.builder;

import naef.dto.NaefDto;
import voss.core.common.diff.DiffType;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface CommandBuilder extends Serializable {
    BuildResult buildCommand() throws IOException, InventoryException, ExternalServiceException;

    BuildResult buildDeleteCommand() throws IOException, InventoryException, ExternalServiceException;

    ShellCommands getCommand();

    BuildResult getBuildResult();

    boolean isBuilt();

    CommandBuilder getDependentBuilder();

    void setDependentBuilder(CommandBuilder builder);

    List<CommandBuilder> getChildBuilders();

    void addChildBuilder(CommandBuilder builder);

    boolean executed();

    void setExecuted(boolean value);

    void setVersionCheck(boolean value);

    boolean hasChange();

    List<String> getChangedAttributes();

    List<ChangeUnit> getChangeUnits();

    String getDiffContent();

    String getObjectType();

    void addComplementBuilder(Class<? extends ComplementBuilder> builderClass, String targetName);

    void addComplementBuilder(Class<? extends ComplementBuilder> builderClass, NaefDto target);

    List<ComplementBuilder> getComplementBuilders();

    void setValue(String key, String value);

    void setValue(String key, Integer value);

    void setValue(String key, Long value);

    void setValue(String key, Boolean value);

    void addValue(String attrName, String value);

    void appendValue(String attrName, String value);

    void removeValue(String attrName, String value);

    void replaceValues(String attrName, List<String> values, boolean keepCurrentValue);

    void resetValues(String attrName);

    String getValue(String attrName);

    String getUpdatedValue(String attrName);

    Map<String, DiffType> getValues(String attrName);

    Map<String, DiffType> getUpdatedValues(String attrName);

    String getEditor();
}