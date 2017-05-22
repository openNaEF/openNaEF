package naef.dto.mpls;

import naef.dto.NetworkDto;
import naef.dto.PortDto;
import naef.mvo.mpls.Pseudowire;
import tef.skelton.Attribute;

import java.util.Set;

public class PseudowireDto extends NetworkDto {

    public static class ExtAttr {

        @Deprecated public static final SingleRefAttr<PseudowireIdPoolDto, PseudowireDto> PSEUDOWIRE_ID_POOL
            = new SingleRefAttr<PseudowireIdPoolDto, PseudowireDto>("pseudowire id pool");
        @Deprecated public static final Attribute.SingleLong<PseudowireDto> VC_ID
            = new Attribute.SingleLong<PseudowireDto>("VC ID");

        public static final SingleRefAttr<PseudowireLongIdPoolDto, PseudowireDto> LONG_IDPOOL
            = new SingleRefAttr<PseudowireLongIdPoolDto, PseudowireDto>("naef.dto.pseudowire.id-pool.long-type");
        public static final Attribute.SingleLong<PseudowireDto> LONG_ID
            = new Attribute.SingleLong<PseudowireDto>("naef.dto.pseudowire.id.long-type");
        public static final SingleRefAttr<PseudowireStringIdPoolDto, PseudowireDto> STRING_IDPOOL
            = new SingleRefAttr<PseudowireStringIdPoolDto, PseudowireDto>("naef.dto.pseudowire.id-pool.string-type");
        public static final Attribute.SingleString<PseudowireDto> STRING_ID
            = new Attribute.SingleString<PseudowireDto>("naef.dto.pseudowire.id.string-type");
        public static final Attribute.SingleEnum<Pseudowire.TransportType, PseudowireDto> TRANSPORT_TYPE
            = new Attribute.SingleEnum<Pseudowire.TransportType, PseudowireDto>(
                "transport type",
                Pseudowire.TransportType.class);
        public static final SingleRefAttr<PortDto, PseudowireDto> AC1
            = new SingleRefAttr<PortDto, PseudowireDto>("AC1");
        public static final SingleRefAttr<PortDto, PseudowireDto> AC2
            = new SingleRefAttr<PortDto, PseudowireDto>("AC2");
        public static final SetRefAttr<RsvpLspDto, PseudowireDto> RSVPLSPS
            = new SetRefAttr<RsvpLspDto, PseudowireDto>("rsvp-lsps");
    }

    public PseudowireDto() {
    }

    @Deprecated public PseudowireIdPoolDto getIdPool() {
        return ExtAttr.PSEUDOWIRE_ID_POOL.deref(this);
    }

    @Deprecated public Long getVcId() {
        return ExtAttr.VC_ID.get(this);
    }

    public PseudowireLongIdPoolDto getLongIdPool() {
        return ExtAttr.LONG_IDPOOL.deref(this);
    }

    public Long getLongId() {
        return ExtAttr.LONG_ID.get(this);
    }

    public PseudowireStringIdPoolDto getStringIdPool() {
        return ExtAttr.STRING_IDPOOL.deref(this);
    }

    public String getStringId() {
        return ExtAttr.STRING_ID.get(this);
    }

    public PortDto getAc1() {
        return ExtAttr.AC1.deref(this);
    }

    public PortDto getAc2() {
        return ExtAttr.AC2.deref(this);
    }

    public Set<RsvpLspDto> getRsvpLsps() {
        return ExtAttr.RSVPLSPS.deref(this);
    }
}
