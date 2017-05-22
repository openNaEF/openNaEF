package voss.multilayernms.inventory.nmscore.model.diagrambuilder;

import jp.iiga.nmt.core.model.Device;
import jp.iiga.nmt.core.model.Diagram;
import jp.iiga.nmt.core.model.IDiagram;
import jp.iiga.nmt.core.model.PhysicalEthernetPort;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.ip.IpSubnetDto;
import net.phalanx.core.models.IPropertyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.nmscore.inventory.InventoryIdUtil;
import voss.multilayernms.inventory.nmscore.inventory.accessor.LinkHandler;
import voss.multilayernms.inventory.nmscore.inventory.constants.OPERATION_STATUS;
import voss.multilayernms.inventory.nmscore.model.creator.LinkModelCreator;
import voss.multilayernms.inventory.renderer.LinkRenderer;
import voss.multilayernms.inventory.renderer.PortRenderer;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class LinkDiagramBuilder extends DiagramBuilder {

    private static final Logger log = LoggerFactory.getLogger(LinkDiagramBuilder.class);

    public IDiagram build(String inventoryId) throws RemoteException, ExternalServiceException, IOException, AuthenticationException, NotBoundException {
        IDiagram diagram = new Diagram();

        IpSubnetDto link = LinkHandler.getLinkDto(inventoryId);

        List<String> iRefs = new ArrayList<String>();
        iRefs.add(inventoryId);
        diagram.setPropertyValue(IPropertyConstants.PROPERTY_INVENTORY_REFS, iRefs);
        diagram.setPropertyValue(IPropertyConstants.PROPERTY_DIAGRAM_SORUCE, LinkModelCreator.createModel(link, inventoryId));
        diagram.setText(LinkRenderer.getName(link));

        PortDto port1 = LinkRenderer.getPort1(link);
        PortDto port2 = LinkRenderer.getPort2(link);

        NodeDto node1 = port1.getNode();
        Device device1 = setNodeModel(diagram, node1);
        NodeDto node2 = port2.getNode();
        Device device2 = setNodeModel(diagram, node2);

        log.debug("port1 instance:" + port1.getClass().getName());
        log.debug("port2 instance:" + port2.getClass().getName());
        if (port1 instanceof EthLagIfDto) {
            if (!(port2 instanceof EthLagIfDto)) {
                throw new IllegalStateException("Link[" + LinkRenderer.getName(link) + "]'s" + "port[" + PortRenderer.getIfName(port1) + "] is LAG."
                        + "But its opposite port[" + PortRenderer.getIfName(port2) + "] is not LAG.");
            }

            EthLagIfDto lag1 = (EthLagIfDto) port1;
            EthLagIfDto lag2 = (EthLagIfDto) port2;

            if (lag1.getBundlePorts().isEmpty() || lag2.getBundlePorts().isEmpty()) {
                setPortLinkPortModel(diagram, device1, port1, device2, port2, link);
            } else {
                for (EthPortDto member1 : lag1.getBundlePorts()) {
                    log.debug("lag1 bundled: " + InventoryIdUtil.getInventoryId(member1));
                }
                for (EthPortDto member2 : lag2.getBundlePorts()) {
                    log.debug("lag2 bundled: " + InventoryIdUtil.getInventoryId(member2));
                }
            }

        } else {
            setPortLinkPortModel(diagram, device1, port1, device2, port2, link);
        }

        return diagram;
    }

    private void setPortLinkPortModel(IDiagram diagram, Device device1, PortDto port1,
                                      Device device2, PortDto port2, IpSubnetDto link) throws IOException {
        PhysicalEthernetPort pe1 = setPortModel(device1, port1);
        PhysicalEthernetPort pe2 = setPortModel(device2, port2);
        String status = LinkRenderer.getOperStatus(link);
        setLinkModel(diagram, link, pe1, pe2,
                true,
                (status == null ? false : status.equals(OPERATION_STATUS.UP)));
    }

}