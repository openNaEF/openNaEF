package pasaran.util;

import tef.MVO;
import tef.skelton.dto.EntityDto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class MvoUtil {
    private MvoUtil() { }

    public static String toMvoId(MVO mvo) {
        return mvo.getMvoId().toString();
    }
    public static String toMvoId(EntityDto dto) {
        return dto.getOid().toString();
    }

    private static String toMvoId(EntityDto.Desc<? extends EntityDto> desc) {
        return desc.oid().toString();
    }

    private static <S extends EntityDto> List<String> toMvoIds(Collection<EntityDto.Desc<S>> descs) {
        return descs.stream().map(MvoUtil::toMvoId).filter(mvoId -> mvoId != null).collect(Collectors.toList());
    }

    // dtoの実体ではなく、MvoIdだけがほしい場合に使う
    public static <S extends EntityDto, T extends EntityDto> String getRefId(T dto, EntityDto.SingleRefAttr<S, T> refAttr) {
        EntityDto.Desc<S> ref = dto.get(refAttr);
        if (ref == null) {
            return null;
        }
        return toMvoId(ref);
    }

    public static <S extends EntityDto, T extends EntityDto> List<String> getRefIds(T dto, EntityDto.ListRefAttr<S, T> refAttr) {
        List<EntityDto.Desc<S>> refs = dto.get(refAttr);
        if (refs == null) {
            return Collections.emptyList();
        }
        return toMvoIds(refs);
    }

    public static <S extends EntityDto, T extends EntityDto> List<String> getRefIds(T dto, EntityDto.SetRefAttr<S, T> refAttr) {
        Set<EntityDto.Desc<S>> refs = dto.get(refAttr);
        if (refs == null) {
            return Collections.emptyList();
        }
        return toMvoIds(refs);
    }
}
