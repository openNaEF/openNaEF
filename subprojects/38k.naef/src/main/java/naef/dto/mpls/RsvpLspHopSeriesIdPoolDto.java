package naef.dto.mpls;

import naef.dto.IdPoolDto;

public class RsvpLspHopSeriesIdPoolDto
    extends IdPoolDto.StringType<RsvpLspHopSeriesIdPoolDto, RsvpLspHopSeriesDto> 
{
    public RsvpLspHopSeriesIdPoolDto() {
        super();
    }

    @Override public String getId(RsvpLspHopSeriesDto dto) {
        return dto.getName();
    }
}
