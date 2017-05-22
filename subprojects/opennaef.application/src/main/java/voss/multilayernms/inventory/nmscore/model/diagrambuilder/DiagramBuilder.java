package voss.multilayernms.inventory.nmscore.model.diagrambuilder;

import jp.iiga.nmt.core.model.*;
import naef.dto.LinkDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpSubnetDto;
import net.phalanx.core.models.IPropertyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.constants.ViewConstants;
import voss.multilayernms.inventory.nmscore.inventory.InventoryIdUtil;
import voss.multilayernms.inventory.nmscore.model.creator.LinkModelCreator;
import voss.multilayernms.inventory.nmscore.model.creator.NodeModelCreator;
import voss.multilayernms.inventory.nmscore.model.creator.PortModelCreator;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.renderer.PortRenderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DiagramBuilder {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(DiagramBuilder.class);

    protected Map<String, Device> deviceCache = new LinkedHashMap<String, Device>();
    protected Map<String, PhysicalEthernetPort> etherPortCache = new LinkedHashMap<String, PhysicalEthernetPort>();

    protected void setOperationStatusColor(ILink pl, boolean isUp) {
        if (isUp) {
            pl.setPropertyValue(IVisualConstants.PROPERTY_LINE_COLOR, "0,0,255");
        } else {
            pl.setPropertyValue(IVisualConstants.PROPERTY_LINE_COLOR, "255,0,0");
        }
    }

    protected Device setNodeModel(IDiagram diagram, NodeDto node) throws IOException {
        String inventoryId = InventoryIdUtil.getInventoryId(node);

        if (deviceCache.containsKey(inventoryId)) {
            return deviceCache.get(inventoryId);
        }
        Device device = NodeModelCreator.createModel(node.getNode(), inventoryId);
        List<String> iRefs = new ArrayList<String>();
        iRefs.add(inventoryId);
        device.setPropertyValue(IPropertyConstants.PROPERTY_INVENTORY_REFS, iRefs);
        device.setText(NodeRenderer.getNodeName(node.getNode()));
        device.setConstraint("0,0,250,150");
        diagram.addDevice(device);
        deviceCache.put(inventoryId, device);

        return device;
    }

    protected PhysicalEthernetPort setPortModel(Device device, PortDto port) throws IOException {
        return setPortModel(device, port, "");
    }

    protected PhysicalEthernetPort setPortModel(Device device, PortDto port, String suffix) throws IOException {
        String inventoryId = InventoryIdUtil.getInventoryId(port);

        if (etherPortCache.containsKey(inventoryId)) {
            return etherPortCache.get(inventoryId);
        }
        PhysicalEthernetPort etherPort = PortModelCreator.createModel(port, inventoryId);
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
        device.addPort(etherPort);

        etherPortCache.put(inventoryId + suffix, etherPort);
        return etherPort;
    }

    protected PhysicalLink setLinkModel(IDiagram diagram, IpSubnetDto link,
                                        EthernetPort srcPort, EthernetPort dstPort,
                                        boolean isActive, boolean isUp) throws IOException {
        return setLinkModel(diagram, link, srcPort, dstPort, isActive, isUp, true);
    }

    protected PhysicalLink setLinkModel(IDiagram diagram, IpSubnetDto link,
                                        EthernetPort srcPort, EthernetPort dstPort,
                                        boolean isActive, boolean isUp, boolean isPrimary) throws IOException {
        String linkID = InventoryIdUtil.getInventoryId(link);
        PhysicalLink phyLink = LinkModelCreator.createModel(link, linkID);
        return setLinkModel(diagram, phyLink, linkID, srcPort, dstPort, isActive, isUp, isPrimary);
    }

    protected PhysicalLink setLinkModel(IDiagram diagram, LinkDto link,
                                        EthernetPort srcPort, EthernetPort dstPort,
                                        boolean isActive, boolean isUp) throws IOException {
        return setLinkModel(diagram, link, srcPort, dstPort, isActive, isUp, true);
    }

    protected PhysicalLink setLinkModel(IDiagram diagram, LinkDto link,
                                        EthernetPort srcPort, EthernetPort dstPort,
                                        boolean isActive, boolean isUp, boolean isPrimary) throws IOException {
        String linkID = InventoryIdCalculator.getId(link);
        PhysicalLink phyLink = LinkModelCreator.createModel(link, linkID);
        return setLinkModel(diagram, phyLink, linkID, srcPort, dstPort, isActive, isUp, isPrimary);
    }

    protected PhysicalLink setLinkModel(IDiagram diagram, PhysicalLink link,
                                        String physicalLinkID, EthernetPort srcPort, EthernetPort dstPort,
                                        boolean isActive, boolean isUp, boolean isPrimary) throws IOException {
        link.setText("");
        List<String> iRefs = new ArrayList<String>();
        iRefs.add(physicalLinkID);
        link.setPropertyValue(IPropertyConstants.PROPERTY_INVENTORY_REFS, iRefs);
        link.setPropertyValue(IPropertyConstants.PROPERTY_LINK_PRIORITY, (isPrimary ? 1 : 2));
        if (!isActive) {
            setBackupPathStyle(link);
        }
        setOperationStatusColor(link, isUp);
        link.setSource(srcPort);
        link.setTarget(dstPort);
        link.attachSource();
        link.attachTarget();
        diagram.addChild(link);

        return link;
    }

    protected void setBackupPathStyle(ILink link) {
        link.setPropertyValue(IVisualConstants.PROPERTY_LINE_ATTRIBUTES, "");
        link.setPropertyValue(IVisualConstants.PROPERTY_LINE_ATTRIBUTES + ".style", 6);
        link.setPropertyValue(IVisualConstants.PROPERTY_LINE_ATTRIBUTES + ".dash", new float[]{3.0F});
        link.setPropertyValue(IVisualConstants.PROPERTY_LINE_ATTRIBUTES + ".dashOffset", 3.0F);
    }

}