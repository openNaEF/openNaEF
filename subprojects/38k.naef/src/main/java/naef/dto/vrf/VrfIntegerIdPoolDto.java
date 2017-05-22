package naef.dto.vrf;

import naef.dto.IdRange;
import tef.skelton.Range;

public class VrfIntegerIdPoolDto extends VrfIdPoolDto<VrfIntegerIdPoolDto, Integer> {

    public VrfIntegerIdPoolDto() {
        super();
    }

    @Override public IdRange<Integer> newIdRange(Range<?> range) {
        return newIntegerIdRange((Range<Integer>) range);
    }

    @Override public Integer getId(VrfDto dto) {
        return dto.getIntegerId();
    }
}
