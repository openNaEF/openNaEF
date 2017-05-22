package naef.dto.vrf;

import naef.dto.IdRange;
import tef.skelton.Range;

public class VrfStringIdPoolDto extends VrfIdPoolDto<VrfStringIdPoolDto, String> {

    public VrfStringIdPoolDto() {
        super();
    }

    @Override public IdRange<String> newIdRange(Range<?> range) {
        return newStringIdRange((Range<String>) range);
    }

    @Override public String getId(VrfDto dto) {
        return dto.getStringId();
    }
}
