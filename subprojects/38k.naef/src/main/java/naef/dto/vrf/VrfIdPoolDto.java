package naef.dto.vrf;

import naef.dto.IdPoolDto;

public abstract class VrfIdPoolDto<R extends VrfIdPoolDto<R, S>, S extends Object & Comparable<S>>
    extends IdPoolDto<R, S, VrfDto>
{
    public VrfIdPoolDto() {
        super();
    }
}
