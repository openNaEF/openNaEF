package voss.multilayernms.inventory.nmscore.portedit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;
import java.text.ParseException;


interface IPortEditUpdate {
    Logger log = LoggerFactory.getLogger(IPortEditUpdate.class);

    void update() throws RuntimeException, IOException, ExternalServiceException, ParseException, InventoryException;
}