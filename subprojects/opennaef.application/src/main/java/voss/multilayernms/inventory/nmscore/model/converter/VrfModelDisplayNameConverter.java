package voss.multilayernms.inventory.nmscore.model.converter;

import jp.iiga.nmt.core.model.IModel;
import net.phalanx.core.models.TableInput;
import net.phalanx.core.models.TableModifyContext;
import net.phalanx.core.models.Vrf;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.config.NmsCoreVrfConfiguration;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.inventory.constants.INVENTORY_FIELD_NAME;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;


public class VrfModelDisplayNameConverter extends DisplayNameConverter {

    public VrfModelDisplayNameConverter() throws IOException {
        super(Vrf.class, NmsCoreVrfConfiguration.getInstance());
    }

    @Override
    public TableInput convertList(List<? extends IModel> objects) throws RemoteException, InstantiationException, IllegalAccessException, IOException, ExternalServiceException, InventoryException {
        TableInput table = super.convertList(objects);

        TableModifyContext context = new TableModifyContext();
        context.setCellEditorName("org.eclipse.jface.viewers.ComboBoxViewerCellEditor");
        context.setSelections(MplsNmsInventoryConnector.getInstance().getConstants(INVENTORY_FIELD_NAME.FACILITY_STATUS));
        table.putModifyContext(getDisplayNames().getProperty(INVENTORY_FIELD_NAME.FACILITY_STATUS), context);

        return table;
    }

}