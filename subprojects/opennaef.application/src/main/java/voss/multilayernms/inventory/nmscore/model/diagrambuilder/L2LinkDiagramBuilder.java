package voss.multilayernms.inventory.nmscore.model.diagrambuilder;

import jp.iiga.nmt.core.model.*;
import naef.dto.LinkDto;
import naef.dto.NaefDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.atm.AtmApsIfDto;
import naef.dto.atm.AtmPortDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.pos.PosApsIfDto;
import naef.dto.pos.PosPortDto;
import net.phalanx.core.models.IPropertyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.MVO;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.MvoDtoMap;
import voss.multilayernms.inventory.constants.ViewConstants;
import voss.multilayernms.inventory.nmscore.inventory.InventoryIdUtil;
import voss.multilayernms.inventory.nmscore.inventory.accessor.L2LinkHandler;
import voss.multilayernms.inventory.nmscore.inventory.constants.OPERATION_STATUS;
import voss.multilayernms.inventory.nmscore.model.creator.LinkModelCreator;
import voss.multilayernms.inventory.nmscore.model.creator.PortModelCreator;
import voss.multilayernms.inventory.renderer.L2LinkRenderer;
import voss.multilayernms.inventory.renderer.LinkRenderer;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.nms.inventory.util.NodeUtil;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class L2LinkDiagramBuilder extends DiagramBuilder {

    private static final Logger log = LoggerFactory.getLogger(L2LinkDiagramBuilder.class);

    public IDiagram build(String inventoryId) throws RemoteException, InventoryException, IOException, AuthenticationException, NotBoundException {
        MvoDtoMap modelCache = new MvoDtoMap();

        IDiagram diagram = new Diagram();

        LinkDto link = L2LinkHandler.getLinkDto(inventoryId);

        List<String> iRefs = new ArrayList<String>();
        iRefs.add(inventoryId);
        diagram.setPropertyValue(IPropertyConstants.PROPERTY_INVENTORY_REFS, iRefs);
        diagram.setPropertyValue(IPropertyConstants.PROPERTY_DIAGRAM_SORUCE, LinkModelCreator.createModel(link, inventoryId));
        diagram.setText(L2LinkRenderer.getName(link));

        PortDto port1 = L2LinkRenderer.getPort1(link);
        PortDto port2 = L2LinkRenderer.getPort2(link);
        PortDto aggregator1 = getAggregatorPort(port1);
        PortDto aggregator2 = getAggregatorPort(port2);
        NodeDto node1 = port1.getNode();
        Device device1 = setNodeModel(diagram, node1);
        NodeDto node2 = port2.getNode();
        Device device2 = setNodeModel(diagram, node2);
        modelCache.put(node1, device1);
        modelCache.put(node2, device2);

        if (aggregator1 != null) {
            buildDevicePortAggregatedStructure(aggregator1, modelCache);
        } else {
            buildDevicePortSimpleStructure(port1, modelCache);
        }
        if (aggregator2 != null) {
            buildDevicePortAggregatedStructure(aggregator2, modelCache);
        } else {
            buildDevicePortSimpleStructure(port2, modelCache);
        }
        assosiatePorts(diagram, modelCache);
        return diagram;
    }

    private void buildDevicePortAggregatedStructure(PortDto aggregator, MvoDtoMap modelCache) throws IOException {
        List<PortDto> members = getMemberPorts(aggregator);
        LogicalDeviceLayer dl = new LogicalDeviceLayer();
        MetaData metadata = new MetaData();
        dl.setMetaData(metadata);
        modelCache.put(aggregator, dl);
        Device d = (Device) modelCache.getObject(aggregator.getNode());
        d.addChild(dl);
        for (PortDto member : members) {
            LogicalEthernetPort port = createLogicalPortModel(member, "");
            dl.addPort(port);
            modelCache.put(member, port);
        }
    }

    private void buildDevicePortSimpleStructure(PortDto port, MvoDtoMap modelCache) throws IOException {
        Device d = (Device) modelCache.getObject(port.getNode());
        PhysicalEthernetPort pe = createPhysicalPortModel(port, "");
        d.addPort(pe);
        modelCache.put(port, pe);
    }

    private void assosiatePorts(IDiagram diagram, MvoDtoMap modelCache) throws IOException {
        Set<MVO.MvoId> known = new HashSet<MVO.MvoId>();
        for (NaefDto dto : modelCache.getDtos()) {
            if (dto == null) {
                continue;
            } else if (!(dto instanceof PortDto)) {
                continue;
            }
            PortDto port = (PortDto) dto;
            LinkDto link = NodeUtil.getLayer2Link(port);
            if (link == null) {
                continue;
            }
            PortDto neighbor = NodeUtil.getLayer2Neighbor(port);
            if (neighbor == null) {
                continue;
            }
            Object o1 = modelCache.getObject(port);
            if (o1 == null) {
                log.warn("port is not built.");
                continue;
            } else if (!(o1 instanceof EthernetPort)) {
                log.warn("port is not EthernetPort: " + o1.toString());
                continue;
            }
            EthernetPort p1 = (EthernetPort) o1;
            Object o2 = modelCache.getObject(neighbor);
            if (o2 == null) {
                log.warn("neighbor is not built.");
                continue;
            } else if (!(o2 instanceof EthernetPort)) {
                log.warn("neighbor is not port: " + o2.toString());
                continue;
            }
            EthernetPort p2 = (EthernetPort) o2;
            Device device1 = (Device) modelCache.getObject(port.getNode());
            Device device2 = (Device) modelCache.getObject(neighbor.getNode());
            setPortLinkPortModel(diagram, device1, p1, device2, p2, link);
            known.add(DtoUtil.getMvoId(port));
            known.add(DtoUtil.getMvoId(neighbor));
        }
    }

    private void setPortLinkPortModel(IDiagram diagram, Device device1, EthernetPort port1,
                                      Device device2, EthernetPort port2, LinkDto link) throws IOException {
        String status = LinkRenderer.getOperStatus(link);
        boolean isUP = (status == null ? false : status.equals(OPERATION_STATUS.UP));
        setLinkModel(diagram, link, port1, port2, true, isUP);
    }

    protected PhysicalEthernetPort createPhysicalPortModel(PortDto port, String suffix) throws IOException {
        String inventoryId = InventoryIdUtil.getInventoryId(port);
        EthernetPort etherPort = PortModelCreator.createModel(port, inventoryId);
        setEthernetPortMetadata(inventoryId, etherPort, port, suffix);
        return (PhysicalEthernetPort) etherPort;
    }

    protected LogicalEthernetPort createLogicalPortModel(PortDto port, String suffix) throws IOException {
        String inventoryId = InventoryIdUtil.getInventoryId(port);
        EthernetPort etherPort = PortModelCreator.createLogicalPortModel(port, inventoryId);
        setEthernetPortMetadata(inventoryId, etherPort, port, suffix);
        return (LogicalEthernetPort) etherPort;
    }

    private void setEthernetPortMetadata(String inventoryId, EthernetPort etherPort, PortDto port,
                                         String suffix) {
        List<String> iRefs = new ArrayList<String>();
        iRefs.add(inventoryId);
        etherPort.setPropertyValue(IPropertyConstants.PROPERTY_INVENTORY_REFS, iRefs);
        etherPort.setText(PortRenderer.getIfName(port) + suffix);
        String portConstraint = DtoUtil.getStringOrNull(port, ViewConstants.ATTR_POSITION);
        if (portConstraint != null) {
            Integer pos = Integer.valueOf(portConstraint);
            etherPort.setConstraint(pos);
        } else {
            etherPort.setConstraint(8);
        }
    }

    private PortDto getAggregatorPort(PortDto port) {
        if (port == null) {
            return null;
        } else if (port instanceof EthPortDto) {
            return NodeUtil.getEthLag((EthPortDto) port);
        } else if (port instanceof AtmPortDto) {
            return NodeUtil.getAtmApsIf((AtmPortDto) port);
        } else if (port instanceof PosPortDto) {
            return NodeUtil.getPosApsIf((PosPortDto) port);
        }
        return null;
    }

    private List<PortDto> getMemberPorts(PortDto aggregator) {
        if (aggregator == null) {
            return null;
        } else if (aggregator instanceof EthLagIfDto) {
            EthLagIfDto lag = (EthLagIfDto) aggregator;
            return new ArrayList<PortDto>(lag.getBundlePorts());
        } else if (aggregator instanceof AtmApsIfDto) {
            AtmApsIfDto aps = (AtmApsIfDto) aggregator;
            return new ArrayList<PortDto>(aps.getAtmPorts());
        } else if (aggregator instanceof PosApsIfDto) {
            PosApsIfDto aps = (PosApsIfDto) aggregator;
            return new ArrayList<PortDto>(aps.getPosPorts());
        }
        return null;
    }

}