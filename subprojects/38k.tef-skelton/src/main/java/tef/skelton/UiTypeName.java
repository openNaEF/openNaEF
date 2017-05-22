package tef.skelton;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UiTypeName {

    public static class Instances {

        private final Set<UiTypeName> instances_ = new HashSet<UiTypeName>();
        private final Map<Class<?>, UiTypeName> types_ = new HashMap<Class<?>, UiTypeName>();
        private final Map<String, UiTypeName> names_ = new HashMap<String, UiTypeName>();

        Instances() {
        }

        synchronized void add(UiTypeName instance) {
            if (getByType(instance.type()) != null) {
                throw new IllegalStateException("duplicated definition, " + instance.type().getName());
            }
            if (getByName(instance.name()) != null) {
                throw new IllegalStateException("duplicated definition, " + instance.name());
            }

            instances_.add(instance);
            types_.put(instance.type(), instance);
            names_.put(instance.name(), instance);
        }

        public synchronized Set<UiTypeName> instances() {
            return Collections.unmodifiableSet(instances_);
        }

        public synchronized UiTypeName getByType(Class<?> type) {
            return types_.get(type);
        }

        public synchronized UiTypeName getByName(String name) {
            return names_.get(name);
        }

        public synchronized UiTypeName getAdaptive(Object o) {
            UiTypeName result = null;
            for (UiTypeName e : instances()) {
                if (e.type().isInstance(o)) {
                    if (result == null) {
                        result = e;
                    } else {
                        result = result.type().isAssignableFrom(e.type())
                            ? e
                            : result;
                    }
                }
            }
            return result;
        }

        public synchronized String getName(Class<?> type) {
            if (type == null) {
                return null;
            }

            UiTypeName mapping = getByType(type);
            return mapping == null ? type.getName() : mapping.name();
        }
    }

    private final Class<?> type_;
    private final String name_;

    public UiTypeName(Instances instances, Class<?> type, String name) {
        if (type == null || name == null) {
            throw new IllegalArgumentException();
        }

        type_ = type;
        name_ = name;

        instances.add(this);
    }

    public Class<?> type() {
        return type_;
    }

    public String name() {
        return name_;
    }
}
