package naef.dto;

import java.util.Collection;

public abstract class HardPortDto extends PortDto {

    public static class ExtAttr {

        public static final SingleRefAttr<LinkDto, HardPortDto> L1LINK
            = new SingleRefAttr<LinkDto, HardPortDto>("naef.dto.hard-port.l1link");
        public static final SingleRefAttr<PortDto, HardPortDto> L1NEIGHBOR
            = new SingleRefAttr<PortDto, HardPortDto>("naef.dto.hard-port.l1neighbor");

        public static final SetRefAttr<LinkDto, HardPortDto> L2LINKS
            = new SetRefAttr<LinkDto, HardPortDto>("naef.dto.hard-port.l2links");
        public static final SetRefAttr<PortDto, HardPortDto> L2NEIGHBORS
            = new SetRefAttr<PortDto, HardPortDto>("naef.dto.hard-port.l2neighbors");
    }

    protected HardPortDto() {
    }

    public LinkDto getL1Link() {
        return ExtAttr.L1LINK.deref(this);
    }

    public PortDto getL1Neighbor() {
        return ExtAttr.L1NEIGHBOR.deref(this);
    }

    public Collection<LinkDto> getL2Links() {
        return ExtAttr.L2LINKS.deref(this);
    }

    public Collection<PortDto> getL2Neighbors() {
        return ExtAttr.L2NEIGHBORS.deref(this);
    }

    /**
     * 過去のAPIとの互換性のためのメソッドです。rev.360 で多重度が 0..1 から 0..n に変更されて
     * います。呼び出しを {@link HardPortDto#getL2Links()} に切り替えてください。
     **/
    @Deprecated public LinkDto getL2Link() {
        Collection<LinkDto> l2links = getL2Links();
        if (l2links.size() > 1) {
            throw new IllegalStateException("l2link の多重度に問題があります: " + getOid() + ", " + l2links.size());
        }

        return l2links.size() == 0
            ? null
            : l2links.iterator().next();
    }

    /**
     * 過去のAPIとの互換性のためのメソッドです。rev.360 で多重度が 0..1 から 0..n に変更されて
     * います。呼び出しを {@link HardPortDto#getL2Neighbors()} に切り替えてください。
     **/
    @Deprecated public PortDto getL2Neighbor() {
        Collection<PortDto> l2neighbors = getL2Neighbors();
        if (l2neighbors.size() > 1) {
            throw new IllegalStateException(
                "l2neighbor の多重度に問題があります: " + getOid() + ", " + l2neighbors.size());
        }

        return l2neighbors.size() == 0
            ? null
            : l2neighbors.iterator().next();
    }
}
