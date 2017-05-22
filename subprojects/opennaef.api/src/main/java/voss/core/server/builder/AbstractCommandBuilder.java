package voss.core.server.builder;

import naef.dto.NaefDto;
import naef.dto.NodeElementDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.common.diff.DiffEntry;
import voss.core.common.diff.DiffType;
import voss.core.common.diff.DiffUtil;
import voss.core.server.config.BuilderConfiguration;
import voss.core.server.database.ATTR;
import voss.core.server.database.CoreConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.*;


public abstract class AbstractCommandBuilder implements CommandBuilder {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(AbstractCommandBuilder.class);

    private CommandBuilder dependent = null;
    private final List<CommandBuilder> dependers = new ArrayList<CommandBuilder>();
    protected final String editorName;
    protected final NaefDto target;
    protected final ShellCommands cmd;
    protected final Class<? extends NaefDto> requiredClass;
    protected Class<? extends NaefDto> constraint;
    protected boolean constraintEnabled = false;
    protected boolean ignoreUnsupportedAttribute = false;
    protected final Map<String, AttributeMeta> supportedAttributeNames = new HashMap<String, AttributeMeta>();
    protected BuildResult result;

    protected List<ComplementBuilderEntry> complementBuilderEntries = new ArrayList<ComplementBuilderEntry>();

    protected boolean built = false;
    protected boolean executed = false;
    protected boolean versionCheck = true;

    protected final Map<String, String> attributes = new HashMap<String, String>();
    protected final Map<String, Map<String, DiffType>> listAttributes = new HashMap<String, Map<String, DiffType>>();
    protected final Map<String, Map<String, String>> mapAttributes = new HashMap<String, Map<String, String>>();

    protected final List<ChangeUnit> changeUnits = new ArrayList<ChangeUnit>();

    public AbstractCommandBuilder(Class<? extends NaefDto> required, NaefDto target, String editorName) {
        this(required, target, editorName, true);
    }

