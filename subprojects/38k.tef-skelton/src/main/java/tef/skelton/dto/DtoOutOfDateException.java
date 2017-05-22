package tef.skelton.dto;

import tef.skelton.KnownRuntimeException;

public class DtoOutOfDateException extends KnownRuntimeException {

    public final EntityDto.Desc<?> desc;

    public DtoOutOfDateException(EntityDto.Desc<?> desc, String message) {
        super(message);
        this.desc = desc;
    }
}
