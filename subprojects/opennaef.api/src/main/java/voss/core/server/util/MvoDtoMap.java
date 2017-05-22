package voss.core.server.util;

import naef.dto.NaefDto;
import tef.MVO;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MvoDtoMap {
    private final Map<MVO.MvoId, NaefDto> dtos = new HashMap<MVO.MvoId, NaefDto>();
    private final Map<MVO.MvoId, Object> objects = new HashMap<MVO.MvoId, Object>();

    public MvoDtoMap() {
    }

    public void put(NaefDto dto, Object object) {
        this.dtos.put(DtoUtil.getMvoId(dto), dto);
        this.objects.put(DtoUtil.getMvoId(dto), object);
    }

    public NaefDto getDto(MVO.MvoId id) {
        return this.dtos.get(id);
    }

    public Object getObject(NaefDto dto) {
        return this.objects.get(DtoUtil.getMvoId(dto));
    }

    public Set<Object> getObjects() {
        return new HashSet<Object>(this.objects.values());
    }

    public Set<NaefDto> getDtos() {
        return new HashSet<NaefDto>(dtos.values());
    }
}