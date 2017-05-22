package voss.multilayernms.inventory.nmscore.view.portedit;

import jp.iiga.nmt.core.model.portedit.LagPort;
import jp.iiga.nmt.core.model.portedit.PortEditModel;
import naef.dto.DtoUtils;
import naef.dto.NetworkDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.atm.AtmApsIfDto;
import naef.dto.atm.AtmPortDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdCalculator;
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

public class LagPortEditDialogMaker extends PortEditDialogMaker {

    PortDto port;

    public LagPortEditDialogMaker(String inventoryId, PortDto port) {
        this.inventoryId = inventoryId;
        this.port = port;
    }

    @Override
    public PortEditModel makeDialog() throws InventoryException, ExternalServiceException, IOException {
        LagPort model = new LagPort();
        NodeDto node = port.getNode();

        model.setInventoryId(inventoryId);
        model.setVersion(DtoUtil.getMvoVersionString(port));
        model.setMvoId(DtoUtil.getMvoId(port).toString());
        model.setIfName(port.getIfname());
        model.setIfType(PortType.getByType(port.getObjectTypeName()).toString());
        model.setNodeName(node.getName());
        model.setNodeType(NodeRenderer.getNodeType(port.getNode()));
        log.debug(DtoUtil.getMvoId(node).toString());
        model.setNodeMvoId(DtoUtil.getMvoId(node).toString());
        model.setMemberPorts(getMemberPorts());
        model.setPortList(getPortList());

        model.setAdminStatus(PortRenderer.getAdminStatus(port));
        model.setIpAddress(convertNull2ZeroString(PortRenderer.getIpAddress(port)));
        model.setSubnetMask(convertNull2ZeroString(PortRenderer.getSubnetMask(port)));
        model.setBandwidth(PortRenderer.getBandwidthAsLong(port));
        model.setDescription(PortRenderer.getDescription(port));
        model.setNotices(PortRenderer.getNotices(port));
        model.setPurpose(PortRenderer.getPurpose(port));
        model.setUser(PortRenderer.getEndUser(port));

        model.setConnectNodeName(PortRenderer.getP2POppositeNodeName(port));
        model.setConnectPortName(PortRenderer.getP2POppositePortName(port));
        if (PortRenderer.getPortMode(port) != null && PortRenderer.getSwitchPortMode(port) != null) {
            model.setPortMode(PortRenderer.getPortMode(port));
            model.setSwitchPortMode(PortRenderer.getSwitchPortMode(port));
        }
        model.setStormcontrolBroadcastLevel(PortRenderer.getStormControlBroadcastLevel(port));
        model.setStormcontrolActions(PortRenderer.getStormControlActions(port));
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

    public Map<String, Map<String, String>> getPortList() {
        Map<String, Map<String, String>> memberPorts = new HashMap<String, Map<String, String>>();

        PortType type = null;
        if (port instanceof EthLagIfDto) {
            type = PortType.ETHERNET;
        } else if (port instanceof AtmApsIfDto) {
            type = PortType.ATM;
        }

        for (PortDto port : this.port.getNode().getPorts()) {
            if (PortType.getByType(port.getObjectTypeName()) == type && port.getContainer() == null) {
                memberPorts.put(port.getIfname(), new HashMap<String, String>());
                memberPorts.get(port.getIfname()).put(InventoryIdCalculator.getId(port), port.getAbsoluteName());
            }
        }
        return memberPorts;
    }

    public Map<String, Map<String, String>> getMemberPorts() {
        Map<String, Map<String, String>> memberPorts = new HashMap<String, Map<String, String>>();

        if (port instanceof EthLagIfDto) {
            for (EthPortDto item : ((EthLagIfDto) port).getBundlePorts()) {
                memberPorts.put(item.getIfname(), new HashMap<String, String>());
                memberPorts.get(item.getIfname()).put(InventoryIdCalculator.getId(item), item.getAbsoluteName());
            }
        } else if (port instanceof AtmApsIfDto) {
            for (AtmPortDto item : ((AtmApsIfDto) port).getAtmPorts()) {
                memberPorts.put(item.getIfname(), new HashMap<String, String>());
                memberPorts.get(item.getIfname()).put(InventoryIdCalculator.getId(item), item.getAbsoluteName());
            }
        }

        return memberPorts;
    }

    public Map<String, Map<String, String>> getConnectPortList() throws IOException, ExternalServiceException {
        Map<String, Map<String, String>> list = new TreeMap<String, Map<String, String>>();

        for (NodeDto node : MplsNmsInventoryConnector.getInstance().getActiveNodes()) {
            String nodeName = node.getName();
            list.put(nodeName, new HashMap<String, String>());
            for (PortDto port : node.getPorts()) {
                if (isTargetType(port)) {
                    list.get(nodeName).put(PortRenderer.getIfName(port), DtoUtil.getMvoId(port).toString());
                }
            }
        }

        PortDto connectedPort = NodeUtil.getLayer2Neighbor(port);
        if (connectedPort != null) {
            String nodeName = connectedPort.getNode().getName();
            if (!list.containsKey(nodeName)) {
                list.put(nodeName, new HashMap<String, String>());
            }
            list.get(nodeName).put(PortRenderer.getIfName(connectedPort), DtoUtil.getMvoId(connectedPort).toString());
        }

        return list;
    }

    private boolean isTargetType(PortDto port) {
        if (port == null) return false;
        if (DtoUtils.isSameEntity(port, this.port)) return false;

        if (port instanceof EthLagIfDto || port instanceof AtmApsIfDto) {
            for (NetworkDto link : port.getNetworks()) {
                if (ATTR.TYPE_LAG_LINK.equals(link.getObjectTypeName())) {
                    return false;
                }
            }
        } else {
            return false;
        }

        return true;
    }
}