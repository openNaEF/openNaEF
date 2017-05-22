package voss.multilayernms.inventory.nmscore.view.portedit;

import jp.iiga.nmt.core.model.portedit.HardPort;
import jp.iiga.nmt.core.model.portedit.PortEditModel;
import naef.dto.*;
import naef.dto.atm.AtmPvpIfDto;
import naef.dto.ip.IpIfDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vpls.VplsIfDto;
import naef.dto.vrf.VrfIfDto;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.constraints.PortEditConstraints;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.renderer.VlanRenderer;
import voss.nms.inventory.constants.PortType;
import voss.nms.inventory.util.NodeUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class HardPortEditDialogMaker extends PortEditDialogMaker {

    HardPortDto port;

    public HardPortEditDialogMaker(String inventoryId, HardPortDto port) {
        this.inventoryId = inventoryId;
        this.port = port;
    }

    @Override
    public PortEditModel makeDialog() throws InventoryException, IOException, ExternalServiceException {
        HardPort model = new HardPort();

        model.setInventoryId(inventoryId);
        model.setVersion(DtoUtil.getMvoVersionString(port));
        model.setMvoId(DtoUtil.getMvoId(port).toString());
        model.setIfName(port.getIfname());
        model.setIfType(PortType.getByType(port.getObjectTypeName()).toString());
        model.setNodeType(NodeRenderer.getNodeType(port.getNode()));
        model.setIpAddress(convertNull2ZeroString(PortRenderer.getIpAddress(port)));
        model.setSubnetLength(convertNull2ZeroString(PortRenderer.getSubnetMask(port)));
        model.setAdminStatus(PortRenderer.getAdminStatus(port));
        model.setDescription(PortRenderer.getDescription(port));
        model.setBandwidth(PortRenderer.getBandwidthAsLong(port));

        if (PortRenderer.getPortMode(port) != null && PortRenderer.getSwitchPortMode(port) != null) {
            model.setPortMode(PortRenderer.getPortMode(port));
            model.setSwitchPortMode(PortRenderer.getSwitchPortMode(port));
        }
        model.setStormcontrolBroadcastLevel(PortRenderer.getStormControlBroadcastLevel(port));
        model.setStormcontrolActions(PortRenderer.getStormControlActions(port));
        model.setAutoNegotiation(convertNull2ZeroString(PortRenderer.getAutoNegotiation(port)));
        model.setAdministrativeDuplex(convertNull2ZeroString(PortRenderer.getAdministrativeDuplex(port)));
        model.setAdministrativeSpeed(convertNull2ZeroString(PortRenderer.getAdministrativeSpeedAsString(port)));
        model.setNotices(convertNull2ZeroString(PortRenderer.getNotices(port)));
        model.setUser(convertNull2ZeroString(PortRenderer.getEndUser(port)));

        model.setConnectNodeName(PortRenderer.getP2POppositeNodeName(port));
        model.setConnectPortName(PortRenderer.getP2POppositePortName(port));

        model.setIsPortSetIpEnabled(PortEditConstraints.isPortSetIpEnabled(port));

        try {
            model.setConnectPortList(getConnectPortList());
            model.setSubnetList(getSubnetList());
            model.setAllVlanPoolName(VlanRenderer.getVlanIdPoolsName());
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

    public Map<String, Map<String, String>> getConnectPortList() throws IOException, ExternalServiceException {
        long start = System.currentTimeMillis();
        Map<String, Map<String, String>> list = new TreeMap<String, Map<String, String>>();

        for (NodeDto node : MplsNmsInventoryConnector.getInstance().getActiveNodes()) {
            String nodeName = node.getName();
            list.put(nodeName, new HashMap<String, String>());
            for (PortDto port : node.getPorts()) {
                boolean isConnect = true;
                if (isTargetType(port)) {
                    for (NetworkDto link : port.getNetworks()) {
                        if (link instanceof LinkDto) {
                            isConnect = false;
                            break;
                        }
                    }
                    if (isConnect) {
                        list.get(nodeName).put(PortRenderer.getIfName(port), DtoUtil.getMvoId(port).toString());
                    }
                }
            }
        }
        long end = System.currentTimeMillis();

        log.debug("getConnectPortList time: " + (end - start) + "ms.");
        return list;
    }

    private static boolean isTargetType(PortDto port) {
        if (port instanceof IpIfDto) {
            if (NodeUtil.isLoopback((IpIfDto) port)) {
                return true;
            } else if (NodeUtil.isIndependentIp((IpIfDto) port)) {
                return true;
            } else {
                return false;
            }
        } else if (port instanceof AtmPvpIfDto) {
            return false;
        } else if (port instanceof VplsIfDto) {
            return false;
        } else if (port instanceof VrfIfDto) {
            return false;
        } else if (port instanceof InterconnectionIfDto) {
            return false;
        } else if (port instanceof VlanIfDto) {
            return false;
        } else {
            return true;
        }
    }
}