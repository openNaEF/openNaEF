package voss.nms.inventory.trigger;

import naef.dto.NaefDto;
import tef.skelton.dto.DtoChanges;

import java.util.Collection;
import java.util.List;

public interface DtoChangesFilter {
    List<NaefDto> getMatchedResult(Collection<?> newObjects, Collection<?> changedObjects, DtoChanges changes);
}