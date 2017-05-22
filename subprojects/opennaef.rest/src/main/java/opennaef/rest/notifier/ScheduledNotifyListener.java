package opennaef.rest.notifier;

import tef.skelton.dto.DtoChanges;

/**
 * Transaction で指定した時間が来たら notify が実行される
 */
public interface ScheduledNotifyListener {
    void notify(DtoChanges dtoChanges);
}
