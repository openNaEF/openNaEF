package tef.skelton;

import lib38k.xml.Xml;
import tef.TefInitializationFailedException;

class AttributeMetaInterlockInitializer {

    void initialize() {
        Xml.Elem root = AttributeMetaInitializer.getConfigurationXmlRoot();
        if (root == null) {
            return;
        }

        for (Xml.Elem interlockEntry : root.getSubElems("interlock.single")) {
            Attribute.SingleAttr<?, ?> attr1 = getSingleAttribute(interlockEntry, "type1", "attribute1");
            Attribute.SingleAttr<?, ?> attr2 = getSingleAttribute(interlockEntry, "type2", "attribute2");
            AttributeInterlock.interlockSingle(attr1, attr2);
        }
    }

    private Attribute.SingleAttr<?, ?> getSingleAttribute(
        Xml.Elem entry, String elemTypeAttrName, String elemAttrAttrName)
    {
        String typeName = SkeltonUtils.getXmlAttribute("attribute-meta.interlock", entry, elemTypeAttrName);
        String attrName = SkeltonUtils.getXmlAttribute("attribute-meta.interlock", entry, elemAttrAttrName);

        UiTypeName type = AttributeMetaInitializer.resolveAsTypeName(typeName);
        if (type == null) {
            throw new TefInitializationFailedException("no such type: attribute-meta.interlock " + typeName);
        }

        if (! Model.class.isAssignableFrom(type.type())) {
            throw new TefInitializationFailedException("type error: " + typeName);
        }

        Attribute<?, ?> attr = Attribute.getAttribute((Class<? extends Model>) type.type(), attrName);
        if (attr == null) {
            throw new TefInitializationFailedException(
                "no such attribute, type:" + typeName + ", attribute-name:" + attrName);
        }
        if (! (attr instanceof Attribute.SingleAttr<?, ?>)) {
            throw new TefInitializationFailedException(
                "attribute is not a single, type:" + typeName + ", attribute-name:" + attrName);
        }
        return (Attribute.SingleAttr<?, ?>) attr;
    }
}
