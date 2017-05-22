package naef.dto;

import naef.mvo.Node;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;

import java.util.Collection;
import java.util.Set;

public class NodeDto extends NodeElementDto {

    public static final AttributeType<NodeDto> TYPE = new AttributeType.Adapter<NodeDto>();

    public static class ExtAttr {

        /**
         * {@link naef.mvo.Node.Attr#OBJECT_TYPE} のDTO転写属性です.
         */
        public static final SingleRefAttr<NodeTypeDto, NodeDto> OBJECT_TYPE
            = new SingleRefAttr<NodeTypeDto, NodeDto>(naef.mvo.AbstractNodeElement.Attr.OBJECT_TYPE_FIELD_NAME);

        public static final Attribute.SingleString<NodeDto> NODE_NAME
            = new Attribute.SingleString<NodeDto>("ノード名");
        public static final SetRefAttr<ChassisDto, NodeDto> CHASSISES
            = new SetRefAttr<ChassisDto, NodeDto>("シャーシ");
        public static final SetRefAttr<JackDto, NodeDto> JACKS
            = new SetRefAttr<JackDto, NodeDto>("naef.dto.node.jacks");
        public static final SetRefAttr<PortDto, NodeDto> PORTS
            = new SetRefAttr<PortDto, NodeDto>("naef.dto.node.ports");
        public static final SetRefAttr<NodeGroupDto, NodeDto> NODE_GROUPS
            = new SetRefAttr<NodeGroupDto, NodeDto>("naef.dto.node.node-groups");

        public static final SetRefAttr<NodeDto, NodeDto> VIRTUALIZATION_HOST_NODES
            = new SetRefAttr<NodeDto, NodeDto>("naef.dto.node.virtualization-host-nodes");
        public static final SetRefAttr<NodeDto, NodeDto> VIRTUALIZATION_GUEST_NODES
            = new SetRefAttr<NodeDto, NodeDto>("naef.dto.node.virtualization-guest-nodes");
    }

    public NodeDto() {
    }

    public Set<ChassisDto> getChassises() {
        return ExtAttr.CHASSISES.deref(this);
    }

    public Set<JackDto> getJacks() {
        return ExtAttr.JACKS.deref(this);
    }

    public Set<PortDto> getPorts() {
        return ExtAttr.PORTS.deref(this);
    }

    public Set<NodeGroupDto> getNodeGroups() {
        return ExtAttr.NODE_GROUPS.deref(this);
    }

    public boolean isVirtualizedHostingEnabled() {
        Boolean value = (Boolean) getValue(Node.Attr.VIRTUALIZED_HOSTING_ENABLED.getName());
        return value == null ? false : value.booleanValue();
    }

    public Set<NodeDto> getVirtualizationGuestNodes() {
        return ExtAttr.VIRTUALIZATION_GUEST_NODES.deref(this);
    }

    public Node.VirtualizationHostedType getVirtualizationHostedType() {
        Node.VirtualizationHostedType result
            = (Node.VirtualizationHostedType) getValue(Node.Attr.VIRTUALIZATION_HOSTED_TYPE.getName());
        return result == null
            ? Node.VirtualizationHostedType.NONE
            : result;
    }

    public Set<NodeDto> getVirtualizationHostNodes() {
        return ExtAttr.VIRTUALIZATION_HOST_NODES.deref(this);
    }

    public NodeDto getVirtualizationHostNode() {
        Collection<NodeDto> hosts = getVirtualizationHostNodes();
        return hosts.size() == 0 ? null : hosts.iterator().next();
    }
}
