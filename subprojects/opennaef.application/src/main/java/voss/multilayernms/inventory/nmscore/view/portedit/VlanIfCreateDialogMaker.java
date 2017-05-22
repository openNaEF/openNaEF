package voss.multilayernms.inventory.nmscore.view.portedit;

import jp.iiga.nmt.core.model.portedit.HardPort;
import jp.iiga.nmt.core.model.portedit.PortEditModel;
import naef.dto.HardPortDto;
import voss.core.server.util.DtoUtil;
import voss.nms.inventory.constants.PortType;

public class VlanIfCreateDialogMaker extends PortEditDialogMaker {


    HardPortDto port;

    public VlanIfCreateDialogMaker(String inventoryId, HardPortDto port) {
        this.inventoryId = inventoryId;
        this.port = port;
    }

    @Override
    public PortEditModel makeDialog() {
        HardPort model = new HardPort();

        model.setInventoryId(inventoryId);
        model.setVersion(DtoUtil.getMvoVersionString(port));
        model.setMvoId(DtoUtil.getMvoId(port).toString());
        model.setIfName(port.getIfname());
        model.setIfType(PortType.getByType(port.getObjectTypeName()).toString());

        return model;
    }

}