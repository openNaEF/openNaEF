package naef.dto;

import java.util.Set;

import tef.skelton.Attribute;
import tef.skelton.NamedModel;

public class NodeGroupDto extends NaefDto implements NamedModel {

    public static class ExtAttr {

        public static final SetRefAttr<NodeDto, NodeGroupDto> MEMBERS
            = new SetRefAttr<NodeDto, NodeGroupDto>("naef.dto.node-group.members");
    }

    public NodeGroupDto() {
    }

    @Override public String getName() {
        return Attribute.NAME.get(this);
    }

    public Set<NodeDto> getMembers() {
        return ExtAttr.MEMBERS.deref(this);
    }
}
