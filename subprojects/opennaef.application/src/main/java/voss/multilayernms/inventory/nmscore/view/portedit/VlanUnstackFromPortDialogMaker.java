package voss.multilayernms.inventory.nmscore.view.portedit;

import jp.iiga.nmt.core.model.portedit.HardPort;
import jp.iiga.nmt.core.model.portedit.LagPort;
import jp.iiga.nmt.core.model.portedit.PortEditModel;
import naef.dto.PortDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.renderer.VlanRenderer;
import voss.nms.inventory.constants.PortType;

public class VlanUnstackFromPortDialogMaker extends PortEditDialogMaker {
    PortDto port;

    public VlanUnstackFromPortDialogMaker(String inventoryId, PortDto port) {
        this.inventoryId = inventoryId;
        this.port = port;
    }

    @Override
    public PortEditModel makeDialog() throws ExternalServiceException {
        if (port instanceof EthPortDto) {
            HardPort model = new HardPort();
            model.setInventoryId(inventoryId);
            model.setVersion(DtoUtil.getMvoVersionString(port));
            model.setMvoId(DtoUtil.getMvoId(port).toString());
            model.setIfName(port.getIfname());
            model.setIfType(PortType.getByType(port.getObjectTypeName()).toString());
            model.setVlanUnstackFlag(true);
            model.setAllVlanPoolName(VlanRenderer.getVlanIdPoolsName());
            return model;
        } else if (port instanceof EthLagIfDto) {
            LagPort model = new LagPort();
            model.setInventoryId(inventoryId);
            model.setVersion(DtoUtil.getMvoVersionString(port));
            model.setMvoId(DtoUtil.getMvoId(port).toString());
            model.setIfName(port.getIfname());
            model.setIfType(PortType.getByType(port.getObjectTypeName()).toString());
            model.setVlanUnstackFlag(true);
            return model;
        }
        return null;
    }
}