package voss.multilayernms.inventory.nmscore.inventory.accessor;

import jp.iiga.nmt.core.expressions.EqualsMatcher;
import naef.dto.InterconnectionIfDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspDto;
import naef.ui.NaefDtoFacade;
import naef.ui.NaefDtoFacade.SearchMethod;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.PseudoWire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.MVO;
import tef.skelton.AuthenticationException;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdBuilder;
import voss.core.server.naming.inventory.InventoryIdDecoder;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.builder.PortFacilityStatusBuilder;
import voss.multilayernms.inventory.builder.PseudoWireFacilityStatusBuilder;
import voss.multilayernms.inventory.config.NmsCorePseudoWireConfiguration;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.inventory.InventoryIdUtil;
import voss.multilayernms.inventory.nmscore.model.FakePseudoWire;
import voss.multilayernms.inventory.nmscore.model.FakePseudoWirePipeType;
import voss.multilayernms.inventory.nmscore.model.FakePseudoWirePwType;
import voss.multilayernms.inventory.nmscore.model.creator.PseudoWireModelCreator;
import voss.multilayernms.inventory.nmscore.rendering.PseudoWireRenderingUtil;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.renderer.PseudoWireRenderer;
import voss.multilayernms.inventory.renderer.RsvpLspRenderer;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.PseudoWireUtil;
import voss.nms.inventory.util.RsvpLspUtil;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.*;

public class PseudoWireHandler {

    private static final Logger log = LoggerFactory.getLogger(PseudoWireHandler.class);

    public static String getInventoryIdOnLsp(PseudowireDto pw, RsvpLspDto lsp) {
        return InventoryIdBuilder.getPseudoWireID(RsvpLspUtil.getIngressNode(lsp).getName(),
                PseudoWireRenderer.getPseudoWireID(pw));
    }

    public static String parseNodeId(String inventoryId) {
        String result = null;
        try {
            String nodeName = InventoryIdDecoder.getNode(inventoryId);
            result = InventoryIdBuilder.getNodeID(nodeName);
        } catch (ParseException e) {
            log.debug(e.getMessage());
        }
        return result;
    }

    public static String parseVcId(String inventoryId) {
        String result = null;
        try {
            result = InventoryIdDecoder.getPseudoWireID(inventoryId).toString();
        } catch (ParseException e) {
            log.debug(e.getMessage());
        }
        return result;
    }

    public static List<PseudoWire> get(String inventoryId) throws NotBoundException, AuthenticationException, IOException, ExternalServiceException {
        ObjectFilterQuery query = new ObjectFilterQuery(PseudoWire.class);

        query.addMacther(NmsCorePseudoWireConfiguration.getInstance().getIdFieldName(), new EqualsMatcher(parseVcId(inventoryId)));

        return getList(query);
    }

    public static FakePseudoWire getPseudowireDto(String inventoryId) throws AuthenticationException, RemoteException,
            IOException, ExternalServiceException {
        MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
        for (PseudowireDto pw : conn.getDtoFacade().getPseudowires()) {
            if (pw.getAc1() != null && InventoryIdUtil.getInventoryIdOnAc1(pw).equals(inventoryId)) {
                return new FakePseudoWirePwType(pw);
            } else if (pw.getAc2() != null && InventoryIdUtil.getInventoryIdOnAc2(pw).equals(inventoryId)) {
                return new FakePseudoWirePwType(pw);
            } else if (InventoryIdUtil.getInventoryId(pw).equals(inventoryId)) {
                return new FakePseudoWirePwType(pw);
            }
        }
        for (InterconnectionIfDto pipe : conn.getNodePipeDtos()) {
            for (PortDto ac : pipe.getAttachedPorts()) {
                if (ac != null && InventoryIdUtil.getInventoryIdOnAc(pipe, ac).equals(inventoryId)) {
                    return new FakePseudoWirePipeType(pipe);
                }
            }
        }
        log.warn("no pseudowire or pipe found: " + inventoryId);
        return null;
    }

