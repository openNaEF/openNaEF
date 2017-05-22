package naef.dto.vpls;

import naef.dto.IdRange;
import tef.skelton.Range;

public class VplsIntegerIdPoolDto extends VplsIdPoolDto<VplsIntegerIdPoolDto, Integer> {

    public VplsIntegerIdPoolDto() {
    }

    @Override public IdRange<Integer> newIdRange(Range<?> range) {
        return newIntegerIdRange((Range<Integer>) range);
    }

    @Override public Integer getId(VplsDto dto) {
        return dto.getIntegerId();
    }
}
