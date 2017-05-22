package tef.skelton.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntityDtoUtils {

    public static boolean isNullOrEntityDto(Object o) {
        return o == null || o instanceof EntityDto;
    }

    public static EntityDto.Oid getOid(EntityDto dto) {
        return dto == null ? null : dto.getOid();
    }

    public static List<EntityDto.Oid> getOids(Collection<?> c) {
        List<EntityDto.Oid> oids = new ArrayList<EntityDto.Oid>();
        for (Object o : c) {
            if (isNullOrEntityDto(o)) {
                oids.add(getOid((EntityDto) o));
            }
        }
        return oids;
    }

    public static Set<EntityDto.Oid> getOidsSet(Collection<?> c) {
        return new HashSet<EntityDto.Oid>(getOids(c));
    }
}
