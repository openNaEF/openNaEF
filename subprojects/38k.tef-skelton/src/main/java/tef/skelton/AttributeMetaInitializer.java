package tef.skelton;

import lib38k.xml.Xml;
import tef.TefService;
import tef.skelton.dto.DtoAttrTranscript;
import tef.skelton.dto.EntityDto;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

class AttributeMetaInitializer {

    private enum AttrType {

        STRING(
            AttributeType.STRING,
            AttributeType.MvoSetType.STRING,
            AttributeType.MvoListType.STRING,
            ValueResolver.STRING),
        BOOLEAN(
            AttributeType.BOOLEAN,
            AttributeType.MvoSetType.BOOLEAN,
            AttributeType.MvoListType.BOOLEAN,
            ValueResolver.BOOLEAN),
        INTEGER(
            AttributeType.INTEGER,
            AttributeType.MvoSetType.INTEGER,
            AttributeType.MvoListType.INTEGER,
            ValueResolver.INTEGER),
        LONG(
            AttributeType.LONG,
            AttributeType.MvoSetType.LONG,
            AttributeType.MvoListType.LONG,
            ValueResolver.LONG),
        FLOAT(
            AttributeType.FLOAT,
            AttributeType.MvoSetType.FLOAT,
            AttributeType.MvoListType.FLOAT,
            ValueResolver.FLOAT),
        DOUBLE(
            AttributeType.DOUBLE,
            AttributeType.MvoSetType.DOUBLE,
            AttributeType.MvoListType.DOUBLE,
            ValueResolver.DOUBLE),
        DATETIME(
            AttributeType.DATETIME,
            AttributeType.MvoSetType.DATETIME,
            AttributeType.MvoListType.DATETIME,
            ValueResolver.DATETIME);

        final AttributeType<?> type;
        final AttributeType.MvoSetType<?> typeOfMvoSet;
        final AttributeType.MvoListType<?> typeOfMvoList;
        final ValueResolver<?> valueResolver;

        AttrType(
            AttributeType<?> type,
            AttributeType.MvoSetType<?> typeOfMvoSet,
            AttributeType.MvoListType<?> typeOfMvoList,
            ValueResolver<?> valueResolver)
        {
            this.type = type;
            this.typeOfMvoSet = typeOfMvoSet;
            this.typeOfMvoList = typeOfMvoList;
            this.valueResolver = valueResolver;
        }
    }

    private static class AttrTypeHandlerArgs {

        final SkeltonTefService tefService;
        final String className;
        final String attrName;
        final String attrTypename;

        AttrTypeHandlerArgs(
            SkeltonTefService tefService,
            String className,
            String attrName,
            String attrTypename)
        {
            this.tefService = tefService;
            this.className = className;
            this.attrName = attrName;
            this.attrTypename = attrTypename;
        }

        Class<? extends Model> modelClass() {
            return resolveClass(className);
        }

        Class<? extends EntityDto> dtoClass() {
            return tefService.getMvoDtoMapping().getDtoClass(modelClass());
        }
    }

    private enum AttrTypeHandler {

        SET("set(", ")") {

            @Override Attribute<?, Model> resolveAttribute(AttrTypeHandlerArgs args) {
                return new Attribute.SetAttr<Object, Model>(
                    args.attrName,
                    (AttributeType.MvoSetType<Object>) resolveTypeAsSet(args));
            }

            private AttributeType.MvoSetType<?> resolveTypeAsSet(final AttrTypeHandlerArgs args) {
                String elementTypename = extractContent(args.attrTypename);
                AttrType primitiveType = resolveAsPrimitive(elementTypename);
                if (primitiveType != null) {
                    return primitiveType.typeOfMvoSet;
                }

                UiTypeName objectType = resolveAsTypeName(elementTypename);
                if (objectType != null) {
                    args.tefService.getMvoDtoFactory().addDtoAttrTranscript(
                        new DtoAttrTranscript.SetRef<EntityDto, Model>(
                            args.dtoClass(),
                            new EntityDto.SetRefAttr<EntityDto, EntityDto>(args.attrName),
                            DtoAttrTranscript.EvalStrategy.LAZY)
                        {
                            @Override protected Set<Model> getValues(Model o) {
                                MvoSet<Model> values = (MvoSet<Model>) o.getValue(args.attrName);
                                return values == null
                                    ? Collections.<Model>emptySet()
                                    : values.get();
                            }
                        });

                    final Class<?> elementClass = objectType.type();
                    return new AttributeType.MvoSetType<Object>(elementClass) {

                        @Override public Object parseElement(String str) {
                            return ValueResolver.<Object>resolve(elementClass, null, str);
                        }
                    };
                }

                throw new ConfigurationException(
                    CONFIG_FILENAME + " の " + args.className + "."+ args.attrName
                    + " のset-element型名 " + elementTypename + " が解決できません.");
            }
        },

