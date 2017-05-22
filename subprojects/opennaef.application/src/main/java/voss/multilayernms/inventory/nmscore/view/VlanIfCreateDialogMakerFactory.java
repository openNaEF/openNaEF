package voss.multilayernms.inventory.nmscore.view;

import jp.iiga.nmt.core.model.PhysicalEthernetPort;
import naef.dto.HardPortDto;
import naef.dto.PortDto;
import net.phalanx.core.expressions.ObjectFilterQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.view.portedit.VlanIfCreateDialogMaker;

import java.io.IOException;
import java.util.List;

public class VlanIfCreateDialogMakerFactory {

    private final static Logger log = LoggerFactory.getLogger(VlanIfCreateDialogMakerFactory.class);

    public static VlanIfCreateDialogMaker getDialogMaker(ObjectFilterQuery query) throws IOException, ExternalServiceException, InventoryException {

        if (!query.getTarget().getName().equals(PhysicalEthernetPort.class.getName())) {
            throw new IllegalArgumentException("Target is unknown.");
        }

        if (!query.containsKey("ID")) {
            throw new IllegalArgumentException("Target is unknown.");
        }

        String inventoryId = getInventoryIdFromQuery(query);
        inventoryId = inventoryId.replace("\\", "");
        log.debug(inventoryId);
        PortDto port = MplsNmsInventoryConnector.getInstance().getMvoDto(getMvoIdFromQuery(query), PortDto.class);

        if (port instanceof HardPortDto) {
            return new VlanIfCreateDialogMaker(inventoryId, (HardPortDto) port);
        } else {
            throw new IllegalStateException("unexpected type: " + port.getAbsoluteName());
        }
    }

    @SuppressWarnings("unchecked")
    public static String getInventoryIdFromQuery(ObjectFilterQuery query) {
        String inventoryId = null;

        Object target = query.get("ID").getPattern();
        if (target instanceof String) {
            inventoryId = (String) target;
        } else if (target instanceof List) {
            inventoryId = (String) ((List<String>) target).get(0);
        }

        log.debug("inventoryId[" + inventoryId + "]");
        return inventoryId;

    }

    @SuppressWarnings("unchecked")
    public static String getMvoIdFromQuery(ObjectFilterQuery query) {
        String mvoId = null;

        Object target = query.get("MVO_ID").getPattern();
        if (target instanceof String) {
            mvoId = (String) target;
        } else if (target instanceof List) {
            mvoId = (String) ((List<String>) target).get(0);
        }

        log.debug("mvoId[" + mvoId + "]");
        return mvoId;

    }

}