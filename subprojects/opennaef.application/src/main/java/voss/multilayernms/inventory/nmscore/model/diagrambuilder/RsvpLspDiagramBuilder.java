package voss.multilayernms.inventory.nmscore.model.diagrambuilder;

import jp.iiga.nmt.core.model.Device;
import jp.iiga.nmt.core.model.Diagram;
import jp.iiga.nmt.core.model.IDiagram;
import jp.iiga.nmt.core.model.PhysicalEthernetPort;
import naef.dto.PathHopDto;
import naef.dto.PortDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import net.phalanx.core.models.IPropertyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.nmscore.inventory.InventoryIdUtil;
import voss.multilayernms.inventory.nmscore.inventory.accessor.RsvpLspHandler;
import voss.multilayernms.inventory.nmscore.inventory.constants.OPERATION_STATUS;
import voss.multilayernms.inventory.nmscore.model.creator.RsvpLspModelCreator;
import voss.multilayernms.inventory.renderer.RsvpLspRenderer;
import voss.nms.inventory.util.NodeUtil;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.*;

public class RsvpLspDiagramBuilder extends DiagramBuilder {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(RsvpLspDiagramBuilder.class);

    public IDiagram build(String inventoryId) throws RemoteException, InventoryException, ExternalServiceException, IOException, AuthenticationException, NotBoundException {
        IDiagram diagram = new Diagram();

        RsvpLspDto lsp = RsvpLspHandler.getRsvpLspDto(inventoryId);

        List<String> iRefs = new ArrayList<String>();
        iRefs.add(inventoryId);
        diagram.setPropertyValue(IPropertyConstants.PROPERTY_INVENTORY_REFS, iRefs);
        diagram.setPropertyValue(IPropertyConstants.PROPERTY_DIAGRAM_SORUCE, RsvpLspModelCreator.createModel(lsp, inventoryId));
        diagram.setText(InventoryIdUtil.unEscape(inventoryId));

        if (lsp.getHopSeries1() != null) {
            setModelsOnHopSeries(diagram, lsp.getHopSeries1().getHops(),
                    DtoUtil.isSameMvoEntity(lsp.getHopSeries1(), lsp.getActiveHopSeries()),
                    RsvpLspRenderer.getMainPathOperationStatus(lsp).equals(OPERATION_STATUS.UP),
                    true);
        }
        if (lsp.getHopSeries2() != null) {
            setModelsOnHopSeries(diagram, lsp.getHopSeries2().getHops(),
                    DtoUtil.isSameMvoEntity(lsp.getHopSeries2(), lsp.getActiveHopSeries()),
                    RsvpLspRenderer.getBackupPathOperationStatus(lsp).equals(OPERATION_STATUS.UP),
                    false);
        }

        sortDevicesOrder(diagram, lsp);

        return diagram;
    }

    private void setModelsOnHopSeries(IDiagram diagram, List<PathHopDto> hops,
                                      boolean isActive, boolean isUp, boolean isPrimary) throws IOException {

        for (PathHopDto hop : hops) {
            PortDto src = NodeUtil.getAssociatedPort(hop.getSrcPort());
            PortDto dst = NodeUtil.getAssociatedPort(hop.getDstPort());

            Device srcNode = setNodeModel(diagram, src.getNode());
            PhysicalEthernetPort srcPort = setPortModel(srcNode, src);

            Device dstNode = setNodeModel(diagram, dst.getNode());
            PhysicalEthernetPort dstPort = setPortModel(dstNode, dst);

            IpSubnetDto link = NodeUtil.getLayer3Link(src);

            setLinkModel(diagram, link, srcPort, dstPort, isActive, isUp, isPrimary);
        }

    }

    private void sortDevicesOrder(IDiagram diagram, RsvpLspDto lsp) throws InventoryException {
        for (Device device : diagram.getDevices()) {
            diagram.removeDevice(device);
        }

        for (String inventoryId : getDevicesOrder(lsp)) {
            diagram.addDevice(deviceCache.get(inventoryId));
        }
    }

