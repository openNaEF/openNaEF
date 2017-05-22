package naef.dto.vpls;

import naef.dto.IdPoolDto;

public abstract class VplsIdPoolDto<R extends VplsIdPoolDto<R, S>, S extends Object & Comparable<S>>
    extends IdPoolDto<R, S, VplsDto>
{
    public VplsIdPoolDto() {
    }
}