    public static List<PseudoWire> getListOnNode(String nodeInventoryId) throws AuthenticationException, NotBoundException,
            IOException, ExternalServiceException {
        List<PseudoWire> pwList = new ArrayList<PseudoWire>();
        Map<MVO.MvoId, FakePseudoWire> foundPseudoWires = new HashMap<MVO.MvoId, FakePseudoWire>();
        MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
        NaefDtoFacade facade = conn.getDtoFacade();
        for (NodeDto node : conn.getActiveNodes()) {
            if (!InventoryIdUtil.getInventoryId(node).equals(nodeInventoryId)) {
                continue;
            }
            Set<PseudowireDto> pws = conn.getDtoFacade().getPseudowires(node);
            for (PseudowireDto pw : pws) {
                if (foundPseudoWires.get(DtoUtil.getMvoId(pw)) != null) {
                    continue;
                }
                FakePseudoWire fpw = new FakePseudoWirePwType(pw);
                foundPseudoWires.put(DtoUtil.getMvoId(pw), fpw);
            }
            Collection<InterconnectionIfDto> pipes = facade.selectNodeElements(node, InterconnectionIfDto.class, SearchMethod.REGEXP, MPLSNMS_ATTR.IFNAME, ".*");
            for (InterconnectionIfDto pipe : pipes) {
                if (foundPseudoWires.get(DtoUtil.getMvoId(pipe)) != null) {
                    continue;
                } else if (pipe.getAttachedPorts().size() != 2) {
                    continue;
                }
                FakePseudoWire fpw = new FakePseudoWirePipeType(pipe);
                foundPseudoWires.put(DtoUtil.getMvoId(pipe), fpw);
            }
        }
        appendPwList(pwList, foundPseudoWires.values());
        return pwList;
    }

    public static List<PseudoWire> getListOnPort(String portInventoryId) throws AuthenticationException,
            NotBoundException, IOException, ExternalServiceException {
        LinkedHashSet<FakePseudoWire> pws = new LinkedHashSet<FakePseudoWire>();
        for (RsvpLspDto lsp : RsvpLspHandler.getLspDtosOnPort(portInventoryId)) {
            pws.addAll(getPseudoWiresOn(lsp));
        }

        List<PseudoWire> pwList = new ArrayList<PseudoWire>();
        appendPwList(pwList, pws);
        return pwList;
    }


    public static List<PseudoWire> getListOnLink(String linkInventoryId) throws AuthenticationException,
            NotBoundException, IOException, ExternalServiceException {
        LinkedHashSet<FakePseudoWire> pws = new LinkedHashSet<FakePseudoWire>();
        for (RsvpLspDto lsp : RsvpLspHandler.getLspDtosOnLink(linkInventoryId)) {
            pws.addAll(getPseudoWiresOn(lsp));
        }

        List<PseudoWire> pwList = new ArrayList<PseudoWire>();
        appendPwList(pwList, pws);
        return pwList;
    }

    public static List<PseudoWire> getListUpperLsp(String lspInventoryId) throws AuthenticationException,
            NotBoundException, IOException, ExternalServiceException {
        List<PseudoWire> pwList = new ArrayList<PseudoWire>();

        for (RsvpLspDto lsp : MplsNmsInventoryConnector.getInstance().getDtoFacade().getRsvpLsps()) {
            if (InventoryIdUtil.getInventoryId(lsp).equals(lspInventoryId)) {
                appendPwList(pwList, getPseudoWiresOn(lsp));
            }
        }
        return pwList;
    }