    private List<String> getDevicesOrder(RsvpLspDto lsp) throws InventoryException {
        LinkedHashSet<String> order = new LinkedHashSet<String>();
        Deque<String> stack = new ArrayDeque<String>();

        LinkedList<String> primaryHopNodes = getNodeOrderInHops(lsp.getHopSeries1());
        LinkedList<String> secondaryHopNodes = getNodeOrderInHops(lsp.getHopSeries2());

        String inventoryId_on_primary = null;
        String inventoryId_on_secondary = null;

        Iterator<String> primaryHops_descendingIterator = primaryHopNodes.descendingIterator();
        Iterator<String> secondaryHops_descendingIterator = secondaryHopNodes.descendingIterator();

        if (primaryHopNodes.size() > 0 && secondaryHopNodes.size() > 0) {
            while (primaryHops_descendingIterator.hasNext() && secondaryHops_descendingIterator.hasNext()) {
                inventoryId_on_primary = primaryHops_descendingIterator.next();
                inventoryId_on_secondary = secondaryHops_descendingIterator.next();
                if (inventoryId_on_primary.equals(inventoryId_on_secondary)) {
                    stack.push(inventoryId_on_primary);
                } else {
                    break;
                }
            }
        } else if (primaryHopNodes.size() > 0) {
            inventoryId_on_primary = primaryHopNodes.get(primaryHopNodes.size() - 1);
            stack.push(inventoryId_on_primary);
        } else if (secondaryHopNodes.size() > 0) {
            inventoryId_on_secondary = secondaryHopNodes.get(secondaryHopNodes.size() - 1);
            stack.push(inventoryId_on_secondary);
        }
        if (stack.isEmpty()) {
            throw new InventoryException("lsp[" + RsvpLspRenderer.getLspName(lsp) + "] does not have egress!");
        }

        Iterator<String> primaryHops_iterator = primaryHopNodes.iterator();
        Iterator<String> secondaryHops_iterator = secondaryHopNodes.iterator();
        if (primaryHopNodes.size() > 0 && secondaryHopNodes.size() > 0) {
            while (primaryHops_iterator.hasNext() && secondaryHops_iterator.hasNext()) {
                inventoryId_on_primary = primaryHops_iterator.next();
                inventoryId_on_secondary = secondaryHops_iterator.next();
                if (inventoryId_on_primary.equals(inventoryId_on_secondary)) {
                    order.add(inventoryId_on_primary);
                } else {
                    break;
                }
            }
        } else if (primaryHopNodes.size() > 0) {
            inventoryId_on_primary = primaryHopNodes.get(0);
            order.add(inventoryId_on_primary);
        } else if (secondaryHopNodes.size() > 0) {
            inventoryId_on_secondary = secondaryHopNodes.get(0);
            order.add(inventoryId_on_secondary);
        }
        if (order.isEmpty()) {
            throw new InventoryException("lsp[" + RsvpLspRenderer.getLspName(lsp) + "] does not have ingress!");
        }

        while (inventoryId_on_primary != null && !inventoryId_on_primary.equals(stack.getFirst())) {
            order.add(inventoryId_on_primary);
            if (primaryHops_iterator.hasNext()) {
                inventoryId_on_primary = primaryHops_iterator.next();
            } else {
                break;
            }
        }
        while (inventoryId_on_secondary != null && !inventoryId_on_secondary.equals(stack.getFirst())) {
            order.add(inventoryId_on_secondary);
            if (secondaryHops_iterator.hasNext()) {
                inventoryId_on_secondary = secondaryHops_iterator.next();
            } else {
                break;
            }
        }

        order.addAll(stack);

        return (new ArrayList<String>(order));
    }

    private LinkedList<String> getNodeOrderInHops(RsvpLspHopSeriesDto path) {
        LinkedList<String> result = new LinkedList<String>();
        if (path == null) {
            return result;
        }
        LinkedHashSet<String> nodes = new LinkedHashSet<String>();
        List<PathHopDto> hops = path.getHops();
        for (PathHopDto hop : hops) {
            PortDto src = NodeUtil.getAssociatedPort(hop.getSrcPort());
            PortDto dst = NodeUtil.getAssociatedPort(hop.getDstPort());

            nodes.add(InventoryIdUtil.getInventoryId(src.getNode()));
            nodes.add(InventoryIdUtil.getInventoryId(dst.getNode()));
        }
        result.addAll(nodes);
        return result;
    }


}