package naef.dto.vxlan;

import naef.dto.IdPoolDto;

public class VxlanIdPoolDto extends IdPoolDto.LongType<VxlanIdPoolDto, VxlanDto> {

    public VxlanIdPoolDto() {
    }

    @Override public Long getId(VxlanDto dto) {
        return dto.getId();
    }
}
