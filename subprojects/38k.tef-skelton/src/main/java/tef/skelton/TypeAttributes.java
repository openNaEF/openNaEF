package tef.skelton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import tef.TefService;

class TypeAttributes {

    static class Instances {

        private final Map<Class<?>, TypeAttributes> instances_ = new HashMap<Class<?>, TypeAttributes>();

        Instances() {
        }

        synchronized void add(TypeAttributes instance) {
            instances_.put(instance.type(), instance);
        }

        synchronized TypeAttributes getInstance(Class<?> type) {
            return instances_.get(type);
        }

        synchronized TypeAttributes gainInstance(Class<?> type) {
            if (type == null) {
                throw new IllegalArgumentException();
            }

            TypeAttributes result = getInstance(type);
            return result != null ? result : new TypeAttributes(this, type);
        }

        synchronized Set<Class<?>> types() {
            return instances_.keySet();
        }

        synchronized Set<TypeAttributes> getSupertypeInstances(Class<?> subtype) {
            Set<TypeAttributes> result = new HashSet<TypeAttributes>();
            for (Class<?> type : types()) {
                if (type != subtype && type.isAssignableFrom(subtype)) {
                    result.add(getInstance(type));
                }
            }
            return result;
        }

        synchronized Set<TypeAttributes> getSubtypeInstances(Class<?> supertype) {
            Set<TypeAttributes> result = new HashSet<TypeAttributes>();
            for (Class<?> type : types()) {
                if (type != supertype && supertype.isAssignableFrom(type)) {
                    result.add(getInstance(type));
                }
            }
            return result;
        }
    }

    private static class Entry {

        final Class<?> declaringType;
        final Attribute<?, ?> attribute;

        Entry(Class<?> declaringType, Attribute<?, ?> attribute) {
            this.declaringType = declaringType;
            this.attribute = attribute;
        }

        String name() {
            return attribute.getName();
        }
    }

    private final Instances instances_;
    private final Class<?> type_;
    private final SortedMap<String, Entry> attributes_ = new TreeMap<String, Entry>();

    TypeAttributes(Instances instances, Class<?> type) {
        type_ = type;
        instances_ = instances;

        instances_.add(this);

        for (TypeAttributes supertypeAttrs : instances_.getSupertypeInstances(type_)) {
            for (Entry supertypeEntry : supertypeAttrs.entries()) {
                install(supertypeEntry);
            }
        }
    }

    synchronized void install(Attribute<?, ?> attribute) {
        Entry entry = new Entry(type_, attribute);
        install(entry);

        for (TypeAttributes subtypeAttrs : instances_.getSubtypeInstances(type_)) {
            subtypeAttrs.install(entry);
        }
    }

    private synchronized void install(Entry entry) {
        Entry existing = entry(entry.name());
        if (entry == existing) {
            return;
        }

        if (existing != null) {
            if (isOverridable(entry, existing)) {
                return;
            }

            if (! isOverridable(existing, entry)) {
                throw new IllegalStateException(
                    "duplicated attribute: " + type_.getName()
                    + ", " + entry.name()
                    + ", " + existing.declaringType.getName()
                    + ", " + entry.declaringType.getName());
            }
        }

        attributes_.put(entry.name(), entry);

        UiTypeName.Instances uitypenames = SkeltonTefService.instance().uiTypeNames();
        TefService.instance().logMessage(
            "[attr-def]" + uitypenames.getName(type_) + "\t" + entry.name()
            + "\tdef:" + uitypenames.getName(entry.declaringType)
            + (existing == null
                ? ""
                : "\toverride:" + uitypenames.getName(existing.declaringType)));
    }

    private boolean isOverridable(Entry overridee, Entry overrider) {
        if (overridee == null) {
            return true;
        }

        return overridee.declaringType.isAssignableFrom(overrider.declaringType)
            && (overridee.attribute instanceof Attribute.SingleAttr<?, ?>)
            && (overrider.attribute instanceof Attribute.SingleAttr<?, ?>)
            && overridee.attribute.getType().getJavaType()
                .isAssignableFrom(overrider.attribute.getType().getJavaType());
    }

    Class<?> type() {
        return type_;
    }

    synchronized List<String> names() {
        return new ArrayList<String>(attributes_.keySet());
    }

    synchronized List<Attribute<?, ?>> attributes() {
        List<Attribute<?, ?>> result = new ArrayList<Attribute<?, ?>>();
        for (String name : names()) {
            result.add(entry(name).attribute);
        }
        return result;
    }

    synchronized List<Entry> entries() {
        List<Entry> result = new ArrayList<Entry>();
        for (String name : names()) {
            result.add(entry(name));
        }
        return result;
    }

    synchronized Attribute<?, ?> get(String name) {
        Entry entry = entry(name);
        return entry == null ? null : entry.attribute;
    }

    synchronized Entry entry(String name) {
        return attributes_.get(name);
    }

    synchronized boolean hasInstalled(Attribute<?, ?> attribute) {
        return attributes().contains(attribute);
    }
}
