package naef.mvo.serial;

import naef.mvo.AbstractPort;
import naef.mvo.NaefMvoUtils;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.ConfigurationException;
import tef.skelton.ValueException;

public class TdmSerialIf extends AbstractPort {

    public static class Attr {

        public static final Attribute.SetAttr<Integer, TdmSerialIf> TIMESLOTS
            = new Attribute.SetAttr<Integer, TdmSerialIf>(
                "naef.serial.tdm-serial-if.timeslots",
                AttributeType.MvoSetType.INTEGER)
        {
            @Override public void validateAddValue(TdmSerialIf model, Integer value)
                throws ValueException, ConfigurationException
            {
                super.validateAddValue(model, value);

                for (TdmSerialIf sibling : NaefMvoUtils.getHereafterSubElements(model, TdmSerialIf.class, false)) {
                    if (sibling == model) {
                        continue;
                    }

                    if (this.snapshot(sibling).contains(value)) {
                        throw new ValueException("割当済です.");
                    }
                }
            }
        };
    }

    public TdmSerialIf(MvoId id) {
        super(id);
    }

    public TdmSerialIf() {
    }
}
