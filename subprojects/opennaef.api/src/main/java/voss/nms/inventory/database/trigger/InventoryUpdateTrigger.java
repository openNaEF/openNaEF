package voss.nms.inventory.database.trigger;

import tef.TransactionId;
import tef.skelton.dto.DtoChanges;
import tef.skelton.dto.EntityDto;
import voss.nms.inventory.trigger.TriggerService;

import java.util.Set;

public class InventoryUpdateTrigger implements TriggerService {
    public static final String SERVICE_NAME = "InventoryUpdateTrigger";

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void update(TransactionId id, Set<EntityDto> newObjects, Set<EntityDto> changedObjects, DtoChanges changes) {

    }
}