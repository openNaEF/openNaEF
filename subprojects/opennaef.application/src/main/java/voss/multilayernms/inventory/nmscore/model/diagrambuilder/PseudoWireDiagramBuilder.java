package voss.multilayernms.inventory.nmscore.model.diagrambuilder;

import jp.iiga.nmt.core.model.Device;
import jp.iiga.nmt.core.model.Diagram;
import jp.iiga.nmt.core.model.IDiagram;
import jp.iiga.nmt.core.model.PhysicalEthernetPort;
import naef.dto.PortDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspDto;
import net.phalanx.core.models.IPropertyConstants;
import net.phalanx.core.models.LabelSwitchedPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.nmscore.inventory.InventoryIdUtil;
import voss.multilayernms.inventory.nmscore.inventory.accessor.PseudoWireHandler;
import voss.multilayernms.inventory.nmscore.inventory.accessor.RsvpLspHandler;
import voss.multilayernms.inventory.nmscore.inventory.constants.OPERATION_STATUS;
import voss.multilayernms.inventory.nmscore.model.FakePseudoWire;
import voss.multilayernms.inventory.nmscore.model.creator.PseudoWireModelCreator;
import voss.multilayernms.inventory.nmscore.model.creator.RsvpLspModelCreator;
import voss.multilayernms.inventory.renderer.PseudoWireRenderer;
import voss.multilayernms.inventory.renderer.RsvpLspRenderer;
import voss.nms.inventory.util.RsvpLspUtil;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class PseudoWireDiagramBuilder extends DiagramBuilder {
    private static final Logger log = LoggerFactory.getLogger(PseudoWireDiagramBuilder.class);

    public IDiagram build(String inventoryId) throws RemoteException, ExternalServiceException, IOException, AuthenticationException, NotBoundException {
        IDiagram diagram = new Diagram();

        FakePseudoWire fpw = PseudoWireHandler.getPseudowireDto(inventoryId);
        RsvpLspDto lsp = null;
        PortDto ingress = null;
        PortDto egress = null;
        if (fpw.isPipe()) {
            lsp = null;
            ingress = fpw.getAc1();
            egress = fpw.getAc2();
        } else {
            lsp = PseudoWireHandler.getLspOnSrc(inventoryId);
            ingress = PseudoWireHandler.getAcOnIngress(fpw.getPseudowireDto(), lsp);
            egress = PseudoWireHandler.getAcOnEgress(fpw.getPseudowireDto(), lsp);
        }

        List<String> iRefs = new ArrayList<String>();
        iRefs.add(inventoryId);
        diagram.setPropertyValue(IPropertyConstants.PROPERTY_INVENTORY_REFS, iRefs);
        diagram.setPropertyValue(IPropertyConstants.PROPERTY_DIAGRAM_SORUCE,
                PseudoWireModelCreator.createModel(fpw, lsp, inventoryId, ingress, egress));
        diagram.setText(InventoryIdUtil.unEscape(inventoryId));

        if (fpw.isPseudoWire()) {
            attachLsp(diagram, fpw.getPseudowireDto());
        } else {
            drawPipe(diagram, fpw);
        }

        return diagram;
    }

    private void drawPipe(IDiagram diagram, FakePseudoWire pipe) throws IOException {
        PortDto ac1 = pipe.getAc1();
        PortDto ac2 = pipe.getAc2();
        Device device = setNodeModel(diagram, ac1.getNode());
        setPortModel(device, ac1);
        setPortModel(device, ac2);
    }

    private void attachLsp(IDiagram diagram, PseudowireDto pw) throws IOException {
        PortDto ac1 = pw.getAc1();
        PortDto ac2 = pw.getAc2();
        Device device1 = setNodeModel(diagram, ac1.getNode());
        Device device2 = setNodeModel(diagram, ac2.getNode());
        setPortModel(device1, ac1);
        setPortModel(device2, ac2);

        for (RsvpLspDto lsp : RsvpLspUtil.getLspsUnder(pw)) {
            String inventoryId = InventoryIdUtil.getInventoryId(lsp);

            PortDto src = RsvpLspHandler.getSrcPort(lsp);
            PortDto dst = RsvpLspHandler.getDstPort(lsp);
            if (src == null || dst == null) {
                log.warn("lsp has no hop: ignored: " + lsp.getName());
                continue;
            }

            String srcNodeInventoryId = InventoryIdUtil.getInventoryId(src.getNode());
            String dstNodeInventoryId = InventoryIdUtil.getInventoryId(dst.getNode());
            Device srcDevice = deviceCache.get(srcNodeInventoryId);
            if (srcDevice == null) {
                srcDevice = setNodeModel(diagram, src.getNode());
            }
            PhysicalEthernetPort srcPort = setPortModel(srcDevice, src, "(src)");
            Device dstDevice = deviceCache.get(dstNodeInventoryId);
            if (dstDevice == null) {
                dstDevice = setNodeModel(diagram, dst.getNode());
            }
            PhysicalEthernetPort dstPort = setPortModel(dstDevice, dst, "(dst)");

            LabelSwitchedPath path = RsvpLspModelCreator.createModel(lsp, InventoryIdUtil.getInventoryId(lsp));
            List<String> iRefs = new ArrayList<String>();
            iRefs.add(inventoryId);
            path.setPropertyValue(IPropertyConstants.PROPERTY_INVENTORY_REFS, iRefs);
            path.setText(RsvpLspRenderer.getLspName(lsp));
            setOperationStatusColor(path, PseudoWireRenderer.getOperStatus(pw).equals(OPERATION_STATUS.UP));

            path.setSource(srcPort);
            path.setTarget(dstPort);

            path.attachSource();
            path.attachTarget();

            diagram.addChild(path);
        }
    }

}