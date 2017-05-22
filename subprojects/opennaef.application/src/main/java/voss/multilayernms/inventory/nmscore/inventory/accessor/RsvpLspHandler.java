package voss.multilayernms.inventory.nmscore.inventory.accessor;

import jp.iiga.nmt.core.expressions.EqualsMatcher;
import naef.dto.NodeDto;
import naef.dto.PathHopDto;
import naef.dto.PortDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.LabelSwitchedPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdBuilder;
import voss.core.server.naming.inventory.InventoryIdDecoder;
import voss.multilayernms.inventory.builder.SpecificRsvpLspAttributeCommandBuilder;
import voss.multilayernms.inventory.builder.LspCouplingCommandBuilder;
import voss.multilayernms.inventory.builder.MplsNmsCreationTaskCommandBuilder;
import voss.multilayernms.inventory.config.NmsCoreRsvpLspConfiguration;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.inventory.InventoryIdUtil;
import voss.multilayernms.inventory.nmscore.model.creator.RsvpLspModelCreator;
import voss.multilayernms.inventory.nmscore.rendering.RsvpLspRenderingUtil;
import voss.multilayernms.inventory.renderer.PseudoWireRenderer;
import voss.multilayernms.inventory.renderer.RsvpLspRenderer;
import voss.nms.inventory.builder.PathCommandBuilder;
import voss.nms.inventory.builder.RsvpLspCommandBuilder;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.RsvpLspUtil;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.*;

public class RsvpLspHandler {

    private static final Logger log = LoggerFactory.getLogger(RsvpLspHandler.class);

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

    public static String parseLspName(String inventoryId) {
        String result = null;
        try {
            result = InventoryIdDecoder.getLspName(inventoryId);
        } catch (ParseException e) {
            log.debug(e.getMessage());
        }
        return result;
    }

    public static List<LabelSwitchedPath> get(String inventoryId) throws NotBoundException, AuthenticationException, IOException, ExternalServiceException {
        ObjectFilterQuery query = new ObjectFilterQuery(LabelSwitchedPath.class);
        query.addMacther(NmsCoreRsvpLspConfiguration.getInstance().getIdFieldName(), new EqualsMatcher(parseLspName(inventoryId)));
        return getList(query);
    }

    public static RsvpLspDto getRsvpLspDto(String inventoryId) throws AuthenticationException, RemoteException, IOException, ExternalServiceException {
        for (RsvpLspDto lsp : MplsNmsInventoryConnector.getInstance().getRegularRsvpLspPool().getUsers()) {
            if (InventoryIdUtil.getInventoryId(lsp).equals(inventoryId)) {
                return lsp;
            }
        }
        return null;
    }

    public static List<LabelSwitchedPath> getListOnNode(String nodeInventoryId) throws AuthenticationException, NotBoundException, IOException, ExternalServiceException {
        List<LabelSwitchedPath> lspList = new ArrayList<LabelSwitchedPath>();
        appendLSPList(lspList, getLspDtosOnNode(nodeInventoryId));
        return lspList;
    }

    private static List<RsvpLspDto> getLspDtosOnNode(String nodeInventoryId) throws AuthenticationException, RemoteException, IOException, ExternalServiceException {
        List<RsvpLspDto> lsps = new ArrayList<RsvpLspDto>();
        for (RsvpLspDto lsp : MplsNmsInventoryConnector.getInstance().getRegularRsvpLspPool().getUsers()) {
            NodeDto ingress = RsvpLspUtil.getIngressNode(lsp);
            NodeDto egress = RsvpLspUtil.getEgressNode(lsp);

            if (ingress != null
                    && InventoryIdUtil.getInventoryId(ingress).equals(nodeInventoryId)) {
                lsps.add(lsp);
            } else if (egress != null
                    && InventoryIdUtil.getInventoryId(egress).equals(nodeInventoryId)) {
                lsps.add(lsp);
            }
        }
        return lsps;
    }

    public static List<LabelSwitchedPath> getListOnPort(String portInventoryId) throws RemoteException, ExternalServiceException, IOException, AuthenticationException, NotBoundException {
        List<LabelSwitchedPath> lspList = new ArrayList<LabelSwitchedPath>();
        appendLSPList(lspList, getLspDtosOnPort(portInventoryId));
        return lspList;
    }

