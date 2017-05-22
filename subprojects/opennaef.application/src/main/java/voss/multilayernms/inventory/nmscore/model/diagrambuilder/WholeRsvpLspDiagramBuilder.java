package voss.multilayernms.inventory.nmscore.model.diagrambuilder;

import jp.iiga.nmt.core.model.*;
import naef.dto.PathHopDto;
import naef.dto.PortDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspIdPoolDto;
import net.phalanx.core.models.IPropertyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.inventory.InventoryIdUtil;
import voss.multilayernms.inventory.nmscore.model.creator.LinkModelCreator;
import voss.nms.inventory.util.NodeUtil;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WholeRsvpLspDiagramBuilder extends DiagramBuilder {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(WholeRsvpLspDiagramBuilder.class);

    private final Map<String, PhysicalLink> linkCache = new HashMap<String, PhysicalLink>();

    public IDiagram build() throws RemoteException, InventoryException, ExternalServiceException, IOException, AuthenticationException, NotBoundException {
        IDiagram diagram = new Diagram();
        List<String> iRefs = new ArrayList<String>();
        diagram.setPropertyValue(IPropertyConstants.PROPERTY_INVENTORY_REFS, iRefs);
        diagram.setText("Whole LSP Topology");

        MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
        for (RsvpLspIdPoolDto pool : conn.getRsvpLspIdPool()) {
            for (RsvpLspDto lsp : pool.getUsers()) {
                if (lsp.getHopSeries1() != null) {
                    setModelsOnHopSeries(diagram, lsp.getHopSeries1().getHops(), true);
                }
                if (lsp.getHopSeries2() != null) {
                    setModelsOnHopSeries(diagram, lsp.getHopSeries2().getHops(), false);
                }
            }
        }
        for (PhysicalLink link : this.linkCache.values()) {
            setPathStyle(link);
        }
        return diagram;
    }

    private void setModelsOnHopSeries(IDiagram diagram, List<PathHopDto> hops, boolean isActive) throws IOException {

        for (PathHopDto hop : hops) {
            PortDto src = NodeUtil.getAssociatedPort(hop.getSrcPort());
            PortDto dst = NodeUtil.getAssociatedPort(hop.getDstPort());

            Device srcNode = setNodeModel(diagram, src.getNode());
            PhysicalEthernetPort srcPort = setPortModel(srcNode, src);

            Device dstNode = setNodeModel(diagram, dst.getNode());
            PhysicalEthernetPort dstPort = setPortModel(dstNode, dst);

            IpSubnetDto link = NodeUtil.getLayer3Link(src);

            setLinkModel(diagram, link, srcPort, dstPort, isActive);
        }

    }

    protected PhysicalLink setLinkModel(IDiagram diagram, IpSubnetDto subnet,
                                        EthernetPort srcPort, EthernetPort dstPort, boolean isActive) throws IOException {
        String linkID = InventoryIdUtil.getInventoryId(subnet);
        PhysicalLink link = this.linkCache.get(linkID);
        if (link != null) {
            setPrimaryFlag(link, isActive);
            return link;
        }
        link = LinkModelCreator.createModel(subnet, linkID);
        link.setText("");
        List<String> iRefs = new ArrayList<String>();
        iRefs.add(linkID);
        link.setPropertyValue(IPropertyConstants.PROPERTY_INVENTORY_REFS, iRefs);
        setPrimaryFlag(link, isActive);
        setOperationStatusColor(link, true);
        link.setSource(srcPort);
        link.setTarget(dstPort);
        link.attachSource();
        link.attachTarget();
        diagram.addChild(link);
        this.linkCache.put(linkID, link);

        return link;
    }

    private void setPrimaryFlag(PhysicalLink link, boolean isActive) {
        if (isActive) {
            link.setPropertyValue("primary", "true");
        }
    }

    private void setPathStyle(PhysicalLink link) {
        Object isPrimary = link.getPropertyValue("primary");
        if (isPrimary == null) {
            setBackupPathStyle(link);
        }
    }
}