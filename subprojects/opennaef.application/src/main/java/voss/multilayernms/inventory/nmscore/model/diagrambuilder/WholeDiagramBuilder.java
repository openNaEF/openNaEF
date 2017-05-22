package voss.multilayernms.inventory.nmscore.model.diagrambuilder;

import jp.co.iiga.phalanx.core.export.PhalanxConstants;
import jp.iiga.nmt.core.model.*;
import naef.dto.*;
import naef.dto.ip.IpSubnetDto;
import naef.mvo.Jack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.MVO;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.naming.inventory.InventoryIdBuilder;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.Util;
import voss.multilayernms.inventory.constants.ViewConstants;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.L2LinkRenderer;
import voss.multilayernms.inventory.renderer.LinkRenderer;
import voss.nms.inventory.util.NodeUtil;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class WholeDiagramBuilder extends DiagramBuilder {

    private static final Logger log = LoggerFactory.getLogger(LinkDiagramBuilder.class);

    public IDiagram build() throws RemoteException, ExternalServiceException, IOException, AuthenticationException, NotBoundException {
        MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
        Map<MVO.MvoId, Set<PhysicalEthernetPort>> linkAndPorts = new HashMap<MVO.MvoId, Set<PhysicalEthernetPort>>();
        Map<MVO.MvoId, NaefDto> dtoMap = new HashMap<MVO.MvoId, NaefDto>();

        IDiagram diagram = new Diagram();
        diagram.setText("Whole Physical Topology");
        for (NodeDto node : conn.getActiveNodes()) {
            Device device = setNodeModel(diagram, node);

            try {
                String ipAddress = (String) node.getValue("Management IP Address");
                String nodeName = node.getName();
                String inventoryId = InventoryIdBuilder.getNodeID(node.getName());

                if (ipAddress != null) {
                    String hyphenatedIpAddress = ipAddress.replace('.', '-');
                    String emsUrl = System.getProperty("vossnms.ems.url");
                    if (emsUrl != null && emsUrl.length() != 0) {
                        emsUrl = emsUrl.replaceAll("_IPADDRESS_", ipAddress);
                        emsUrl = emsUrl.replaceAll("_HYPHENATEDIPADDRESS_", hyphenatedIpAddress);
                        emsUrl = emsUrl.replaceAll("_NODENAME_", nodeName);
                        log.debug(PhalanxConstants.EMS_URL_PROPERTY_NAME + ": " + emsUrl);
                    }
                    device.setPropertyValue(PhalanxConstants.EMS_URL_PROPERTY_NAME, emsUrl);
                    device.setPropertyValue("MgmtIPAddr", ipAddress);
                }
                String constraint = DtoUtil.getStringOrNull(node, ViewConstants.ATTR_POSITION);
                if (constraint != null) {
                    device.setPropertyValue(ViewConstants.VIEW_POSITION, constraint);
                }

                String affectedUsersUrl = System.getProperty("vossnms.affectedUsers.url");
                if (affectedUsersUrl != null && affectedUsersUrl.length() != 0) {
                    affectedUsersUrl = affectedUsersUrl.replaceAll("_INVENTORYID_", inventoryId);
                    affectedUsersUrl = affectedUsersUrl.replaceAll("_NODENAME_", nodeName);
                    log.debug(PhalanxConstants.AFFECTED_USERS_URL_PROPERTY_NAME + ": " + affectedUsersUrl);
                }
                device.setPropertyValue(PhalanxConstants.AFFECTED_USERS_URL_PROPERTY_NAME, affectedUsersUrl);

                String webtelnetUrl = System.getProperty("vossnms.webtelnet.url");
                if (webtelnetUrl != null && webtelnetUrl.length() != 0) {
                    webtelnetUrl = webtelnetUrl.replaceAll("_IPADDRESS_", ipAddress);
                    webtelnetUrl = webtelnetUrl.replaceAll("_NODENAME_", nodeName);
                    log.debug(PhalanxConstants.WEBTELNET_URL_PROPERTY_NAME + ": " + webtelnetUrl);
                }
                device.setPropertyValue(PhalanxConstants.WEBTELNET_URL_PROPERTY_NAME, webtelnetUrl);

            } catch (Exception e) {
                e.printStackTrace();
            }

            for (JackDto jack : DtoUtil.getNaefDtoFacade(node).getSubElements(node, Jack.class, JackDto.class)) {
                PortDto port = jack.getPort();
                if (port == null || !HardPortDto.class.isInstance(port)) {
                    continue;
                }
                HardPortDto hardPort = (HardPortDto) port;
                PhysicalEthernetPort pe = setPortModel(device, hardPort);
                LinkDto link = NodeUtil.getLayer2Link(hardPort);
                if (link != null) {
                    MVO.MvoId id = DtoUtil.getMvoId(link);
                    dtoMap.put(id, link);
                    Set<PhysicalEthernetPort> ports = Util.getOrCreateSet(linkAndPorts, id);
                    ports.add(pe);
                    continue;
                }
                log.debug("- no link.");
            }
        }
        for (Map.Entry<MVO.MvoId, Set<PhysicalEthernetPort>> entry : linkAndPorts.entrySet()) {
            MVO.MvoId id = entry.getKey();
            NaefDto link = dtoMap.get(id);
            Set<PhysicalEthernetPort> ports = entry.getValue();
            if (ports.size() != 2) {
                log.warn("illegal number of member port: " + ports.size() + " ["
                        + link.getAbsoluteName() + " (" + DtoUtil.getMvoId(link).toString() + ")]");
                continue;
            }
            createLink(diagram, link, ports);
        }
        return diagram;
    }

    private PhysicalLink createLink(IDiagram diagram, NaefDto link, Set<PhysicalEthernetPort> ports)
            throws IOException {
        Iterator<PhysicalEthernetPort> it = ports.iterator();
        PhysicalEthernetPort pe1 = it.next();
        PhysicalEthernetPort pe2 = it.next();
        if (link instanceof LinkDto) {
            LinkDto l2Link = (LinkDto) link;
            String operStatus = L2LinkRenderer.getOperStatus(l2Link);
            boolean isUp = (operStatus != null && operStatus.toLowerCase().equals("up") ? true : false);
            return setLinkModel(diagram, l2Link, pe1, pe2, true, isUp, false);
        } else if (link instanceof IpSubnetDto) {
            IpSubnetDto subnet = (IpSubnetDto) link;
            String operStatus = LinkRenderer.getOperStatus(subnet);
            boolean isUp = (operStatus != null && operStatus.toLowerCase().equals("up") ? true : false);
            return setLinkModel(diagram, subnet, pe1, pe2, true, isUp, false);
        } else {
            throw new IllegalStateException();
        }
    }
}