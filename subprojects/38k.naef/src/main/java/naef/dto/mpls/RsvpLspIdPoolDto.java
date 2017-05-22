package naef.dto.mpls;

import naef.dto.IdPoolDto;

public class RsvpLspIdPoolDto 
    extends IdPoolDto.StringType<RsvpLspIdPoolDto, RsvpLspDto>
{
    public RsvpLspIdPoolDto() {
        super();
    }

    @Override public String getId(RsvpLspDto dto) {
        return dto.getName();
    }
}
