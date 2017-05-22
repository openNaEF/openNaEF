package voss.multilayernms.inventory.nmscore.view.portedit;

import jp.iiga.nmt.core.model.portedit.Vlan;
import jp.iiga.nmt.core.model.portedit.VlanEditModel;
import naef.dto.vlan.VlanDto;
import net.phalanx.core.expressions.ObjectFilterQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.VlanRenderer;

import java.io.IOException;
import java.util.List;

public class VlanPropertyEditDialogMaker {
    static Logger log = LoggerFactory.getLogger(VlanPropertyEditDialogMaker.class);

    private String inventoryId;
    private VlanDto vlan;

    private VlanPropertyEditDialogMaker(String inventoryId, VlanDto vlan) {
        this.inventoryId = inventoryId;
        this.vlan = vlan;
    }

    public static VlanPropertyEditDialogMaker getDialogMaker(ObjectFilterQuery query) throws IOException, InventoryException, ExternalServiceException {

        if (!query.containsKey("ID")) {
            throw new IllegalArgumentException("Target is unknown.");
        }

        String inventoryId = getInventoryIdFromQuery(query);
        inventoryId = inventoryId.replace("\\", "");
        String mvoId = getMvoIdFromQuery(query);
        log.debug(inventoryId);
        VlanDto vlan = MplsNmsInventoryConnector.getInstance().getMvoDto(mvoId, VlanDto.class);

        return new VlanPropertyEditDialogMaker(inventoryId, vlan);
    }

    public VlanEditModel makeDialog() throws ExternalServiceException {
        Vlan model = new Vlan();

        model.setVersion(DtoUtil.getMvoVersionString(vlan));
        model.setMvoId(DtoUtil.getMvoId(vlan).toString());
        model.setInventoryId(inventoryId);
        model.setVlanId(vlan.getVlanId());
        model.setVlanPoolName(vlan.getIdPool().getName());
        model.setNotices(VlanRenderer.getNotice(vlan));
        model.setPurpose(VlanRenderer.getPurpose(vlan));
        model.setUser(VlanRenderer.getUser(vlan));
        model.setAreaCode(VlanRenderer.getAreaCode(vlan));
        model.setUserCode(VlanRenderer.getUserCode(vlan));
        model.setVlanPoolName(VlanRenderer.getVlanIdPoolName(vlan));
        return model;
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