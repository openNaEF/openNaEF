package voss.multilayernms.inventory.nmscore.view.datamodifier;

import jp.iiga.nmt.core.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.nmscore.inventory.constants.INVENTORY_FIELD_NAME;
import voss.multilayernms.inventory.nmscore.model.converter.DisplayNameConverter;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class FacilityStatusUpdater extends Modifier {

    private final Logger log = LoggerFactory.getLogger(FacilityStatusUpdater.class);

    private final DisplayNameConverter converter;

    public FacilityStatusUpdater(Collection<? extends IModel> targets, String userName, DisplayNameConverter converter) {
        super(targets, userName);
        this.converter = converter;
    }

    protected DisplayNameConverter getConverter() {
        return converter;
    }

    public abstract List<? extends IModel> update() throws AuthenticationException, RemoteException, IOException,
            InventoryException, ExternalServiceException, InstantiationException, IllegalAccessException, NotBoundException;

    protected Map<String, FacilityStatus> getNewStatus(
            Collection<? extends IModel> targets) throws AuthenticationException, RemoteException, IOException, InventoryException {

        Map<String, FacilityStatus> newStatus = new HashMap<String, FacilityStatus>();

        for (IModel target : targets) {
            String inventoryId = target.getId();
            String status = (String) target.getMetaData().getPropertyValue(
                    getConverter().getDisplayNames().getProperty(INVENTORY_FIELD_NAME.FACILITY_STATUS));
            if (status.isEmpty()) {
                throw new IllegalArgumentException("Unknown Facility Status: [" + inventoryId + "]");
            }
            FacilityStatus fs = FacilityStatus.getByDisplayString(status);
            log.debug("id[" + inventoryId + "]: new -> [" + fs + "]");
            newStatus.put(inventoryId, fs);
        }

        return newStatus;
    }
}