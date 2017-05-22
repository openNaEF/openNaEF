package tef.skelton.dto;

import tef.MVO;
import tef.skelton.Attribute;
import tef.skelton.AttributeTask;
import tef.skelton.Model;
import tef.skelton.SkeltonTefService;

public class TaskSynthesizedDtoBuilder {

    public static class SynthesizeException extends Exception {

        public SynthesizeException(String message) {
            super(message);
        }
    }

    private final MvoDtoFactory dtoFactory_;

    public TaskSynthesizedDtoBuilder() {
        dtoFactory_ = SkeltonTefService.instance().getMvoDtoFactory();
    }

    public EntityDto build(DtoOriginator originator, AttributeTask task)
        throws SynthesizeException
    {
        Model mvo = task.getConfiguratee();
        EntityDto dto = dtoFactory_.build(originator, (MVO) mvo);

        synthesize(mvo, dto, task);

        return dto;
    }

    protected void synthesize(Model mvo, EntityDto dto, AttributeTask task)
        throws SynthesizeException
    {
        for (String attrName : task.getAttributeNames()) {
            Attribute<?, ?> attr = Attribute.getAttribute(mvo.getClass(), attrName);
            Object value = task.getValue(attrName);

            dto.putValue(attrName, convertToDtoAdaptiveValue(mvo, dto, attr, value));
        }
    }

    protected Object convertToDtoAdaptiveValue(Model mvo, EntityDto dto, Attribute<?, ?> attribute, Object value)
        throws SynthesizeException
    {
        if (Attribute.getSerializableAttributes(mvo.getClass()).contains(attribute)) {
            return value;
        }

        throw new SynthesizeException(
            "DTOへのタスク合成がサポートされていない属性です: " + mvo.getClass().getName() + ", " + attribute.getName());
    }
}