    public AbstractCommandBuilder(Class<? extends NaefDto> required, NaefDto target, String editorName, boolean versionCheckRequired) {
        if (required == null) {
            throw new IllegalArgumentException("required-class is mandatory.");
        }
        if (editorName == null) {
            throw new IllegalArgumentException("editorName is mandatory.");
        }
        if (target != null) {
            if (!required.isAssignableFrom(target.getClass())) {
                throw new IllegalArgumentException("target doesn't match with requred-class. " +
                        "required=" + required.getName() + ", target=" + target.getAbsoluteName());
            }
        }
        this.requiredClass = required;
        this.target = target;
        this.cmd = new ShellCommands(editorName);
        this.cmd.setVersionCheck(versionCheckRequired);
        if (target != null) {
            this.cmd.addVersionCheckTarget(target);
        }
        this.editorName = editorName;
        this.cmd.addCommand("# builder: " + this.getClass().getSimpleName());
        if (editorName != null) {
            this.cmd.addCommand("# - edited by " + editorName);
        }
        BuilderConfiguration config = BuilderConfiguration.getInstance();
        if (config.isSystemUserUpdateEnabled()) {
            String time = DtoUtil.getMvoDateFormatForMilliSecond().format(new Date());
            InventoryBuilder.changeContext(cmd, ATTR.TYPE_SYSTEM_USER, editorName);
            InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.LAST_EDIT_TIME, time);
        }
    }

    @SuppressWarnings({"unchecked"})
    public void setConstraint(Class<? extends NaefDto> cls) {
        if (cls == null) {
            throw new IllegalArgumentException();
        }
        if (!requiredClass.isAssignableFrom(cls)) {
            throw new IllegalStateException("cls is not sub-type of "
                    + requiredClass.getName() + ", cls=" + cls.getName());
        }
        this.constraint = (Class<NaefDto>) cls;
        try {
            CoreConnector conn = CoreConnector.getInstance();
            List<AttributeMeta> result = conn.getSupportedAttributeMeta(constraint);
            for (AttributeMeta meta : result) {
                this.supportedAttributeNames.put(meta.getAttrName(), meta);
            }
        } catch (Exception e) {
            log.error("cannot contact facade.", e);
        }
    }

    @Override
    public BuildResult buildCommand() throws IOException, ExternalServiceException, InventoryException {
        checkBuilt();
        BuildResult result = buildCommandInner();
        built();
        return setResult(result);
    }

    abstract protected BuildResult buildCommandInner() throws IOException, ExternalServiceException, InventoryException;

    @Override
    public BuildResult buildDeleteCommand() throws IOException, ExternalServiceException, InventoryException {
        checkBuilt();
        BuildResult result = buildDeleteCommandInner();
        built();
        return setResult(result);
    }

    abstract protected BuildResult buildDeleteCommandInner() throws IOException, ExternalServiceException, InventoryException;

    public void setIgnoreUnsupportedAttribute(boolean value) {
        this.ignoreUnsupportedAttribute = value;
    }

    public boolean isIgnoreUnsupportedAttribute() {
        return this.ignoreUnsupportedAttribute;
    }

    public void setConstraintEnable(boolean value) {
        this.constraintEnabled = value;
    }

    public void setVersionCheck(boolean value) {
        this.cmd.setVersionCheck(value);
    }

    protected void built() {
        this.built = true;
    }

    @Override
    public boolean isBuilt() {
        return this.built;
    }

    protected BuildResult setResult(BuildResult result) {
        this.result = result;
        return result;
    }

    @Override
    public ShellCommands getCommand() {
        if (!built) {
            throw new IllegalStateException("not built. ->" + this.getClass().getSimpleName());
        }
        if (!hasChange()) {
            return new ShellCommands(editorName);
        }
        return this.cmd;
    }

    @Override
    public boolean executed() {
        return executed;
    }

    public Set<String> getSupportedAttributes() {
        Set<String> result = new HashSet<String>();
        for (AttributeMeta meta : this.supportedAttributeNames.values()) {
            result.add(meta.getAttrName());
        }
        return result;
    }

    protected boolean isSupportedAttributeName(String name) {
        if (name == null) {
            return false;
        }
        if (!this.constraintEnabled) {
            return true;
        }
        return this.supportedAttributeNames.containsKey(name);
    }

    protected boolean isListAttribute(String attrName) {
        if (attrName == null) {
            return false;
        }
        AttributeMeta meta = this.supportedAttributeNames.get(attrName);
        if (meta == null) {
            return false;
        }
        Class<?> type = meta.getType();
        if (List.class.isAssignableFrom(type)) {
            return true;
        }
        return false;
    }

    protected boolean isSetAttribute(String attrName) {
        if (attrName == null) {
            return false;
        }
        AttributeMeta meta = this.supportedAttributeNames.get(attrName);
        if (meta == null) {
            return false;
        }
        Class<?> type = meta.getType();
        if (Set.class.isAssignableFrom(type)) {
            return true;
        }
        return false;
    }

    protected boolean isMapAttribute(String attrName) {
        if (attrName == null) {
            return false;
        }
        AttributeMeta meta = this.supportedAttributeNames.get(attrName);
        if (meta == null) {
            return false;
        }
        Class<?> type = meta.getType();
        if (Map.class.isAssignableFrom(type)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean hasChange() {
        return this.changeUnits.size() > 0;
    }

    @Override
    public List<String> getChangedAttributes() {
        List<String> result = new ArrayList<String>();
        for (ChangeUnit unit : this.changeUnits) {
            result.add(unit.getKey());
        }
        return result;
    }

    public void setValues(Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            setValueInner(key, value);
        }
    }

    public void setValue(String attrName, Long value) {
        String val = (value == null ? null : value.toString());
        setValueInner(attrName, val);
    }

    public void setValue(String attrName, Integer value) {
        String val = (value == null ? null : value.toString());
        setValueInner(attrName, val);
    }

    public void setValue(String attrName, Boolean value) {
        String val = (value == null ? null : value.toString());
        setValueInner(attrName, val);
    }

    public void setValue(String attrName, String value) {
        setValueInner(attrName, value);
    }

    private void setValueInner(String attrName, String value) {
        if (this.constraintEnabled && !this.supportedAttributeNames.containsKey(attrName)) {
            if (this.ignoreUnsupportedAttribute) {
                log.debug("attribute set request ignored: " + attrName + "=" + value);
                return;
            } else {
                throw new IllegalArgumentException("not supported attribute: " + attrName);
            }
        }
        value = Util.stringToNull(value);
        String original = DtoUtil.getStringOrNull(target, attrName);
        if (original != null && original.equals(value)) {
            return;
        } else if (original == null && value == null) {
            return;
        }
        this.attributes.put(attrName, value);
        recordChange(attrName, original, value);
    }

    @Override
    public void addValue(String attrName, String value) {
        addValueInner(attrName, value);
    }

    @Override
    public void appendValue(String attrName, String value) {
        appendValueInner(attrName, value);
    }

    @Override
    public void removeValue(String attrName, String value) {
        removeValueInner(attrName, value);
    }

    @Override
    public void resetValues(String attrName) {
        resetValuesInner(attrName);
    }

    @Override
    public void replaceValues(String attrName, List<String> values, boolean keepCurrentValue) {
        replaceValuesInner(attrName, values, keepCurrentValue);
    }

    protected void addValueInner(String attrName, String value) {
        attrName = Util.stringToNull(attrName);
        value = Util.stringToNull(value);
        if (attrName == null) {
            return;
        } else if (value == null) {
            return;
        } else if (!isProceedableAsListOrSet(attrName, value)) {
            return;
        }
        List<String> added = new ArrayList<String>();
        Map<String, DiffType> listValues = this.listAttributes.get(attrName);
        if (listValues == null) {
            listValues = initListAttribute(attrName);
        }
        if (listValues.containsKey(value)) {
            DiffType t = listValues.get(value);
            if (t == null) {
                throw new IllegalStateException("no diff-type: [" + attrName + "] add [" + value + "]");
            }
            switch (t) {
                case ADD:
                    break;
                case REMOVE:
                    listValues.put(value, DiffType.KEEP);
                    break;
                case KEEP:
                    break;
            }
        } else {
            listValues.put(value, DiffType.ADD);
            added.add(value);
        }
        recordChanges(attrName, new ArrayList<String>(), added);
    }

    protected void appendValueInner(String attrName, String value) {
        attrName = Util.stringToNull(attrName);
        value = Util.stringToNull(value);
        if (attrName == null) {
            return;
        } else if (value == null) {
            return;
        } else if (!isProceedableAsListOrSet(attrName, value)) {
            return;
        }
        List<String> added = new ArrayList<String>();
        Map<String, DiffType> listValues = this.listAttributes.get(attrName);
        if (listValues == null) {
            listValues = initListAttribute(attrName);
        }
        listValues.put(value, DiffType.ADD);
        added.add(value);
        recordChanges(attrName, new ArrayList<String>(), added);
    }

    protected void removeValueInner(String attrName, String value) {
        attrName = Util.stringToNull(attrName);
        value = Util.stringToNull(value);
        if (attrName == null) {
            return;
        } else if (value == null) {
            return;
        } else if (!isProceedableAsListOrSet(attrName, value)) {
            return;
        }
        Map<String, DiffType> listValues = this.listAttributes.get(attrName);
        if (listValues == null) {
            listValues = initListAttribute(attrName);
        }
        List<String> removed = new ArrayList<String>();
        if (listValues.containsKey(value)) {
            DiffType t = listValues.get(value);
            switch (t) {
                case ADD:
                    listValues.remove(value);
                    removed.add(value);
                    break;
                case REMOVE:
                    break;
                case KEEP:
                    listValues.put(value, DiffType.REMOVE);
                    removed.add(value);
                    break;
            }
        } else {
        }
        recordChanges(attrName, removed, new ArrayList<String>());
    }

    protected void resetValuesInner(String attrName) {
        attrName = Util.stringToNull(attrName);
        if (attrName == null) {
            return;
        } else if (!isProceedableAsListOrSet(attrName, "-")) {
            return;
        }
        Map<String, DiffType> listValues = this.listAttributes.get(attrName);
        if (listValues == null) {
            listValues = initListAttribute(attrName);
        }
        Map<String, DiffType> _temp = new LinkedHashMap<String, DiffType>();
        List<String> values = new ArrayList<String>();
        for (Map.Entry<String, DiffType> entry : listValues.entrySet()) {
            String key = entry.getKey();
            _temp.put(key, DiffType.REMOVE);
            values.add(key);
        }
        listValues.clear();
        listValues.putAll(_temp);
        recordChanges(attrName, values, new ArrayList<String>());
    }

    protected void replaceValuesInner(String attrName, List<String> values, boolean keepCurrentValue) {
        attrName = Util.stringToNull(attrName);
        if (attrName == null) {
            return;
        } else if (!isProceedableAsListOrSet(attrName, "-")) {
            return;
        }
        Map<String, DiffType> listValues = this.listAttributes.get(attrName);
        if (listValues == null) {
            listValues = initListAttribute(attrName);
        }
        Map<String, DiffType> _temp = new LinkedHashMap<String, DiffType>();
        List<String> added = new ArrayList<String>();
        List<String> removed = new ArrayList<String>();
        if (keepCurrentValue) {
            List<String> _remains = new ArrayList<String>(values);
            for (Map.Entry<String, DiffType> currentEntry : listValues.entrySet()) {
                String currentValue = currentEntry.getKey();
                if (values.contains(currentValue)) {
                    _temp.put(currentValue, DiffType.KEEP);
                    _remains.remove(currentValue);
                    continue;
                } else {
                    _temp.put(currentValue, DiffType.REMOVE);
                    removed.add(currentValue);
                }
            }
            for (String value : _remains) {
                _temp.put(value, DiffType.ADD);
                added.add(value);
            }
        } else {
            for (Map.Entry<String, DiffType> entry : listValues.entrySet()) {
                String key = entry.getKey();
                _temp.put(key, DiffType.REMOVE);
                removed.add(key);
            }
            for (String value : values) {
                _temp.put(value, DiffType.ADD);
                added.add(value);
            }
        }
        listValues.clear();
        listValues.putAll(_temp);
        recordChanges(attrName, added, removed);
    }

    protected Map<String, DiffType> initListAttribute(String attrName) {
        List<String> listValues = DtoUtil.getStringList(this.target, attrName);
        Map<String, DiffType> result = new LinkedHashMap<String, DiffType>();
        this.listAttributes.put(attrName, result);
        for (String listValue : listValues) {
            result.put(listValue, DiffType.KEEP);
        }
        return result;
    }

    protected boolean isProceedableAsListOrSet(String attrName, String value) {
        if (this.constraintEnabled && !this.supportedAttributeNames.containsKey(attrName)) {
            if (this.ignoreUnsupportedAttribute) {
                log.debug("attribute set request ignored: " + attrName + "=" + value);
                return false;
            } else {
                throw new IllegalStateException("not supported attribute: " + attrName);
            }
        }
        if (!isSetAttribute(attrName) && !isListAttribute(attrName)) {
            throw new IllegalStateException("not list/set attribute: " + attrName);
        }
        return true;
    }

    protected List<String> getAddedListValues(String attrName) {
        Map<String, DiffType> listValues = this.listAttributes.get(attrName);
        List<String> result = new ArrayList<String>();
        for (Map.Entry<String, DiffType> entry : listValues.entrySet()) {
            String value = entry.getKey();
            DiffType type = entry.getValue();
            if (type == DiffType.ADD) {
                result.add(value);
            }
        }
        return result;
    }

    protected List<String> getRemovedListValues(String attrName) {
        Map<String, DiffType> listValues = this.listAttributes.get(attrName);
        List<String> result = new ArrayList<String>();
        for (Map.Entry<String, DiffType> entry : listValues.entrySet()) {
            String value = entry.getKey();
            DiffType type = entry.getValue();
            if (type == DiffType.REMOVE) {
                result.add(value);
            }
        }
        return result;
    }

    protected List<String> getNewListValues(String attrName) {
        Map<String, DiffType> listValues = this.listAttributes.get(attrName);
        List<String> result = new ArrayList<String>();
        for (Map.Entry<String, DiffType> entry : listValues.entrySet()) {
            String value = entry.getKey();
            DiffType type = entry.getValue();
            if (type == null || type == DiffType.REMOVE) {
                continue;
            }
            result.add(value);
        }
        return result;
    }

    protected List<String> getCurrentListValues(String attrName) {
        return DtoUtil.getStringList(this.target, attrName);
    }

    protected void putMap(String attrName, String key, String value) {
        if (attrName == null) {
            throw new IllegalArgumentException();
        } else if (key == null || value == null) {
            return;
        }
        Map<String, String> map = getCurrentMap(attrName);
        map.put(key, value);
    }

    protected void removeMap(String attrName, String key) {
        if (attrName == null) {
            throw new IllegalArgumentException();
        } else if (key == null) {
            return;
        }
        Map<String, String> map = getCurrentMap(attrName);
        map.remove(key);
    }

    public void resetMap(String attrName) {
        Map<String, String> map = getCurrentMap(attrName);
        map.clear();
    }

    protected Map<String, String> getCurrentMap(String attrName) {
        if (!isProceedableAsMap(attrName)) {
            return new HashMap<String, String>();
        }
        Map<String, String> map = this.mapAttributes.get(attrName);
        if (map == null) {
            map = initMapAttribute(attrName);
        }
        return map;
    }

    protected Map<String, String> initMapAttribute(String attrName) {
        Map<String, String> values = DtoUtil.getStringMap(this.target, attrName);
        this.mapAttributes.put(attrName, values);
        return values;
    }

    protected boolean isProceedableAsMap(String attrName) {
        if (this.constraintEnabled && !this.supportedAttributeNames.containsKey(attrName)) {
            if (this.ignoreUnsupportedAttribute) {
                log.debug("map attribute request ignored: " + attrName);
                return false;
            } else {
                throw new IllegalStateException("not supported attribute: " + attrName);
            }
        }
        if (!isMapAttribute(attrName)) {
            throw new IllegalStateException("not map attribute: " + attrName);
        }
        return true;
    }

    @Override
    public String getValue(String attrName) {
        if (this.attributes.containsKey(attrName)) {
            return this.attributes.get(attrName);
        }
        return DtoUtil.getStringOrNull(this.target, attrName);
    }

    @Override
    public String getUpdatedValue(String attrName) {
        if (this.attributes.containsKey(attrName)) {
            return this.attributes.get(attrName);
        }
        return null;
    }

    @Override
    public Map<String, DiffType> getValues(String attrName) {
        Map<String, DiffType> result = new HashMap<String, DiffType>();
        if (this.listAttributes.containsKey(attrName)) {
            Map<String, DiffType> updated = this.listAttributes.get(attrName);
            if (updated != null) {
                return updated;
            }
            return result;
        }
        List<String> current = DtoUtil.getStringList(this.target, attrName);
        for (String item : current) {
            result.put(item, DiffType.KEEP);
        }
        return result;
    }

    @Override
    public Map<String, DiffType> getUpdatedValues(String attrName) {
        if (this.listAttributes.containsKey(attrName)) {
            Map<String, DiffType> updated = this.listAttributes.get(attrName);
            if (updated != null) {
                return updated;
            }
        }
        return new HashMap<String, DiffType>();
    }

    public void recordChanges(String attrName, List<String> addedValues, List<String> removedValues) {
        recordChanges(attrName, addedValues, removedValues, true);
    }

    public void recordChanges(String attrName, List<String> addedValues, List<String> removedValues, boolean isPublic) {
        recordChangesInner(null, attrName, addedValues, removedValues, isPublic);
    }

    public void recordChangesDiff(String attrName, Collection<String> originalNames, Collection<String> newNames, boolean isPublic) {
        recordChangesDiff(null, attrName, originalNames, newNames, isPublic);
    }

    public void recordChangesDiff(NaefDto target, String attrName, Collection<String> originalNames, Collection<String> newNames, boolean isPublic) {
        List<DiffEntry<String>> diffs = DiffUtil.getStringDiff(originalNames, newNames);
        List<String> added = new ArrayList<String>();
        List<String> removed = new ArrayList<String>();
        for (DiffEntry<String> diff : diffs) {
            if (diff.isCreated()) {
                added.add(diff.getTarget());
            } else if (diff.isDeleted()) {
                removed.add(diff.getBase());
            }
        }
        recordChangesInner(target, attrName, added, removed, isPublic);
    }

    protected void recordChangesInner(NaefDto target, String attrName, List<String> added, List<String> removed, boolean isPublic) {
        if (added.size() == 0 && removed.size() == 0) {
            return;
        }
        if (target == null) {
            target = this.target;
        }
        CollectionChangeUnit unit = null;
        for (ChangeUnit u : this.changeUnits) {
            if (u.getKey().equals(attrName)) {
                if (!CollectionChangeUnit.class.isInstance(u)) {
                    throw new IllegalStateException("illegal change unit type: " + attrName);
                }
                unit = CollectionChangeUnit.class.cast(u);
                break;
            }
        }
        if (unit == null) {
            List<String> preChangedValues = DtoUtil.getStringList(target, attrName);
            unit = new CollectionChangeUnit(target, attrName, preChangedValues);
            this.changeUnits.add(unit);
        }
        unit.addValues(added);
        unit.removeValues(removed);
    }

    public void recordChange(String attrName, Map<String, String> originalNames,
                             Map<String, String> newNames, boolean isPublic) {
        List<DiffEntry<String>> diffs = DiffUtil.getStringDiff(originalNames, newNames);
        StringBuilder sbNew = new StringBuilder();
        StringBuilder sbOld = new StringBuilder();
        for (DiffEntry<String> diff : diffs) {
            if (diff.isCreated()) {
                if (sbNew.length() > 0) {
                    sbNew.append(", ");
                }
                sbNew.append("+").append(diff.getTarget());
            } else if (diff.isDeleted()) {
                if (sbOld.length() > 0) {
                    sbOld.append(", ");
                }
                sbOld.append("-").append(diff.getBase());
            } else if (diff.isUpdated()) {
                if (sbNew.length() > 0) {
                    sbNew.append(", ");
                }
                sbNew.append("*").append(diff.getTarget());
                if (sbOld.length() > 0) {
                    sbOld.append(", ");
                }
                sbOld.append("*").append(diff.getBase());
            }
        }
        recordChange(attrName, sbOld.toString(), sbNew.toString(), isPublic);
    }

    public void recordChange(NaefDto target, String attrName, Collection<String> originalNames, Collection<String> newNames, boolean isPublic) {
        List<DiffEntry<String>> diffs = DiffUtil.getStringDiff(originalNames, newNames);
        List<String> added = new ArrayList<String>();
        List<String> removed = new ArrayList<String>();
        for (DiffEntry<String> diff : diffs) {
            if (diff.isCreated()) {
                added.add(diff.getTarget());
            } else if (diff.isDeleted()) {
                removed.add(diff.getBase());
            }
        }
        Collections.sort(added);
        Collections.sort(removed);
        recordChange(target, attrName, added.toString(), removed.toString(), isPublic);
    }

    public void recordChange(String attrName, Collection<String> originalNames, Collection<String> newNames, boolean isPublic) {
        recordChange(null, attrName, originalNames, newNames, isPublic);
    }

    public void recordChange(NaefDto target, String attrName, String oldValue, String newValue) {
        recordChange(target, attrName, oldValue, newValue, true);
    }

    public void recordChange(String attrName, String oldValue, String newValue) {
        recordChange(this.target, attrName, oldValue, newValue, true);
    }

    public void recordChange(String attrName, String oldValue, String newValue, boolean isPublic) {
        recordChange(this.target, attrName, oldValue, newValue, isPublic);
    }

    public void recordChange(String attrName, Object oldValue, Object newValue) {
        String old_ = getString(oldValue);
        String new_ = getString(newValue);
        recordChange(this.target, attrName, old_, new_, true);
    }

    private String getString(Object o) {
        if (o == null) {
            return null;
        } else if (NaefDto.class.isInstance(o)) {
            return ((NaefDto) o).getAbsoluteName();
        } else {
            return o.toString();
        }
    }

    public void recordChange(String attrName, List<? extends NaefDiffUnit> oldValues,
                             List<? extends NaefDiffUnit> newValues) {
        for (NaefDiffUnit oldValue : oldValues) {
            if (newValues.contains(oldValue)) {
                continue;
            }
            recordChange(this.target, attrName, oldValue.getKey(), null);
        }
        for (NaefDiffUnit newValue : newValues) {
            if (oldValues.contains(newValue)) {
                continue;
            }
            recordChange(this.target, attrName, null, newValue.getKey());
        }
    }

    public void recordChange(NaefDto target, String attrName, String oldValue, String newValue, boolean isPublic) {
        if (target == null) {
            target = this.target;
        }
        SimpleChangeUnit unit = new SimpleChangeUnit(target, attrName);
        unit.setPrevious(oldValue);
        unit.setCurrent(newValue);
        if (!this.changeUnits.contains(unit)) {
            this.changeUnits.add(unit);
        }
    }

    @Override
    public List<ChangeUnit> getChangeUnits() {
        return new ArrayList<ChangeUnit>(this.changeUnits);
    }

    public Map<String, String> getAttributes() {
        return new HashMap<String, String>(this.attributes);
    }

    public String getAttribute(String attrName) {
        return this.attributes.get(attrName);
    }

    public boolean hasAttributeValue(String attrName) {
        return this.attributes.get(attrName) != null;
    }

    public boolean hasAttributesValue(String... attrNames) {
        for (String attrName : attrNames) {
            if (hasAttributeValue(attrName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAttributeChanged(String attrName) {
        return this.attributes.containsKey(attrName);
    }


    public boolean areAttributesChanged(String... attrNames) {
        for (String attrName : attrNames) {
            if (isAttributeChanged(attrName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isChanged(String attrName) {
        for (ChangeUnit unit : this.changeUnits) {
            if (unit.getKey().equals(attrName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isChanged(NaefDto dto, String attrName) {
        for (ChangeUnit unit : this.changeUnits) {
            if (unit.isFor(dto) && unit.getKey().equals(attrName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getDiffContent() {
        StringBuilder sb = new StringBuilder();
        for (ChangeUnit unit : this.changeUnits) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            if (isSimple(unit)) {
                sb.append(unit.simpleToString());
            } else {
                sb.append(unit.toString());
            }
        }
        return sb.toString();
    }

    private boolean isSimple(ChangeUnit unit) {
        if (unit.getTarget() != null) {
            return this.requiredClass.equals(unit.getTarget().getClass());
        }
        return true;
    }

    protected String getNameForDelete(String name) {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");
        String suffix = df.format(new Date());
        return name + ATTR.DELETED + suffix;
    }

    protected void setBuildResult(BuildResult result) {
        this.result = result;
    }

    public BuildResult getBuildResult() {
        return this.result;
    }

    protected void checkBuilt() {
        if (isBuilt()) {
            throw new IllegalStateException("already built.");
        }
    }

    @Override
    public void addChildBuilder(CommandBuilder builder) {
        this.dependers.add(builder);
    }

    @Override
    public List<CommandBuilder> getChildBuilders() {
        return this.dependers;
    }

    @Override
    public CommandBuilder getDependentBuilder() {
        return this.dependent;
    }

    @Override
    public void setDependentBuilder(CommandBuilder builder) {
        this.dependent = builder;
    }

    protected ComplementBuilderEntry getOrCreateComplementBuilderEntry(Class<? extends ComplementBuilder> _class) {
        if (_class == null) {
            throw new IllegalArgumentException();
        }
        for (ComplementBuilderEntry entry : this.complementBuilderEntries) {
            Class<?> cls = entry.getBuilderClass();
            if (cls.equals(_class)) {
                return entry;
            }
        }
        ComplementBuilderEntry newEntry = new ComplementBuilderEntry(_class);
        this.complementBuilderEntries.add(newEntry);
        return newEntry;
    }

    @Override
    public void addComplementBuilder(Class<? extends ComplementBuilder> builderClass, String targetName) {
        if (builderClass == null) {
            throw new IllegalArgumentException();
        } else if (targetName == null) {
            return;
        }
        ComplementBuilderEntry entry = getOrCreateComplementBuilderEntry(builderClass);
        entry.addTargetName(targetName);
    }

    @Override
    public void addComplementBuilder(Class<? extends ComplementBuilder> builderClass, NaefDto target) {
        if (builderClass == null) {
            throw new IllegalArgumentException();
        } else if (target == null) {
            return;
        }
        ComplementBuilderEntry entry = getOrCreateComplementBuilderEntry(builderClass);
        entry.addTargetDto(target);
    }

    @Override
    public List<ComplementBuilder> getComplementBuilders() {
        List<ComplementBuilder> builders = new ArrayList<ComplementBuilder>();
        for (ComplementBuilderEntry entry : this.complementBuilderEntries) {
            ComplementBuilder builder = entry.getBuilder();
            builders.add(builder);
        }
        return builders;
    }

    public String getSimpleAbsoluteName(NodeElementDto dto) {
        List<String> arr = new ArrayList<String>();
        NodeElementDto current = dto;
        while (current != null) {
            arr.add(current.getName());
            current = current.getOwner();
        }
        Collections.reverse(arr);
        StringBuilder sb = new StringBuilder();
        for (String element : arr) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(element);
        }
        return sb.toString();
    }

    @Override
    public void setExecuted(boolean value) {
        this.executed = value;
    }

    public void setSource(String source) {
        setValueInner(ATTR.SOURCE, source);
    }

    public String getEditor() {
        return this.editorName;
    }

    protected Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }

    private class ComplementBuilderEntry {
        private final Class<? extends ComplementBuilder> _class;
        private final List<String> names = new ArrayList<String>();
        private final MvoDtoList<NaefDto> dtos = new MvoDtoList<NaefDto>();

        public ComplementBuilderEntry(Class<? extends ComplementBuilder> _class) {
            if (_class == null) {
                throw new IllegalArgumentException();
            }
            this._class = _class;
        }

        public Class<? extends ComplementBuilder> getBuilderClass() {
            return this._class;
        }

        public void addTargetName(String name) {
            if (name == null) {
                return;
            } else if (this.names.contains(name)) {
                return;
            }
            this.names.add(name);
        }

        public void addTargetDto(NaefDto dto) {
            if (dto == null) {
                return;
            } else if (this.dtos.contains(dto)) {
                return;
            }
            this.dtos.add(dto);
        }

        public List<String> getTargetNames() {
            return this.names;
        }

        public List<NaefDto> getTargetDtos() {
            return this.dtos;
        }

        public ComplementBuilder getBuilder() {
            try {
                Constructor<? extends ComplementBuilder> constructor = this._class.getConstructor(String.class);
                ComplementBuilder builder = constructor.newInstance(AbstractCommandBuilder.this.editorName);
                builder.addTargetNames(getTargetNames());
                builder.addTargetDtos(getTargetDtos());
                return builder;
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }
    }
}