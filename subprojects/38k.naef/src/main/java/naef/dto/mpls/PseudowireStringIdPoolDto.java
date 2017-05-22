package naef.dto.mpls;

import naef.dto.IdPoolDto;

public class PseudowireStringIdPoolDto
    extends IdPoolDto.StringType<PseudowireStringIdPoolDto, PseudowireDto>
{
    public PseudowireStringIdPoolDto() {
        super();
    }

    @Override public String getId(PseudowireDto dto) {
        return dto.getStringId();
    }
}