    public static List<PseudoWire> getList(ObjectFilterQuery query) throws NotBoundException, AuthenticationException,
            IOException, ExternalServiceException {
        List<PseudoWire> pwList = new ArrayList<PseudoWire>();

        MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
        NaefDtoFacade facade = conn.getDtoFacade();
        List<FakePseudoWire> pws = new ArrayList<FakePseudoWire>();
        for (PseudowireDto pw : facade.getPseudowires()) {
            FakePseudoWire fpw = new FakePseudoWirePwType(pw);
            pws.add(fpw);
        }
        for (InterconnectionIfDto pipe : conn.getNodePipeDtos()) {
            if (pipe.getAttachedPorts().size() != 2) {
                log.warn("i13n-if[" + pipe.getName() + "] doesn't have two attached ports.");
                continue;
            }
            FakePseudoWire fpw = new FakePseudoWirePipeType(pipe);
            pws.add(fpw);
        }
        appendPwList(pwList, filterPseudowires(query, pws));
        return pwList;
    }

    private static void appendPwList(List<PseudoWire> pwList, Collection<FakePseudoWire> pws) throws IOException {
        for (FakePseudoWire fpw : pws) {
            if (fpw.isPipe()) {
                String pipeName = fpw.getPseudoWireName();
                String nodeName = fpw.getPipe().getNode().getName();
                PortDto ac1 = fpw.getAc1();
                PortDto ac2 = fpw.getAc2();
                String ac1Name = PortRenderer.getIfName(ac1);
                String ac2Name = PortRenderer.getIfName(ac2);
                String id1 = InventoryIdBuilder.getPipeID(nodeName, pipeName, ac1Name);
                String id2 = InventoryIdBuilder.getPipeID(nodeName, pipeName, ac2Name);
                PseudoWire model1 = PseudoWireModelCreator.createModel(fpw, null, id1, ac1, ac2);
                PseudoWire model2 = PseudoWireModelCreator.createModel(fpw, null, id2, ac2, ac1);
                pwList.add(model1);
                pwList.add(model2);
            } else {
                PseudowireDto pw = fpw.getPseudowireDto();
                List<RsvpLspDto> lsps = RsvpLspUtil.getLspsUnder(pw);
                for (RsvpLspDto lsp : lsps) {
                    PortDto acOnIngress = PseudoWireHandler.getAcOnIngress(pw, lsp);
                    PortDto acOnEgress = PseudoWireHandler.getAcOnEgress(pw, lsp);
                    String id = getInventoryIdOnLsp(pw, lsp);
                    PseudoWire model = PseudoWireModelCreator.createModel(fpw, lsp, id, acOnIngress, acOnEgress);
                    pwList.add(model);
                }
            }
        }
    }

    public static boolean isAvailable(FakePseudoWire pw) {
        return (pw.getAc1() != null && pw.getAc2() != null);
    }

    private static List<FakePseudoWire> filterPseudowires(ObjectFilterQuery query, Collection<FakePseudoWire> pws) throws IOException {
        List<FakePseudoWire> result = new ArrayList<FakePseudoWire>();
        for (FakePseudoWire fpw : pws) {
            boolean matched = filter(query, fpw);
            if (matched) {
                result.add(fpw);
            }
        }
        return result;
    }

    private static boolean filter(ObjectFilterQuery query, FakePseudoWire fpw) throws IOException {
        if (fpw.isPseudoWire()) {
            return filterPseudoWire(query, fpw);
        } else if (fpw.isPipe()) {
            return filterNodePipe(query, fpw);
        } else {
            return false;
        }
    }

