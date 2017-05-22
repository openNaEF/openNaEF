package naef.dto.mpls;

import naef.dto.IdPoolDto;

public class PseudowireLongIdPoolDto
    extends IdPoolDto.LongType<PseudowireLongIdPoolDto, PseudowireDto>
{
    public PseudowireLongIdPoolDto() {
        super();
    }

    @Override public Long getId(PseudowireDto dto) {
        return dto.getLongId();
    }
}