        LIST("list(", ")") {

            @Override Attribute<?, Model> resolveAttribute(AttrTypeHandlerArgs args) {
                return new Attribute.ListAttr<Object, Model>(
                    args.attrName,
                    (AttributeType.MvoListType<Object>) resolveTypeAsList(args));
            }

            private AttributeType.MvoListType<?> resolveTypeAsList(final AttrTypeHandlerArgs args) {
                String elementTypename = extractContent(args.attrTypename);
                AttrType primitiveType = resolveAsPrimitive(elementTypename);
                if (primitiveType != null) {
                    return primitiveType.typeOfMvoList;
                }

                UiTypeName objectType = resolveAsTypeName(elementTypename);
                if (objectType != null) {
                    args.tefService.getMvoDtoFactory().addDtoAttrTranscript(
                        new DtoAttrTranscript.ListRef<EntityDto, Model>(
                            args.dtoClass(),
                            new EntityDto.ListRefAttr<EntityDto, EntityDto>(args.attrName),
                            DtoAttrTranscript.EvalStrategy.LAZY)
                        {
                            @Override protected List<Model> getValues(Model o) {
                                MvoList<Model> values = (MvoList<Model>) o.getValue(args.attrName);
                                return values == null
                                    ? Collections.<Model>emptyList()
                                    : values.get();
                            }
                        });

                    final Class<?> elementClass = objectType.type();
                    return new AttributeType.MvoListType<Object>(elementClass) {

                        @Override public Object parseElement(String str) {
                            return ValueResolver.<Object>resolve(elementClass, null, str);
                        }
                    };
                }

                throw new ConfigurationException(
                    CONFIG_FILENAME + " の " + args.className + "."+ args.attrName
                    + " のlist-element型名 " + elementTypename + " が解決できません.");
            }
        },

