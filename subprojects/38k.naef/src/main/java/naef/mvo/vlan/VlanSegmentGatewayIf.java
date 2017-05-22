package naef.mvo.vlan;

import naef.mvo.AbstractPort;
import naef.mvo.NaefMvoUtils;
import tef.skelton.Attribute;
import tef.skelton.ValueException;

public class VlanSegmentGatewayIf extends AbstractPort {

    public static class Attr {

        public static final Attribute.SingleInteger<VlanSegmentGatewayIf> ID
            = new Attribute.SingleInteger<VlanSegmentGatewayIf>("naef.vlan.vlan-segment-gateway-if.id")
        {
            @Override public void validateValue(VlanSegmentGatewayIf model, Integer id)
                throws ValueException
            {
                if (id == null) {
                    return;
                }

                for (VlanSegmentGatewayIf sibling
                    : NaefMvoUtils.getHereafterSubElements(model.getOwner(), VlanSegmentGatewayIf.class, false))
                {
                    if (sibling == model) {
                        continue;
                    }

                    if (id.equals(this.get(sibling))) {
                        throw new ValueException("IDの重複が検出されました: " + id);
                    }
                }
            }
        };

        public static final Attribute.SingleModel<VlanSegment, VlanSegmentGatewayIf> VLAN_SEGMENT
            = new Attribute.SingleModel<VlanSegment, VlanSegmentGatewayIf>(
                "naef.vlan.vlan-segment-gateway-if.vlan-segment",
                VlanSegment.class);
        static {
            VLAN_SEGMENT.addPostProcessor(new Attribute.SingleAttr.PostProcessor<VlanSegment, VlanSegmentGatewayIf>() {

                @Override public void set(VlanSegmentGatewayIf model, VlanSegment oldValue, VlanSegment newValue) {
                    if (oldValue != null
                        && VlanSegment.Attr.VLAN_SEGMENT_GATEWAY_IFS.containsValue(oldValue, model))
                    {
                        VlanSegment.Attr.VLAN_SEGMENT_GATEWAY_IFS.removeValue(oldValue, model);
                    }
                    if (newValue != null
                        && ! VlanSegment.Attr.VLAN_SEGMENT_GATEWAY_IFS.containsValue(newValue, model))
                    {
                        VlanSegment.Attr.VLAN_SEGMENT_GATEWAY_IFS.addValue(newValue, model);
                    }
                }
            });
        }

        public static final Attribute.SingleModel<VlanIf, VlanSegmentGatewayIf> VLAN_IF
            = new Attribute.SingleModel<VlanIf, VlanSegmentGatewayIf>(
                "naef.vlan.vlan-segment-gateway-if.vlan-if",
                VlanIf.class);
        static {
            VLAN_IF.addPostProcessor(new Attribute.SingleAttr.PostProcessor<VlanIf, VlanSegmentGatewayIf>() {

                @Override public void set(VlanSegmentGatewayIf model, VlanIf oldValue, VlanIf newValue) {
                    if (oldValue != null
                        && VlanIf.Attr.VLAN_SEGMENT_GATEWAY_IFS.containsValue(oldValue, model))
                    {
                        VlanIf.Attr.VLAN_SEGMENT_GATEWAY_IFS.removeValue(oldValue, model);
                    }
                    if (newValue != null
                        && ! VlanIf.Attr.VLAN_SEGMENT_GATEWAY_IFS.containsValue(newValue, model))
                    {
                        VlanIf.Attr.VLAN_SEGMENT_GATEWAY_IFS.addValue(newValue, model);
                    }
                }
            });
        }
    }

    public VlanSegmentGatewayIf(MvoId id) {
        super(id);
    }

    public VlanSegmentGatewayIf() {
    }
}
