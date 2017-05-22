package tef.skelton.dto;

import tef.MVO;
import tef.TransactionContext;
import tef.TransactionId;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.Model;
import tef.skelton.MvoCollection;
import tef.skelton.NamedModel;
import tef.skelton.SkeltonTefService;

import java.io.Serializable;

public class MvoDtoFactory extends DtoFactory {

    private final MvoDtoMapping mapping_;

    public MvoDtoFactory(SkeltonTefService tefService, MvoDtoMapping mapping) {
        super(tefService);

        mapping_ = mapping;
        mapping_.initialize();
    }

    public MvoDtoMapping getMapping() {
        return mapping_;
    }

    public <T extends EntityDto> T build(DtoOriginator originator, MVO mvo) {
        Class<T> dtoClass = mvo == null ? null : (Class<T>) mapping_.getDtoClass(mvo.getClass());
        if (dtoClass == null) {
            return null;
        }

        try {
            T result = dtoClass.newInstance();
            result.setOriginator(originator);
            result.setDescriptor(MvoDtoDesc.<EntityDto>build1((Model) mvo));

            setAttributes(result, (Model) mvo);

            for (Attribute<?, ?> attr : Attribute.getAttributes(dtoClass)) {
                if (isLazyTranscript(dtoClass, attr.getName())) {
                    result.addLazyInitAttrName(attr.getName());
                }
            }

            return result;
        } catch (Exception e) {
            String message = "mvo:" + mvo.getMvoId().getLocalStringExpression() + ",dto-class:" + dtoClass.getName();
            throw new RuntimeException(message, e);
        }
    }

    public void setAttributes(EntityDto dto, Model model) {
        if (model instanceof NamedModel) {
            dto.putValue(Attribute.NAME.getName(), ((NamedModel) model).getName());
        }

        for (Attribute<?, ?> attr : Attribute.getAttributes(model.getClass())) {
            String key = attr.getName();
            Object value = model.get((Attribute<?, Model>) attr);

            if (attr instanceof Attribute.CollectionAttr<?, ?, ?, ?>) {
                AttributeType.MvoCollectionType<?, ?> mvocollectionType
                    = (AttributeType.MvoCollectionType<?, ?>) attr.getType();
                if (isSerializable(mvocollectionType.getCollectionType())) {
                    MvoCollection<?, ?> mvocollection = (MvoCollection<?, ?>) value;
                    dto.putValue(
                        key,
                        value == null
                            ? ((Attribute.CollectionAttr<?, ?, ?, ?>) attr).emptyJavaCollection()
                            : mvocollection.get());
                }
            } else if (attr instanceof Attribute.MapAttr<?, ?, ?>) {
                Attribute.MapAttr<Object, ?, Model> mappingAttr = (Attribute.MapAttr<Object, ?, Model>) attr;
                AttributeType.MvoMapType<?, ?> mapType = mappingAttr.getType();
                if (isSerializable(mapType.getKeyType())
                    && isSerializable(mapType.getValueType()))
                {
                    dto.putValue(key, mappingAttr.snapshot(model));
                }
            } else if (attr.getType() != null && attr.getType().isValueSerializable()) {
                if (value != null && ! (value instanceof Serializable)) {
                    throw new IllegalStateException(
                        "直列化可能でない値が代入されています: " + model.getClass().getName() + " " + attr.getName());
                }

                dto.putValue(key, value);
            }
        }

        for (DtoInitializer<?, ?> initializer : getInitializers()) {
            if (initializer.isToInitialize(dto)) {
                initialize(initializer, model, dto);
            }
        }

        for (DtoAttrTranscript<?, ?> transcript : getAggressiveTranscripts(dto.getClass())) {
            dto.putValue(
                transcript.getAttribute().getName(),
                getAttributeValue((Model) model, (DtoAttrTranscript<?, Model>) transcript));
        }
    }

    private boolean isSerializable(Class<?> klass) {
        return Serializable.class.isAssignableFrom(klass);
    }

    private <MODEL, DTO> void initialize(DtoInitializer<MODEL, DTO> initializer, Object mvo, Object dto) {
        initializer.initialize((MODEL) mvo, (DTO) dto);
    }

    /**
     * EntityDto の属性の値を取得します.
     * <p>
     * getAttributeInitialValue(TransactionId.W,Long,MVO.MvoId,String) に委譲しますが, 
     * 引数#1 の TransactionId.W は MvoDtoDesc#getVersion() ではなく MvoDtoDesc#getTimestamp() 
     * が使用されることに注意してください.
     * <p>
     * これは MvoDto 生成時の transaction context target version は MvoDtoDesc#getTimestamp()
     * で表されるためです.
     */
    public Object getAttributeValue(MvoDtoDesc<?> ref, String attrName) {
        return getAttributeValue(ref.getTimestamp(), ref.getTime(), ref.getMvoId(), attrName);
    }

    /**
     * MvoDto の遅延初期化属性の値を取得します.
     */
    public Object getAttributeValue(TransactionId.W version, Long time, MVO.MvoId mvoId, String attrName) {
        TransactionId.W savedVersion = TransactionContext.getTargetVersion();
        long savedTime = TransactionContext.getTargetTime();
        try {
            TransactionContext.setTargetVersion(version);
            TransactionContext.setTargetTime(time);

            SkeltonTefService tefservice = SkeltonTefService.instance();

            MVO mvo = tefservice.getMvoRegistry().get(mvoId);
            if (mvo == null) {
                throw new IllegalArgumentException(
                    "no such object: " + mvoId + ", " + TransactionContext.getTransactionId());
            }
            Class<? extends EntityDto> dtoClass = tefservice.getMvoDtoMapping().getDtoClass(mvo.getClass());

            DtoAttrTranscript<?, ?> transcript = tefservice.getMvoDtoFactory().getDtoAttrTranscript(dtoClass, attrName);
            if (transcript == null) {
                throw new RuntimeException("no attribute transcript found: " + dtoClass.getName() + ", " + attrName);
            }

            return getAttributeValue((MVO) mvo, (DtoAttrTranscript<?, MVO>) transcript);
        } finally {
            TransactionContext.setTargetVersion(savedVersion);
            TransactionContext.setTargetTime(savedTime);
        }
    }

    private <T> Object getAttributeValue(T obj, DtoAttrTranscript<?, T> transcript) {
        return transcript.get(obj);
    }
}