        MAP("map(", ")") {

            @Override Attribute<?, Model> resolveAttribute(AttrTypeHandlerArgs args) {
                return new Attribute.MapAttr<Object, Object, Model>(
                    args.attrName,
                    (AttributeType.MvoMapType<Object, Object>) resolveTypeAsMap(args));
            }

            private AttributeType.MvoMapType<?, ?> resolveTypeAsMap(final AttrTypeHandlerArgs args) {
                String keyvalueStr = extractContent(args.attrTypename);
                String[] tokens = keyvalueStr.split(",");
                if (tokens.length != 2) {
                    throw new ConfigurationException(
                        CONFIG_FILENAME + " の " + args.className + "."+ args.attrName
                        + " のmap要素型指定形式が不正です.");
                }

                String keyTypename = tokens[ 0 ];
                String valueTypename = tokens[ 1 ];

                ValueResolver<?> keyResolver = getValueResolver("map-key", args, keyTypename);
                ValueResolver<?> valueResolver = getValueResolver("map-value", args, valueTypename);

                if (keyResolver instanceof ValueResolver.Model<?>
                    && valueResolver instanceof ValueResolver.Model<?>)
                {
                    args.tefService.getMvoDtoFactory().addDtoAttrTranscript(
                        new DtoAttrTranscript.MapKeyValueRef<EntityDto, EntityDto, Model>(
                            args.dtoClass(),
                            new EntityDto.MapKeyValueRefAttr<EntityDto, EntityDto, EntityDto>(args.attrName),
                            DtoAttrTranscript.EvalStrategy.LAZY)
                        {
                            @Override protected Map<Model, Model> getValues(Model o) {
                                return ((Attribute.MapAttr<Model, Model, Model>) Attribute.getAttribute(
                                        args.modelClass(),
                                        args.attrName))
                                    .snapshot(o);
                            }
                        });
                } else if (keyResolver instanceof ValueResolver.Model<?>) {
                    args.tefService.getMvoDtoFactory().addDtoAttrTranscript(
                        new DtoAttrTranscript.MapKeyRef<EntityDto, Object, Model>(
                            args.dtoClass(),
                            new EntityDto.MapKeyRefAttr<EntityDto, Object, EntityDto>(args.attrName),
                            DtoAttrTranscript.EvalStrategy.LAZY)
                        {
                            @Override protected Map<Model, Object> getValues(Model o) {
                                return ((Attribute.MapAttr<Model, Object, Model>) Attribute.getAttribute(
                                        args.modelClass(),
                                        args.attrName))
                                    .snapshot(o);
                            }
                        });
                } else if (valueResolver instanceof ValueResolver.Model<?>) {
                    args.tefService.getMvoDtoFactory().addDtoAttrTranscript(
                        new DtoAttrTranscript.MapValueRef<Object, EntityDto, Model>(
                            args.dtoClass(),
                            new EntityDto.MapValueRefAttr<Object, EntityDto, EntityDto>(args.attrName),
                            DtoAttrTranscript.EvalStrategy.LAZY)
                        {
                            @Override protected Map<Object, Model> getValues(Model o) {
                                return ((Attribute.MapAttr<Object, Model, Model>) Attribute.getAttribute(
                                        args.modelClass(),
                                        args.attrName))
                                    .snapshot(o);
                            }
                        });
                }

                return new AttributeType.MvoMapType<Object, Object>(
                    (ValueResolver<Object>) keyResolver,
                    (ValueResolver<Object>) valueResolver);
            }

            private ValueResolver<?> getValueResolver(String caption, AttrTypeHandlerArgs args, String name) {
                AttrType primitiveType = resolveAsPrimitive(name);
                if (primitiveType != null) {
                    return primitiveType.valueResolver;
                }

                UiTypeName objectType = resolveAsTypeName(name);
                if (objectType != null) {
                    return new ValueResolver.Model<Object>(objectType.type());
                }

                throw new ConfigurationException(
                    CONFIG_FILENAME + " の " + args.className + "."+ args.attrName
                    + " の" + caption + "型名 " + name + " が解決できません.");
            }
        },

        SIMPLE_TYPE ("", "") { 

            @Override Attribute<?, Model> resolveAttribute(AttrTypeHandlerArgs args) {
                return new Attribute.SingleAttr<Object, Model>(
                    args.attrName,
                    (AttributeType<Object>) resolveTypeAsSimpleType(args));
            }

            private AttributeType<?> resolveTypeAsSimpleType(AttrTypeHandlerArgs args) {
                AttrType attrType = resolveAsPrimitive(args.attrTypename);
                if (attrType != null) {
                    return attrType.type;
                }

                UiTypeName typename = resolveAsTypeName(args.attrTypename);
                if (typename != null) {
                    return new AttributeType.ModelType(typename.type());
                }

                throw new ConfigurationException(
                    CONFIG_FILENAME + " の " + args.className + "."+ args.attrName
                    + " の型名 " + args.attrTypename + " が解決できません.");
            }
        };

        static AttrTypeHandler getInstance(String attrTypename) {
            for (AttrTypeHandler instance : values()) {
                if (instance.matches(attrTypename)) {
                    return instance;
                }
            }
            throw new RuntimeException();
        }

        private final String prefix_;
        private final String suffix_;

        AttrTypeHandler(String prefix, String suffix) {
            prefix_ = prefix;
            suffix_ = suffix;
        }

        boolean matches(String str) {
            return str.startsWith(prefix_) && str.endsWith(suffix_);
        }

