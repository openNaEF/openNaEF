package naef.dto;

import tef.skelton.Attribute;
import tef.skelton.dto.Dto;

public class InternodeLinkDto extends Dto {

    public static class ExtAttr {

        public static final Attribute.SingleAttr<NodeDto, InternodeLinkDto> NODE1
            = new Attribute.SingleAttr<NodeDto, InternodeLinkDto>("Node1", NodeDto.TYPE);
        public static final Attribute.SingleAttr<NodeDto, InternodeLinkDto> NODE2
            = new Attribute.SingleAttr<NodeDto, InternodeLinkDto>("Node2", NodeDto.TYPE);
    }

    public InternodeLinkDto(NodeDto node1, NodeDto node2) {
        set(ExtAttr.NODE1, node1);
        set(ExtAttr.NODE2, node2);
    }

    public NodeDto getNode1() {
        return ExtAttr.NODE1.get(this);
    }

    public NodeDto getNode2() {
        return ExtAttr.NODE2.get(this);
    }

    @Override public int hashCode() {
        return getNode1().getOid().hashCode() + getNode2().getOid().hashCode();
    }

    @Override public boolean equals(Object o) {
        if (! (o instanceof InternodeLinkDto)) {
            return false;
        }

        InternodeLinkDto another = (InternodeLinkDto) o;
        return (DtoUtils.isSameEntity(getNode1(), another.getNode1())
                && DtoUtils.isSameEntity(getNode2(), another.getNode2()))
            || (DtoUtils.isSameEntity(getNode1(), another.getNode2())
                && DtoUtils.isSameEntity(getNode2(), another.getNode1()));
    }
}
