package voss.multilayernms.inventory.scheduler;

import voss.core.server.exception.ExternalServiceException;

import java.io.IOException;

public interface ScheduledLogic {
    void execute() throws ExternalServiceException, IOException;

    void interrupt();
}