        String extractContent(String str) {
            if (! matches(str)) {
                throw new IllegalArgumentException();
            }

            return str.substring(prefix_.length(), str.length() - suffix_.length());
        }

        abstract Attribute<?, Model> resolveAttribute(AttrTypeHandlerArgs args);
    }

    private static final class AttrMeta {

        final String name;
        final AttributeType<?> type;

        AttrMeta(String name, AttributeType<?> type) {
            this.name = name;
            this.type = type;
        }
    }

    static Xml.Elem getConfigurationXmlRoot() {
        File attributeMetaDefinitionFile = new File(TefService.instance().getConfigsDirectory(), CONFIG_FILENAME);
        if (! attributeMetaDefinitionFile.isFile()) {
            return null;
        }

        Xml.Elem root = new Xml(attributeMetaDefinitionFile).getRoot();
        if (! root.getName().equals("attribute-meta")) {
            throw new ConfigurationException("attribute meta の root 名が不正です.");
        }

        return root;
    }

    private static final String CONFIG_FILENAME = "AttributeMetaDefinition.xml";

    private final SkeltonTefService tefService_;

    AttributeMetaInitializer(SkeltonTefService tefService) {
        tefService_ = tefService;
    }

    void initialize() {
        Xml.Elem root = getConfigurationXmlRoot();
        if (root == null) {
            return;
        }

        for (Xml.Elem classEntry : root.getSubElems("class")) {
            String className = getXmlAttribute(classEntry, "name");
            Class<? extends Model> klass = resolveClass(className);
            for (Attribute<?, Model> attr : getAttributes(root, classEntry)) {
                tefService_.installAttributes(klass, attr);
            }
        }
    }

    private static Class<? extends Model> resolveClass(String className) {
        UiTypeName typename = resolveAsTypeName(className);
        if (typename != null && Model.class.isAssignableFrom(typename.type())) {
            return (Class<? extends Model>) typename.type();
        } else {
            return SkeltonUtils.classForName("attribute-meta", Model.class, className);
        }
    }

    private static String getXmlAttribute(Xml.Elem e, String attrName) {
        return SkeltonUtils.getXmlAttribute("attribute-meta", e, attrName);
    }

    private List<Attribute<?, Model>> getAttributes(Xml.Elem root, Xml.Elem classEntry) {
        String className = getXmlAttribute(classEntry, "name");

        List<Attribute<?, Model>> result = new ArrayList<Attribute<?, Model>>();

        for (Xml.Elem includeClassEntry : classEntry.getSubElems("include")) {
            String includeClassName = getXmlAttribute(includeClassEntry, "class");
            for (Xml.Elem includee : getClassEntries(root, includeClassName)) {
                result.addAll(getAttributes(root, includee));
            }
        }

        for (Xml.Elem attrEntry : classEntry.getSubElems("attribute")) {
            String attrName = getXmlAttribute(attrEntry, "name");
            String attrTypename = getXmlAttribute(attrEntry, "type");

            result.add(
                AttrTypeHandler
                    .getInstance(attrTypename)
                    .resolveAttribute(new AttrTypeHandlerArgs(tefService_, className, attrName, attrTypename)));
        }

        return result;
    }

    private static List<Xml.Elem> getClassEntries(Xml.Elem root, String className) {
        List<Xml.Elem> classEntries = new ArrayList<Xml.Elem>();
        for (Xml.Elem e : root.getSubElems("class")) {
            if (getXmlAttribute(e, "name").equals(className)) {
                classEntries.add(e);
            }
        }
        if (classEntries.size() == 0) {
            throw new ConfigurationException("attribute-meta, " + className + " の定義がありません.");
        }
        return classEntries;
    }

    private static AttrType resolveAsPrimitive(String name) {
        try {
            return ValueResolver.resolveEnum(AttrType.class, name, false);
        } catch (FormatException fe) {
            return null;
        }
    }

    static UiTypeName resolveAsTypeName(String name) {
        UiTypeName typename = SkeltonTefService.instance().uiTypeNames().getByName(name);
        return typename == null || typename.type() == null
            ? null
            : typename;
    }
}