    public static List<RsvpLspDto> getLspDtosOnPort(String portInventoryId) throws AuthenticationException, RemoteException, IOException, ExternalServiceException {
        LinkedHashSet<RsvpLspDto> lsps = new LinkedHashSet<RsvpLspDto>();

        for (RsvpLspDto lsp : MplsNmsInventoryConnector.getInstance().getRegularRsvpLspPool().getUsers()) {

            HashSet<PathHopDto> hops = new HashSet<PathHopDto>();
            if (lsp.getHopSeries1() != null) {
                hops.addAll(lsp.getHopSeries1().getHops());
            }
            if (lsp.getHopSeries2() != null) {
                hops.addAll(lsp.getHopSeries2().getHops());
            }

            for (PathHopDto hop : hops) {
                PortDto src = NodeUtil.getAssociatedPort(hop.getSrcPort());
                PortDto dst = NodeUtil.getAssociatedPort(hop.getDstPort());
                if (InventoryIdUtil.getInventoryId(src).equals(portInventoryId)
                        || InventoryIdUtil.getInventoryId(dst).equals(portInventoryId)) {

                    lsps.add(lsp);
                }
            }
        }

        return (new ArrayList<RsvpLspDto>(lsps));
    }

    public static List<LabelSwitchedPath> getListOnLink(String linkInventoryId) throws RemoteException, AuthenticationException, ExternalServiceException, IOException, NotBoundException {
        List<LabelSwitchedPath> lspList = new ArrayList<LabelSwitchedPath>();
        appendLSPList(lspList, getLspDtosOnLink(linkInventoryId));
        return lspList;
    }

    public static LinkedHashSet<RsvpLspDto> getLspDtosOnLink(String linkInventoryId) throws AuthenticationException, RemoteException, IOException, ExternalServiceException {
        LinkedHashSet<RsvpLspDto> lsps = new LinkedHashSet<RsvpLspDto>();
        lsps.addAll(getLspDtosOnPort(LinkHandler.parseInventoryIdOfPort1(linkInventoryId)));
        lsps.addAll(getLspDtosOnPort(LinkHandler.parseInventoryIdOfPort2(linkInventoryId)));
        return lsps;
    }

    public static List<LabelSwitchedPath> getListUnderPseudoWire(String pwInventoryId) throws AuthenticationException, NotBoundException, IOException, ExternalServiceException {
        List<LabelSwitchedPath> lspList = new ArrayList<LabelSwitchedPath>();

        for (PseudowireDto pw : MplsNmsInventoryConnector.getInstance().getDtoFacade().getPseudowires()) {
            if (PseudoWireRenderer.getPseudoWireID(pw).equals(PseudoWireHandler.parseVcId(pwInventoryId))) {
                appendLSPList(lspList, pw.getRsvpLsps());
            }
        }

        return lspList;
    }

    public static List<LabelSwitchedPath> getList(ObjectFilterQuery query) throws NotBoundException, AuthenticationException, IOException, ExternalServiceException {
        List<LabelSwitchedPath> lspList = new ArrayList<LabelSwitchedPath>();

        List<RsvpLspDto> lsps = new ArrayList<RsvpLspDto>();
        for (RsvpLspDto lsp : MplsNmsInventoryConnector.getInstance().getRegularRsvpLspPool().getUsers()) {
            lsps.add(lsp);
        }
        appendLSPList(lspList, filterLSPs(query, lsps));

        return lspList;
    }

    private static void appendLSPList(List<LabelSwitchedPath> lspList, Collection<RsvpLspDto> lsps) throws IOException {
        for (RsvpLspDto lsp : lsps) {
            lspList.add(RsvpLspModelCreator.createModel(lsp, InventoryIdUtil.getInventoryId(lsp)));
        }
    }

