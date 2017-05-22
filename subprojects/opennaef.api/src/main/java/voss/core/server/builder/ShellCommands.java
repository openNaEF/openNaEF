package voss.core.server.builder;

import naef.dto.NaefDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.Attribute;
import tef.skelton.dto.EntityDto;
import voss.core.server.builder.ChangeUnit.ChangeUnitType;
import voss.core.server.constant.LogConstants;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.Util;

import java.io.Serializable;
import java.util.*;

public class ShellCommands implements Commands, Serializable {
    private static final long serialVersionUID = 1L;
    private final String editorName;
    private final List<String> versionCheckCommands = new ArrayList<String>();
    private final List<String> commands = new ArrayList<String>();
    private final List<String> warnings = new ArrayList<String>();
    private boolean doAssertion = true;
    private boolean valueAssertion = false;
    private final Map<String, Map<String, ChangedValue>> preChangedAttributeValues = new HashMap<String, Map<String, ChangedValue>>();

    public ShellCommands(String editorName) {
        this.editorName = editorName;
    }

    @Override
    public List<String> getAssertions() {
        if (doAssertion) {
            return buildVersionAssertionCommand();
        } else if (valueAssertion) {
            return buildValueAssertionCommand();
        }
        return new ArrayList<String>();
    }

    private List<String> buildVersionAssertionCommand() {
        List<String> result = new ArrayList<String>();
        result.addAll(this.versionCheckCommands);
        return result;
    }

    private List<String> buildValueAssertionCommand() {
        List<String> result = new ArrayList<String>();
        for (Map.Entry<String, Map<String, ChangedValue>> entry : this.preChangedAttributeValues.entrySet()) {
            String targetAbsoluteName = entry.getKey();
            String contextCmd = InventoryBuilder.translate(CMD.CONTEXT_BY_ABSOLUTE_NAME,
                    CMD.ARG_NAME, targetAbsoluteName);
            result.add(contextCmd);
            Map<String, ChangedValue> attributes = entry.getValue();
            for (Map.Entry<String, ChangedValue> attribute : attributes.entrySet()) {
                ChangedValue changed = attribute.getValue();
                switch (changed.getType()) {
                    case SIMPLE:
                        buildSimpleAssertCommand(changed, result);
                        break;
                    case COLLECTION:
                        buildCollectionAssertCommand(changed, result);
                        break;
                    case MAP:
                        throw new IllegalStateException("not implemented.");
                }
            }
        }
        return result;
    }

    private void buildSimpleAssertCommand(ChangedValue unit, List<String> result) {
        String value_ = unit.getValue();
        String key_ = unit.getKey();
        if (value_ != null && value_.length() > 0) {
            String cmd = InventoryBuilder.translate(CMD.ASSERT_ATTRIBUTE_IS,
                    CMD.ARG_ATTR, key_, CMD.ARG_VALUE, value_);
            result.add(cmd);
        } else {
            String cmd;
            if (unit.isString()) {
                cmd = InventoryBuilder.translate(CMD.ASSERT_ATTRIBUTE_IS_NULL_OR_EMPTY,
                        CMD.ARG_ATTR, key_);
            } else {
                cmd = InventoryBuilder.translate(CMD.ASSERT_ATTRIBUTE_IS_NULL,
                        CMD.ARG_ATTR, key_);
            }
            result.add(cmd);
        }
    }

    private void buildCollectionAssertCommand(ChangedValue unit, List<String> result) {
        String key_ = unit.getKey();
        List<String> values = unit.getValues();
        for (String value_ : values) {
            if (value_ == null || value_.length() == 0) {
                continue;
            }
            String cmd = InventoryBuilder.translate(CMD.ASSERT_ATTRIBUTE_CONTAINS,
                    CMD.ARG_ATTR, key_, CMD.ARG_VALUE, value_);
            result.add(cmd);
        }
    }

    @Override
    public List<String> getRawCommands() {
        List<String> rawCommands = new ArrayList<String>();
        rawCommands.addAll(this.commands);
        return rawCommands;
    }

    @Override
    public List<String> getCommands() {
        List<String> withVersionCheck = new ArrayList<String>();
        if (doAssertion || valueAssertion) {
            withVersionCheck.addAll(getAssertions());
        }
        withVersionCheck.addAll(this.commands);
        return withVersionCheck;
    }

    private String getMvoVersionCheckCommand(NaefDto dto) {
        if (doAssertion) {
            return "assert-mvo-version " + DtoUtil.getMvoId(dto).toString() + " " + DtoUtil.getMvoTimestamp(dto).getIdString();
        } else {
            return null;
        }
    }

    public void addVersionCheckTarget(NaefDto... dtos) {
        for (NaefDto dto : dtos) {
            if (dto == null) {
                continue;
            }
            String cmd = getMvoVersionCheckCommand(dto);
            if (this.versionCheckCommands.contains(cmd)) {
                continue;
            }
            this.versionCheckCommands.add(cmd);
        }
    }

    public void addVersionCheckTarget(List<String> commands) {
        for (String command : commands) {
            if (command == null) {
                continue;
            }
            if (this.versionCheckCommands.contains(command)) {
                return;
            }
            this.versionCheckCommands.add(command);
        }
    }

    public List<String> getVersionCheckTargets() {
        return new ArrayList<String>(this.versionCheckCommands);
    }

    public void addCommand(String command) {
        if (command == null) {
            return;
        }
        commands.add(command);
    }

    public void addCommands(List<String> commands) {
        if (commands == null) {
            return;
        }
        this.commands.addAll(commands);
    }

