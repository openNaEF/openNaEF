package voss.multilayernms.inventory.nmscore.portedit.delete;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;

public interface IPortDelete {
    Logger log = LoggerFactory.getLogger(IPortDelete.class);

    void delete() throws RuntimeException, IOException, InventoryException, ExternalServiceException;
}