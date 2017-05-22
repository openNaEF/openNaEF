package naef.dto.of;

import naef.dto.IdPoolDto;
import naef.dto.IdRange;
import naef.dto.NetworkDto;
import naef.dto.PortDto;
import tef.skelton.Attribute;
import tef.skelton.Range;

public class OfPatchLinkDto extends NetworkDto {

    public static class PatchIdPoolDto
        extends IdPoolDto<PatchIdPoolDto, String, OfPatchLinkDto>
    {
        public PatchIdPoolDto() {
        }

        @Override public IdRange<String> newIdRange(Range<?> range) {
            return newStringIdRange((Range<String>) range);
        }

        @Override public String getId(OfPatchLinkDto dto) {
            return dto.getPatchId();
        }
    }

    public static final SingleRefAttr<PatchIdPoolDto, OfPatchLinkDto> PATCH_ID_POOL
        = new SingleRefAttr<PatchIdPoolDto, OfPatchLinkDto>("naef.dto.of-patch-link.patch-id-pool");
    public static final Attribute.SingleString<OfPatchLinkDto> PATCH_ID
        = new Attribute.SingleString<OfPatchLinkDto>("naef.dto.of-patch-link.patch-id");
    public static final SingleRefAttr<PortDto, OfPatchLinkDto> PATCH_PORT1
        = new SingleRefAttr<PortDto, OfPatchLinkDto>("patch-port.1");
    public static final SingleRefAttr<PortDto, OfPatchLinkDto> PATCH_PORT2
        = new SingleRefAttr<PortDto, OfPatchLinkDto>("patch-port.2");

    public OfPatchLinkDto() {
    }

    public PatchIdPoolDto getPatchIdPool() {
        return PATCH_ID_POOL.deref(this);
    }

    public String getPatchId() {
        return PATCH_ID.get(this);
    }

    public PortDto getPatchPort1() {
        return PATCH_PORT1.deref(this);
    }

    public PortDto getPatchPort2() {
        return PATCH_PORT2.deref(this);
    }
}