    public void addBuilder(CommandBuilder builder) {
        if (builder == null) {
            return;
        }
        BuildResult r = builder.getBuildResult();
        if (null == r) {
            throw new IllegalStateException("not built: " + builder.getObjectType() + "@" + builder.toString());
        } else if (r == BuildResult.NO_CHANGES) {
            log("* No Changes: " + builder.getObjectType() + "@" + builder.toString());
            return;
        } else if (r == BuildResult.SUCCESS) {
            log("+ builder Merged: " + builder.getObjectType() + "@" + builder.toString());
            ShellCommands cmd = builder.getCommand();
            this.versionCheckCommands.addAll(cmd.versionCheckCommands);
            this.commands.addAll(cmd.commands);
            this.warnings.addAll(cmd.warnings);
        } else {
            log().warn("- unexpected status: " + r + "; " + builder.getObjectType() + "@" + builder.toString());
        }
    }

    public void addLastEditCommands() {
        InventoryBuilder.buildAttributeSetOrReset(this, ATTR.LAST_EDITOR, editorName);
        InventoryBuilder.buildAttributeSetOrReset(this, ATTR.LAST_EDIT_TIME, DtoUtil.getMvoDateFormat().format(new Date()));
    }

    @Override
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    @Override
    public List<String> getWarnings() {
        return this.warnings;
    }

    @Override
    public void setVersionCheck(boolean versionCheckRequired) {
        this.doAssertion = versionCheckRequired;
    }

    @Override
    public void setValueCheck(boolean checkRequired) {
        this.valueAssertion = checkRequired;
    }

    @Override
    public void setValueCheckContents(List<ChangeUnit> changeUnits) {
        if (changeUnits == null) {
            throw new IllegalArgumentException("target is null.");
        }
        Map<Class<?>, Map<String, Attribute<?, ?>>> attributeMaps = new HashMap<Class<?>, Map<String, Attribute<?, ?>>>();
        for (ChangeUnit unit : changeUnits) {
            NaefDto target = unit.getTarget();
            Map<String, Attribute<?, ?>> attributeMap = attributeMaps.get(target.getClass());
            if (attributeMap == null) {
                attributeMap = DtoUtil.getAttributeMap(target);
                attributeMaps.put(target.getClass(), attributeMap);
            }
            Map<String, ChangedValue> map = Util.getOrCreateMap(this.preChangedAttributeValues, target.getAbsoluteName());
            String attrName = unit.getKey();
            boolean isString = false;
            Attribute<?, ?> attr = attributeMap.get(attrName);
            if (attr == null) {
                continue;
            } else if (attr.getType().getClass().equals(String.class)) {
                isString = true;
            }
            ChangedValue value = new ChangedValue(attrName, unit.getChangeUnitType(), isString);
            switch (unit.getChangeUnitType()) {
                case SIMPLE:
                    SimpleChangeUnit simple = SimpleChangeUnit.class.cast(unit);
                    value.setValue(simple.getPreChangedValue());
                    break;
                case COLLECTION:
                    CollectionChangeUnit col = CollectionChangeUnit.class.cast(unit);
                    value.addValues(col.getPreChangedValues());
                    break;
                case MAP:
                    throw new IllegalStateException("not implemented.");
            }
            map.put(attrName, value);
        }
    }

    public void putValueCheckContents(Map<String, Map<String, ChangedValue>> map) {
        this.preChangedAttributeValues.putAll(map);
    }

    public Map<String, Map<String, ChangedValue>> getValueCheckContents() {
        return this.preChangedAttributeValues;
    }

    @Override
    public boolean isConditional() {
        return false;
    }

    @Override
    public void evaluate() {
    }

    public void log(String log) {
        if (log == null) {
            return;
        }
        commands.add("### " + log);
    }

    public void log(EntityDto dto) {
        if (dto == null) {
            return;
        }
        log(DtoUtil.toDebugString(dto));
    }

    private Logger log() {
        return LoggerFactory.getLogger(LogConstants.LOG_COMMAND);
    }

    public void logCommands() {
        Logger log = log();
        log.info("commands ----");
        int count = 0;
        for (String command : getCommands()) {
            count++;
            if (command == null) {
                log.info("");
            }
            if (command.contains("パスワード")) {
                command = escape(command, "パスワード");
            } else if (command.toLowerCase().contains("password")) {
                command = escape(command, "password");
            }
            command = "[" + count + "] " + command;
            log().info(command);
        }
        log().info("----");
    }

    private String escape(String command, String escapeWord) {
        StringBuilder sb = new StringBuilder();
        boolean occured = false;
        for (String s : command.split(" ")) {
            if (occured) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append("****");
            } else {
                if (s != null) {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(s);
                    occured = s.toLowerCase().contains(escapeWord);
                }
            }
        }
        return sb.toString();
    }

    private static class ChangedValue implements Serializable {
        private static final long serialVersionUID = 1L;
        private final ChangeUnitType type;
        private final String key;
        private String value;
        private final List<String> values = new ArrayList<String>();
        private final boolean isString;

        public ChangedValue(String key, ChangeUnitType type, boolean isString) {
            this.key = key;
            this.type = type;
            this.isString = isString;
        }

        public String getKey() {
            return this.key;
        }

        public ChangeUnitType getType() {
            return this.type;
        }

        public boolean isString() {
            return this.isString;
        }

        public String getValue() {
            return this.value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public void addValues(List<String> values) {
            this.values.addAll(values);
        }

        public List<String> getValues() {
            return this.values;
        }
    }
}