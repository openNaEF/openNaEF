package voss.nms.inventory.trigger;

import naef.dto.NaefDto;
import tef.skelton.dto.DtoChanges;
import voss.core.server.util.Util;

import java.util.*;

public class MappedDtoChangesFilter implements DtoChangesFilter {
    private final Map<Class<? extends NaefDto>, Set<String>> filterMap
            = new HashMap<Class<? extends NaefDto>, Set<String>>();

    public MappedDtoChangesFilter() {
    }

    public void addMatchingFilter(Class<? extends NaefDto> _class, String attrName) {
        Set<String> attrNames = Util.getOrCreateSet(this.filterMap, _class);
        attrNames.add(attrName);
    }

    @Override
    public List<NaefDto> getMatchedResult(Collection<?> newObjects, Collection<?> changedObjects, DtoChanges changes) {
        List<NaefDto> result = new ArrayList<NaefDto>();
        for (Object obj : newObjects) {
            NaefDto dto = toTargetClass(obj);
            if (dto != null) {
                result.add(dto);
            }
        }
        for (Object obj : changedObjects) {
            NaefDto dto = toTargetClass(obj);
            if (dto != null && hasChangedAttribute(dto, changes)) {
                result.add(dto);
            }
        }
        return result;
    }

    private NaefDto toTargetClass(Object obj) {
        Set<Class<? extends NaefDto>> classes = this.filterMap.keySet();
        for (Class<? extends NaefDto> _class : classes) {
            if (_class.isInstance(obj)) {
                return _class.cast(obj);
            }
        }
        return null;
    }

    private boolean hasChangedAttribute(NaefDto dto, DtoChanges changes) {
        Set<Class<? extends NaefDto>> classes = this.filterMap.keySet();
        for (Class<? extends NaefDto> _class : classes) {
            if (!_class.isInstance(dto)) {
                continue;
            }
            Set<String> attrs = this.filterMap.get(_class);
            Set<String> changedAttrs = changes.getChangedAttributeNames(dto);
            for (String attr : attrs) {
                if (changedAttrs.contains(attr)) {
                    return true;
                }
            }
        }
        return false;
    }
}