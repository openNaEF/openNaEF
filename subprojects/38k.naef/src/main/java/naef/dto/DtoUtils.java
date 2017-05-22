package naef.dto;

import tef.skelton.dto.EntityDto;

public class DtoUtils {

    private DtoUtils() {
    }

    public static boolean isSameEntity(EntityDto o1, EntityDto o2) {
        return isSameEntity(o1.getOid(), o2.getOid());
    }

    public static boolean isSameEntity(EntityDto.Oid o1, EntityDto.Oid o2) {
        return o1.equals(o2);
    }
}
