package naef.dto.vpls;

import naef.dto.IdRange;
import tef.skelton.Range;

public class VplsStringIdPoolDto extends VplsIdPoolDto<VplsStringIdPoolDto, String> {

    public VplsStringIdPoolDto() {
    }

    @Override public IdRange<String> newIdRange(Range<?> range) {
        return newStringIdRange((Range<String>) range);
    }

    @Override public String getId(VplsDto dto) {
        return dto.getStringId();
    }
}
