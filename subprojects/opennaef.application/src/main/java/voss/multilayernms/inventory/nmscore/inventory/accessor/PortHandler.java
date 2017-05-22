package voss.multilayernms.inventory.nmscore.inventory.accessor;

import jp.iiga.nmt.core.model.PhysicalEthernetPort;
import naef.dto.InterconnectionIfDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.atm.AtmPvpIfDto;
import naef.dto.ip.IpIfDto;
import naef.dto.vpls.VplsIfDto;
import naef.dto.vrf.VrfIfDto;
import net.phalanx.core.expressions.ObjectFilterQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdBuilder;
import voss.core.server.naming.inventory.InventoryIdDecoder;
import voss.multilayernms.inventory.builder.BestEffortBandwidthCommandBuilder;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.inventory.InventoryIdUtil;
import voss.multilayernms.inventory.nmscore.model.creator.PortModelCreator;
import voss.multilayernms.inventory.nmscore.rendering.PortRenderingUtil;
import voss.nms.inventory.util.NodeUtil;

import java.io.IOException;
import java.math.BigInteger;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PortHandler {

    private static final Logger log = LoggerFactory.getLogger(PortHandler.class);

    public static String parseNodeName(String inventoryId) {
        String result = null;
        try {
            String nodeName = InventoryIdDecoder.getNode(inventoryId);
            result = InventoryIdBuilder.getNodeID(nodeName);
        } catch (ParseException e) {
            log.debug(e.getMessage());
        }
        return result;
    }

    public static String parseIfName(String inventoryId) {
        String result = null;
        try {
            result = InventoryIdDecoder.getPortIfName(inventoryId);
        } catch (ParseException e) {
            log.debug(e.getMessage());

        }

        return result;
    }

    public static PortDto getPortDto(String inventoryId) throws RemoteException, ExternalServiceException, IOException {
        return MplsNmsInventoryConnector.getInstance().getPortByInventoryID(inventoryId);
    }

    public static List<PhysicalEthernetPort> get(String inventoryId) throws NotBoundException, AuthenticationException,
            IOException, InventoryException, ExternalServiceException {
        List<PhysicalEthernetPort> portList = new ArrayList<PhysicalEthernetPort>();
        for (PortDto port : getActivePorts()) {
            if (InventoryIdUtil.getInventoryId(port).equals(inventoryId)) {
                portList.add(PortModelCreator.createModel(port, InventoryIdUtil.getInventoryId(port)));
                break;
            }
        }
        return portList;
    }

    public static List<PhysicalEthernetPort> getListOwnedByNode(String nodeInventoryId) throws NotBoundException,
            AuthenticationException, IOException, InventoryException, ExternalServiceException {
        List<PhysicalEthernetPort> portList = new ArrayList<PhysicalEthernetPort>();
        try {
            String nodeID = InventoryIdDecoder.getNode(nodeInventoryId);
            appendPortList(portList, getTargetPorts(MplsNmsInventoryConnector.getInstance().getNodeDto(nodeID)));
            return portList;
        } catch (ParseException e) {
            throw new InventoryException("illegal inventory-id.", e);
        }
    }

    public static List<PhysicalEthernetPort> getList(ObjectFilterQuery query) throws NotBoundException,
            AuthenticationException, IOException, InventoryException, ExternalServiceException {
        List<PhysicalEthernetPort> portList = new ArrayList<PhysicalEthernetPort>();
        appendPortList(portList, filterPorts(query, getActivePorts()));
        return portList;
    }

    public static List<PortDto> getActivePorts() throws RemoteException, InventoryException, ExternalServiceException,
            IOException {
        List<PortDto> ports = new ArrayList<PortDto>();
        for (NodeDto node : MplsNmsInventoryConnector.getInstance().getActiveNodes()) {
            ports.addAll(getTargetPorts(node));
        }
        return ports;
    }

    private static List<PortDto> getTargetPorts(NodeDto node) {
        List<PortDto> ports = new ArrayList<PortDto>();
        for (PortDto port : node.getPorts()) {
            if (isTargetType(port)) {
                ports.add(port);
            }
        }
        return ports;
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
        } else {
            return true;
        }
    }

    private static void appendPortList(List<PhysicalEthernetPort> portList, List<PortDto> list) throws IOException {
        for (PortDto port : list) {
            portList.add(PortModelCreator.createModel(port, InventoryIdUtil.getInventoryId(port)));
        }
    }

    private static List<PortDto> filterPorts(ObjectFilterQuery query, List<PortDto> list) {
        List<PortDto> result = new ArrayList<PortDto>();

        for (PortDto port : list) {

            boolean unmatched = false;
            for (String field : query.keySet()) {
                String value = PortRenderingUtil.rendering(port, field, true);

                if (!query.get(field).matches(value)) {
                    unmatched = true;
                    break;
                }
            }

            if (!unmatched && !port.isAlias()) {
                result.add(port);
            }
        }
        return result;
    }

    public static void savePeekValueOfBestEffortBandWidth(Map<String, BigInteger> dataOnPort, String userName)
            throws IOException, InventoryException, ExternalServiceException {

        BestEffortBandwidthCommandBuilder builder = new BestEffortBandwidthCommandBuilder(userName);
        for (String inventoryId : dataOnPort.keySet()) {
            builder.addBestEffortValue(inventoryId, dataOnPort.get(inventoryId).longValue());
        }
        builder.buildCommand();

        for (String str : builder.getCommand().getRawCommands()) {
            log.debug("cmd:" + str);
        }

        ShellConnector.getInstance().execute(builder);
    }

}