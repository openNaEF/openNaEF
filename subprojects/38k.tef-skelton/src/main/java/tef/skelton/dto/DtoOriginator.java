package tef.skelton.dto;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * DB との対話を提供するインターフェースです. DB の実装ごとにサブタイプを定義します.
 * <p>
 * {@link EntityDto} が保持する他のオブジェクトへの参照は, 通常は DTO (実体) の形で直接保持する
 * のではなく, {@link EntityDto.Desc} (ポインタ) の形で保持します.
 * {@link EntityDto#toDto(EntityDto.Desc)} などではそれを必要に応じて DTO に実体化しますが, 
 * その時に DtoOriginator との対話を行います.
 */
public interface DtoOriginator extends Serializable {

    public EntityDto getDto(EntityDto.Oid oid);
    public <T extends EntityDto> T getDto(EntityDto.Desc<T> desc);
    public <T extends EntityDto> List<T> getDtosList(List<? extends EntityDto.Desc<T>> descs);
    public <T extends EntityDto> Set<T> getDtosSet(Set<? extends EntityDto.Desc<T>> descs);
    public Object getAttributeValue(EntityDto.Desc<?> desc, String attrName);
}
