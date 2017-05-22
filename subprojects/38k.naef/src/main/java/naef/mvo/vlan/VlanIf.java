package naef.mvo.vlan;

import naef.mvo.AbstractPort;
import naef.mvo.NaefMvoUtils;
import naef.mvo.Network;
import naef.mvo.Node;
import naef.mvo.NodeElement;
import naef.mvo.Port;
import naef.mvo.eth.EthLag;
import naef.mvo.eth.EthLagIf;
import naef.mvo.eth.EthLink;
import naef.mvo.eth.EthPort;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.ConfigurationException;
import tef.skelton.ValueException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class VlanIf extends AbstractPort {

    public static class Attr {

        public static final Attribute.SingleInteger<VlanIf> VLAN_ID
            = new Attribute.SingleInteger<VlanIf>("naef.vlan.vlan-if.vlan-id")
        {
            @Override public void validateValue(VlanIf model, Integer vlanid)
                throws ValueException, ConfigurationException
            {
                if (vlanid == null) {
                    return;
                }

                NodeElement owner = model.getOwner();

                VlanType vlanType = VlanAttrs.ENABLED_NETWORKING_FUNCTION_VLAN.get(owner);
                if (vlanType == null) {
                    throw new ConfigurationException(
                        owner.getFqn() + " に " + VlanAttrs.ENABLED_NETWORKING_FUNCTION_VLAN.getName()
                        + " が設定されていません.");
                }

                if (! vlanType.isIdInRange(vlanid)) {
                    throw new ValueException(vlanType.getTypeName() + " のID範囲を超えています.");
                }

                checkIdDuplication(owner, model, vlanid);
            }

            private void checkIdDuplication(NodeElement owner, VlanIf vlanif, Integer vlanid)
                throws ValueException
            {
                for (VlanIf siblingVlanif : NaefMvoUtils.getHereafterSubElements(owner, VlanIf.class, false)) {
                    if (siblingVlanif == vlanif) {
                        continue;
                    }

                    Integer siblingVlanifVlanid = siblingVlanif.get(this);
                    if (vlanid.equals(siblingVlanifVlanid)) {
                        throw new ValueException("割当済のVLAN IDです.");
                    }
                }
            }
        };

        public static final Attribute.SetAttr<VlanSegmentGatewayIf, VlanIf> VLAN_SEGMENT_GATEWAY_IFS
            = new Attribute.SetAttr<VlanSegmentGatewayIf, VlanIf>(
                "naef.vlan.vlan-if.vlan-segment-gateway-ifs",
                new AttributeType.MvoSetType<VlanSegmentGatewayIf>(VlanSegmentGatewayIf.class) {

                    @Override public VlanSegmentGatewayIf parseElement(String valueStr) {
                        throw new UnsupportedOperationException();
                    }
                });
        static {
            VLAN_SEGMENT_GATEWAY_IFS.addPostProcessor(
                new Attribute.SetAttr.PostProcessor<VlanSegmentGatewayIf, VlanIf>() {

                    @Override public void add(VlanIf model, VlanSegmentGatewayIf value) {
                        if (VlanSegmentGatewayIf.Attr.VLAN_IF.get(value) != model) {
                            VlanSegmentGatewayIf.Attr.VLAN_IF.set(value, model);
                        }
                    }

                    @Override public void remove(VlanIf model, VlanSegmentGatewayIf value) {
                        if (VlanSegmentGatewayIf.Attr.VLAN_IF.get(value) == model) {
                            VlanSegmentGatewayIf.Attr.VLAN_IF.set(value, null);
                        }
                    }
                });
        }
    }

    public VlanIf(MvoId id) {
        super(id);
    }

    public VlanIf() {
    }

    public Set<Port> getCurrentTaggedPorts() {
        Set<Port> result = new HashSet<Port>();
        for (Port lowerPort : getLowerLayerPorts()) {
            if (lowerPort instanceof EthPort || lowerPort instanceof EthLagIf) {
                result.add(lowerPort);
            }
        }

        Node node = getNode();
        for (VlanSegment segment : getCurrentNetworks(VlanSegment.class)) {
            for (Network lowerLayer : segment.getCurrentLowerLayers(false)) {
                if (lowerLayer instanceof EthLink || lowerLayer instanceof EthLag) {
                    for (Port endpoint : lowerLayer.getCurrentMemberPorts()) {
                        if (endpoint.getNode() == node) {
                            result.add(endpoint);
                        }
                    }
                }
            }
        }
        return result;
    }

    public Collection<? extends Port> getCurrentUntaggedPorts() {
        return getCurrentCrossConnectedPorts();
    }

    public Vlan getVlan() {
        Vlan result = null;
        for (Network network : getCurrentNetworks(Network.class)) {
            Vlan vlan = null;
            if (network instanceof VlanSegment) {
                vlan = (Vlan) ((VlanSegment) network).getCurrentContainer();
            } else if (network instanceof Vlan) {
                vlan = (Vlan) network;
            }

            if (vlan == null) {
                continue;
            }

            if (result == null) {
                result = vlan;
            } else if (result != vlan) {
                throw new RuntimeException();
            }
        }
        return result;
    }
}
