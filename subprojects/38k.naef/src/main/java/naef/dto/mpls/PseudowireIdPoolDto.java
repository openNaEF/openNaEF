package naef.dto.mpls;

import naef.dto.IdPoolDto;

@Deprecated public class PseudowireIdPoolDto
    extends IdPoolDto.LongType<PseudowireIdPoolDto, PseudowireDto>
{
    public PseudowireIdPoolDto() {
        super();
    }

    @Override public Long getId(PseudowireDto dto) {
        return dto.getVcId();
    }
}
