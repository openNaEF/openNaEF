package naef.mvo.vlan;

import naef.mvo.AbstractNetwork;
import naef.mvo.Network;
import naef.mvo.Port;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class VlanSegment 
    extends AbstractNetwork 
    implements Network.Containee, Network.UpperStackable
{
    public static class Attr {

        public static final Attribute.SetAttr<VlanSegmentGatewayIf, VlanSegment> VLAN_SEGMENT_GATEWAY_IFS
            = new Attribute.SetAttr<VlanSegmentGatewayIf, VlanSegment>(
                "naef.vlan.vlan-segment.vlan-segment-gateway-ifs",
                new AttributeType.MvoSetType<VlanSegmentGatewayIf>(VlanSegmentGatewayIf.class) {

                    @Override public VlanSegmentGatewayIf parseElement(String valueStr) {
                        throw new UnsupportedOperationException();
                    }
                });
        static {
            VLAN_SEGMENT_GATEWAY_IFS.addPostProcessor(
                new Attribute.SetAttr.PostProcessor<VlanSegmentGatewayIf, VlanSegment>() {

                    @Override public void add(VlanSegment model, VlanSegmentGatewayIf value) {
                        if (VlanSegmentGatewayIf.Attr.VLAN_SEGMENT.get(value) != model) {
                            VlanSegmentGatewayIf.Attr.VLAN_SEGMENT.set(value, model);
                        }
                    }

                    @Override public void remove(VlanSegment model, VlanSegmentGatewayIf value) {
                        if (VlanSegmentGatewayIf.Attr.VLAN_SEGMENT.get(value) == model) {
                            VlanSegmentGatewayIf.Attr.VLAN_SEGMENT.set(value, null);
                        }
                    }
                });
        }
    }

    protected VlanSegment(MvoId id) {
        super(id);
    }

    protected VlanSegment() {
    }

    @Override abstract public Collection<VlanIf> getCurrentMemberPorts();

    @Override public Set<Port> getCurrentAttachedPorts() {
        Set<Port> result = new HashSet<Port>();
        for (VlanIf vlanif : getCurrentMemberPorts()) {
            result.addAll(vlanif.getCurrentUntaggedPorts());
        }
        return result;
    }
}
