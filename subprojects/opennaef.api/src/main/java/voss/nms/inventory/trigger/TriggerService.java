package voss.nms.inventory.trigger;

import tef.TransactionId;
import tef.skelton.dto.DtoChanges;
import tef.skelton.dto.EntityDto;

import java.util.Set;

public interface TriggerService {

    String getServiceName();

    void start();

    void stop();

    void update(TransactionId id, Set<EntityDto> newObjects, Set<EntityDto> changedObjects, DtoChanges changes);
}