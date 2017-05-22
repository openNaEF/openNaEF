package voss.multilayernms.inventory.nmscore.view.portedit;

import jp.iiga.nmt.core.model.portedit.PortEditModel;
import jp.iiga.nmt.core.model.portedit.VlanPort;
import naef.dto.vlan.VlanIfDto;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.nmscore.constraints.PortEditConstraints;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.renderer.VlanRenderer;

import java.io.IOException;

public class VlanIfEditDialogMaker extends PortEditDialogMaker {
    VlanIfDto port;

    public VlanIfEditDialogMaker(String inventoryId, VlanIfDto port) {
        this.inventoryId = inventoryId;
        this.port = port;
    }

    @Override
    public PortEditModel makeDialog() throws InventoryException, IOException, ExternalServiceException {
        VlanPort model = new VlanPort();
        model.setAllVlanPoolName(VlanRenderer.getVlanIdPoolsName());
        try {
            if (!port.isAlias()) {
                model.setInventoryId(inventoryId);
                model.setVersion(DtoUtil.getMvoVersionString(port));
                model.setMvoId(DtoUtil.getMvoId(port).toString());
                model.setVlanId(port.getVlanId());
                model.setVlanPoolName(VlanRenderer.getVlanIdPoolName(port));
                model.setParentIf(VlanRenderer.getParentIfName(port));
                model.setIfName(port.getIfname());
                model.setNodeType(NodeRenderer.getNodeType(port.getNode()));
                model.setConfigName(PortRenderer.getConfigName(port));
                model.setisVlanIfNameEditable(PortEditConstraints.isPortSetConfigNameEnabled(port));
                model.setConnectedTaggedPort(VlanRenderer.getConnectedTaggedPort(port));
                model.setConnectedUnTaggedPort(VlanRenderer.getConnectedUnTaggedPort(port));
                model.setSviPort(PortRenderer.getSviEnabled(port));
                model.setAdminStatus(PortRenderer.getAdminStatus(port));
                model.setIpAddress(convertNull2ZeroString(PortRenderer.getIpAddress(port)));
                model.setSubnetMask(convertNull2ZeroString(PortRenderer.getSubnetMask(port)));
                model.setBandwidth(PortRenderer.getBandwidthAsLong(port));
                model.setDescription(PortRenderer.getDescription(port));
                model.setNotices(PortRenderer.getNotices(port));
                model.setPurpose(PortRenderer.getPurpose(port));
                model.setUser(PortRenderer.getEndUser(port));
                model.setIsPortSetIpEnabled(PortEditConstraints.isPortSetIpEnabled(port));
                model.setSubnetList(getSubnetList());
            } else {
                port = (VlanIfDto) port.getAliasSource();
                model.setInventoryId(inventoryId);
                model.setVersion(DtoUtil.getMvoVersionString(port));
                model.setMvoId(DtoUtil.getMvoId(port).toString());
                model.setVlanId(port.getVlanId());
                model.setVlanPoolName(VlanRenderer.getVlanIdPoolName(port));
                model.setParentIf(VlanRenderer.getParentIfName(port));
                model.setIfName(port.getIfname());
                model.setConfigName(PortRenderer.getConfigName(port));
                model.setisVlanIfNameEditable(PortEditConstraints.isPortSetConfigNameEnabled(port));
                model.setConnectedTaggedPort(VlanRenderer.getConnectedTaggedPort(port));
                model.setConnectedUnTaggedPort(VlanRenderer.getConnectedUnTaggedPort(port));
                model.setSviPort(PortRenderer.getSviEnabled(port));
                model.setAdminStatus(PortRenderer.getAdminStatus(port));
                model.setIpAddress(convertNull2ZeroString(PortRenderer.getIpAddress(port)));
                model.setSubnetMask(convertNull2ZeroString(PortRenderer.getSubnetMask(port)));
                model.setBandwidth(PortRenderer.getBandwidthAsLong(port));
                model.setDescription(PortRenderer.getDescription(port));
                model.setNotices(PortRenderer.getNotices(port));
                model.setPurpose(PortRenderer.getPurpose(port));
                model.setUser(PortRenderer.getEndUser(port));
                model.setIsPortSetIpEnabled(PortEditConstraints.isPortSetIpEnabled(port));
                model.setSubnetList(getSubnetList());
            }


        } catch (IOException e) {
            log.debug("IOException", e);
            throw new IOException(e);
        } catch (ExternalServiceException e) {
            log.debug("ExternalServiceException", e);
            throw new ExternalServiceException(e);
        } catch (RuntimeException e) {
            log.debug("RuntimeException", e);
            throw new RuntimeException(e);
        }

        return model;
    }

}