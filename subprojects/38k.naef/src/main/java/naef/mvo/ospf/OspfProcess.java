package naef.mvo.ospf;

import java.util.Set;

import naef.mvo.RoutingProcess;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.ConstraintException;
import tef.skelton.ValueException;

public class OspfProcess extends RoutingProcess {

    public static class Attr {

        public static final Attribute.SetAttr<OspfAreaId, OspfProcess> AREA_ID
            = new Attribute.SetAttr<OspfAreaId, OspfProcess>(
                "naef.area-ids",
                new AttributeType.MvoSetType<OspfAreaId>(OspfAreaId.class) {

                    @Override public OspfAreaId parseElement(String str) {
                        return OspfAreaId.gain(str);
                    }
                })
        {
            @Override public void validateAddValue(OspfProcess model, OspfAreaId value) {
                super.validateAddValue(model, value);

                Set<OspfAreaId> values = snapshot(model);
                if (value.getIdValue() != 0
                    && 0 < values.size()
                    && ! values.contains(OspfAreaId.gain(0)))
                {
                    throw new ValueException("ABR はエリア " + OspfAreaId.gain(0) + " が必須です.");
                }
            }
        };

        public static final Attribute.SetStringAttr<OspfProcess> SHAM_LINKS
            = new Attribute.SetStringAttr<OspfProcess>("naef.sham-links");
    }

    public OspfProcess(MvoId id) {
        super(id);
    }

    public OspfProcess(String name) throws ConstraintException {
        super(name);
    }
}