    private static List<RsvpLspDto> filterLSPs(ObjectFilterQuery query, List<RsvpLspDto> lsps) {
        List<RsvpLspDto> result = new ArrayList<RsvpLspDto>();
        for (RsvpLspDto lsp : lsps) {
            boolean unmatched = false;
            for (String field : query.keySet()) {
                String value = RsvpLspRenderingUtil.rendering(lsp, field);
                if (!query.get(field).matches(value)) {
                    unmatched = true;
                    break;
                }
            }
            if (!unmatched) {
                result.add(lsp);
            }
        }
        return result;
    }

    public static PortDto getSrcPort(RsvpLspDto lsp) {
        RsvpLspHopSeriesDto activeHop = lsp.getActiveHopSeries();
        if (activeHop == null || activeHop.getHops() == null || activeHop.getHops().size() == 0) {
            return null;
        }
        List<PathHopDto> hops = activeHop.getHops();
        PathHopDto head = hops.get(0);
        return NodeUtil.getAssociatedPort(head.getSrcPort());
    }

    public static PortDto getDstPort(RsvpLspDto lsp) {
        RsvpLspHopSeriesDto activeHop = lsp.getActiveHopSeries();
        if (activeHop == null || activeHop.getHops() == null || activeHop.getHops().size() == 0) {
            return null;
        }
        List<PathHopDto> hops = activeHop.getHops();
        PathHopDto tail = hops.get(hops.size() - 1);
        return NodeUtil.getAssociatedPort(tail.getDstPort());
    }

    public static List<CommandBuilder> buildRsvpLspRemover(RsvpLspDto lsp, String userName)
            throws AuthenticationException, RemoteException, IOException, InventoryException,
            InstantiationException, IllegalAccessException, ExternalServiceException {
        List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
        RsvpLspCommandBuilder builder = new RsvpLspCommandBuilder(
                lsp, userName);
        builder.buildDeleteCommand();
        builders.add(builder);
        if (lsp.getHopSeries1() != null) {
            RsvpLspHopSeriesDto path = lsp.getHopSeries1();
            if (RsvpLspUtil.isLastUser(path, lsp)) {
                PathCommandBuilder builder1 = new PathCommandBuilder(path, userName);
                builder1.buildDeleteCommand();
                builders.add(builder1);
            }
        }
        if (lsp.getHopSeries2() != null) {
            RsvpLspHopSeriesDto path = lsp.getHopSeries2();
            if (RsvpLspUtil.isLastUser(path, lsp)) {
                PathCommandBuilder builder2 = new PathCommandBuilder(path, userName);
                builder2.buildDeleteCommand();
                builders.add(builder2);
            }
        }
        log.debug("remove completed: [" + RsvpLspRenderer.getLspName(lsp) + "]");
        return builders;
    }

    public static void executeTaskCancel(RsvpLspDto lsp, String userName)
            throws IOException, InventoryException, ExternalServiceException {
        MplsNmsCreationTaskCommandBuilder builder = new MplsNmsCreationTaskCommandBuilder(
                lsp, userName);
        builder.buildDeleteCommand();
        ShellConnector.getInstance().execute(builder);
        log.debug("cancel reservation completed: [" + RsvpLspRenderer.getLspName(lsp) + "]");
    }

    public static void executeGrouping(RsvpLspDto lsp1, RsvpLspDto lsp2, String userName)
            throws IOException, InventoryException, ExternalServiceException {
        LspCouplingCommandBuilder builder = new LspCouplingCommandBuilder(
                lsp1, lsp2, userName);
        builder.buildCommand();
        ShellConnector.getInstance().execute(builder);
        log.debug("grouping completed: lsp1[" +
                RsvpLspRenderer.getLspName(lsp1) + "]+lsp2[" + RsvpLspRenderer.getLspName(lsp2) + "]");

    }

    public static void executeSetFacilityStatus(RsvpLspDto lsp, FacilityStatus fs, String userName)
            throws IOException, InventoryException, ExternalServiceException {
        SpecificRsvpLspAttributeCommandBuilder builder = new SpecificRsvpLspAttributeCommandBuilder(
                lsp, userName);
        builder.setFacilityStatus(fs);
        builder.buildCommand();
        ShellConnector.getInstance().execute(builder);
        log.debug("update completed: [" + RsvpLspRenderer.getLspName(lsp) + "/" + fs + "]");
    }

}