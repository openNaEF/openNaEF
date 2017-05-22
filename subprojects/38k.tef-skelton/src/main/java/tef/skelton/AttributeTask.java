package tef.skelton;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AttributeTask extends Task {

    public static final Attribute.SingleAttr<AttributeTask, Model> ATTRIBUTE
        = new Attribute.SingleAttr<AttributeTask, Model>("tef.app-skelton.attribute-configuration-task", null);

    private final F0<Model> configuratee_ = new F0<Model>();
    private final M1<String, Object> configurations_ = new M1<String, Object>();
    private final S1<String> nullConfigurations_ = new S1<String>();

    public AttributeTask(MvoId id) {
        super(id);
    }

    public AttributeTask(String name, Model configuratee) throws TaskException {
        super(name);

        if (configuratee.get(AttributeTask.ATTRIBUTE) != null) {
            throw new TaskException("タスクは設定済です.");
        }

        configuratee_.initialize(configuratee);
        getConfiguratee().set(AttributeTask.ATTRIBUTE, this);
    }

    @Override public void cancel() throws TaskException {
        if (getConfiguratee().get(AttributeTask.ATTRIBUTE) != this) {
            throw new TaskException("キャンセル済です.");
        }

        getConfiguratee().set(AttributeTask.ATTRIBUTE, null);
    }

    @Override public void activate() throws TaskException {
        if (getConfiguratee().get(AttributeTask.ATTRIBUTE) != this) {
            throw new TaskException("キャンセル済です.");
        }

        Model configuratee = getConfiguratee();
        for (String attrName : getAttributeNames()) {
            Object value = getValue(attrName);
            if (value != null) {
                configuratee.putValue(attrName, value);
            }
        }
    }

    public Model getConfiguratee() {
        return configuratee_.get();
    }

    public void putConfiguration(String attributeName, Object value) throws TaskException {
        if (Attribute.getAttribute(getConfiguratee().getClass(), attributeName) == null) {
            throw new TaskException("定義されていない属性名です.");
        }
        if (configurations_.get(attributeName) != null
            || nullConfigurations_.contains(attributeName))
        {
            throw new TaskException("値が設定済です.");
        }

        if (value == null) {
            nullConfigurations_.add(attributeName);
        } else {
            configurations_.put(attributeName, value);
        }
    }

    public void resetConfiguration(String attributeName) {
        if (configurations_.get(attributeName) != null) {
            configurations_.put(attributeName, null);
        }
        if (nullConfigurations_.contains(attributeName)) {
            nullConfigurations_.remove(attributeName);
        }
    }

    public Set<String> getAttributeNames() {
        Set<String> result = new HashSet<String>();
        result.addAll(configurations_.getKeys());
        result.addAll(nullConfigurations_.get());
        return result;
    }

    public Object getValue(String attributeName) {
        return nullConfigurations_.contains(attributeName)
            ? null
            : configurations_.get(attributeName);
    }
}