    private static boolean filterPseudoWire(ObjectFilterQuery query, FakePseudoWire fpw) throws IOException {
        PseudowireDto pw = fpw.getPseudowireDto();
        for (RsvpLspDto lsp : pw.getRsvpLsps()) {
            boolean matched = true;
            PortDto ingressPort = PseudoWireUtil.getAcOnMe(pw, lsp.getIngressNode());
            PortDto egressPort = PseudoWireUtil.getAcOnMe(pw, RsvpLspUtil.getEgressNode(lsp));
            for (String field : query.keySet()) {
                String value = PseudoWireRenderingUtil.rendering(fpw, lsp, field, true, ingressPort, egressPort);
                if (field.equals(NmsCorePseudoWireConfiguration.getInstance().getRelatedRsvplspFieldName())) {
                    if (!isLspMatched(query, pw, field)) {
                        matched = false;
                        break;
                    }
                } else if (!query.get(field).matches(value)) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private static boolean filterNodePipe(ObjectFilterQuery query, FakePseudoWire fpw) throws IOException {
        InterconnectionIfDto pipe = fpw.getPipe();
        for (PortDto ac : pipe.getAttachedPorts()) {
            for (String field : query.keySet()) {
                String value = PseudoWireRenderingUtil.rendering(fpw, null, field, true, ac, ac);
                if (!query.get(field).matches(value)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isLspMatched(ObjectFilterQuery query, PseudowireDto pw, String field) {
        for (RsvpLspDto lsp : RsvpLspUtil.getLspsUnder(pw)) {
            if (query.get(field).matches(RsvpLspRenderer.getLspName(lsp))) {
                return true;
            }
        }
        return false;
    }

    public static PortDto getAcOnIngress(PseudowireDto pw, RsvpLspDto lsp) {
        String ingress = RsvpLspRenderer.getIngressNodeName(lsp);
        if (ingress == null) {
            return null;
        }

        String nodeOnAc1 = NodeRenderer.getNodeName(pw.getAc1().getNode());
        if (ingress.equals(nodeOnAc1)) {
            return pw.getAc1();
        } else {
            return pw.getAc2();
        }
    }

    public static PortDto getAcOnEgress(PseudowireDto pw, RsvpLspDto lsp) {
        String egress = RsvpLspRenderer.getEgressNodeName(lsp);
        if (egress == null) {
            return null;
        }

        String nodeOnAc1 = NodeRenderer.getNodeName(pw.getAc1().getNode());
        if (egress.equals(nodeOnAc1)) {
            return pw.getAc1();
        } else {
            return pw.getAc2();
        }
    }


    public static RsvpLspDto getLspOnSrc(String inventoryId) throws AuthenticationException, RemoteException,
            IOException, ExternalServiceException {
        FakePseudoWire pw = getPseudowireDto(inventoryId);
        if (pw.isPipe()) {
            return null;
        }

        for (RsvpLspDto lsp : RsvpLspUtil.getLspsUnder(pw.getPseudowireDto())) {
            if (getInventoryIdOnLsp(pw.getPseudowireDto(), lsp).equals(inventoryId)) {
                return lsp;
            }
        }
        return null;
    }

    public static void executeSetFacilityStatus(FakePseudoWire fpw, FacilityStatus fs, String userName)
            throws IOException, InventoryException, ExternalServiceException {
        if (fpw == null) {
            return;
        } else if (fpw.isPseudoWire()) {
            executeSetFacilityStatus(fpw.getPseudowireDto(), fs, userName);
            return;
        }
        InterconnectionIfDto pipe = fpw.getPipe();
        PortFacilityStatusBuilder builder = new PortFacilityStatusBuilder(pipe, userName);
        builder.setFacilityStatus(fs);
        builder.buildCommand();
        ShellConnector.getInstance().execute(builder);
        log.debug("update completed: id[" + PortRenderer.getIfName(pipe) + "/" + fs + "]");
    }

    public static void executeSetFacilityStatus(PseudowireDto pw, FacilityStatus fs, String userName)
            throws IOException, InventoryException, ExternalServiceException {
        PseudoWireFacilityStatusBuilder builder = new PseudoWireFacilityStatusBuilder(
                pw, userName);
        builder.setFacilityStatus(fs);
        builder.buildCommand();
        ShellConnector.getInstance().execute(builder);
        log.debug("update completed: id[" + PseudoWireRenderer.getPseudoWireID(pw) + "/" + fs + "]");
    }

    private static List<FakePseudoWire> getPseudoWiresOn(RsvpLspDto lsp) {
        List<PseudowireDto> pws = RsvpLspUtil.getPseudowiresOn(lsp);
        List<FakePseudoWire> result = new ArrayList<FakePseudoWire>();
        for (PseudowireDto pw : pws) {
            FakePseudoWire fpw = new FakePseudoWirePwType(pw);
            result.add(fpw);
        }
        return result;
    }

}