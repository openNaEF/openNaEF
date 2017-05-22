package voss.core.server.util;

import java.io.Serializable;

public class AttributeMeta implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String attrName;
    private final Class<?> mvoType;
    private final Class<?> elementType;
    private final Class<?> keyType;
    private final Class<?> valueType;

    public AttributeMeta(String attrName, Class<?> type) {
        if (Util.isNull(attrName, type)) {
            throw new IllegalArgumentException();
        }
        this.attrName = attrName;
        this.mvoType = type;
        this.elementType = null;
        this.keyType = null;
        this.valueType = null;
    }

    public AttributeMeta(String attrName, Class<?> type, Class<?> elementType) {
        if (Util.isNull(attrName, type, elementType)) {
            throw new IllegalArgumentException();
        }
        this.attrName = attrName;
        this.mvoType = type;
        this.elementType = elementType;
        this.keyType = null;
        this.valueType = null;
    }

    public AttributeMeta(String attrName, Class<?> type, Class<?> keyType, Class<?> valueType) {
        if (Util.isNull(attrName, type, keyType, valueType)) {
            throw new IllegalArgumentException();
        }
        this.attrName = attrName;
        this.mvoType = type;
        this.elementType = null;
        this.keyType = keyType;
        this.valueType = valueType;
    }

    public String getAttrName() {
        return this.attrName;
    }

    public Class<?> getType() {
        return this.mvoType;
    }

    public Class<?> getElementType() {
        return this.elementType;
    }

    public Class<?> getKeyType() {
        return this.keyType;
    }

    public Class<?> getValueType() {
        return this.valueType;
    }

    public boolean isSingleValue() {
        return this.elementType == null && this.keyType == null;
    }

    public boolean isCollection() {
        return this.elementType != null;
    }

    public boolean isMap() {
        return this.keyType != null;
    }

    @Override
    public String toString() {
        return this.attrName;
    }

    @Override
    public int hashCode() {
        return this.attrName.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (this == o) {
            return true;
        } else if (!AttributeMeta.class.isInstance(o)) {
            return false;
        }
        AttributeMeta other = AttributeMeta.class.cast(o);
        return this.attrName.equals(other.attrName);
    }
}