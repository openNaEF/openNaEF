package naef.dto.vlan;

import naef.dto.IdPoolDto;

public class VlanIdPoolDto
    extends IdPoolDto.IntegerType<VlanIdPoolDto, VlanDto>
{

    public VlanIdPoolDto() {
    }

    @Override public Integer getId(VlanDto dto) {
        return dto.getVlanId();
    }
}
