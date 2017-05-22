package naef.dto.eth;

import naef.dto.LinkDto;
import naef.dto.SoftPortDto;

import java.util.Set;

public class EthLagIfDto extends SoftPortDto {

    public static class ExtAttr {

        public static final SingleRefAttr<LinkDto, EthLagIfDto> LINK
            = new SingleRefAttr<LinkDto, EthLagIfDto>("link");
        public static final SetRefAttr<EthPortDto, EthLagIfDto> BUNDLE_PORTS
            = new SetRefAttr<EthPortDto, EthLagIfDto>("bundle ports");
    }

    public EthLagIfDto() {
    }

    public LinkDto getLink() {
        return ExtAttr.LINK.deref(this);
    }

    public Set<EthPortDto> getBundlePorts() {
        return ExtAttr.BUNDLE_PORTS.deref(this);
    }
}
