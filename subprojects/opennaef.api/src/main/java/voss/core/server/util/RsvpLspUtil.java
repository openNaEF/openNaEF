package voss.core.server.util;

import naef.dto.NodeDto;
import naef.dto.PathHopDto;
import naef.dto.PortDto;
import naef.dto.mpls.*;
import naef.ui.NaefDtoFacade;
import tef.skelton.dto.EntityDto;
import voss.core.server.database.CoreConnector;
import voss.core.server.naming.inventory.InventoryIdCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RsvpLspUtil {

    public static RsvpLspIdPoolDto getIdPool(String name) {
        try {
            CoreConnector conn = CoreConnector.getInstance();
            NaefDtoFacade facade = conn.getDtoFacade();
            Set<RsvpLspIdPoolDto> pools = facade.getRootIdPools(RsvpLspIdPoolDto.class);
            for (RsvpLspIdPoolDto pool : pools) {
                if (pool.getName() != null && pool.getName().equals(name)) {
                    return pool;
                }
            }
            return null;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static RsvpLspHopSeriesIdPoolDto getHopSeriesIdPool(String name) {
        try {
            CoreConnector conn = CoreConnector.getInstance();
            NaefDtoFacade facade = conn.getDtoFacade();
            Set<RsvpLspHopSeriesIdPoolDto> pools = facade.getRootIdPools(RsvpLspHopSeriesIdPoolDto.class);
            for (RsvpLspHopSeriesIdPoolDto pool : pools) {
                if (pool.getName() != null && pool.getName().equals(name)) {
                    return pool;
                }
            }
            return null;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static RsvpLspDto getRsvpLsp(String name) {
        try {
            CoreConnector conn = CoreConnector.getInstance();
            NaefDtoFacade facade = conn.getDtoFacade();
            Set<RsvpLspIdPoolDto> pools = facade.getRootIdPools(RsvpLspIdPoolDto.class);
            for (RsvpLspIdPoolDto pool : pools) {
                for (RsvpLspDto rsvpLsp : pool.getUsers()) {
                    if (rsvpLsp.getName() != null && rsvpLsp.getName().equals(name)) {
                        return rsvpLsp;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static RsvpLspDto getRsvpLsp(RsvpLspIdPoolDto pool, String name) {
        try {
            for (RsvpLspDto rsvpLsp : pool.getUsers()) {
                if (rsvpLsp.getName() != null && rsvpLsp.getName().equals(name)) {
                    return rsvpLsp;
                }
            }
            return null;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static List<RsvpLspDto> getRsvpLsps() {
        try {
            List<RsvpLspDto> results = new ArrayList<RsvpLspDto>();
            CoreConnector conn = CoreConnector.getInstance();
            NaefDtoFacade facade = conn.getDtoFacade();
            Set<RsvpLspIdPoolDto> pools = facade.getRootIdPools(RsvpLspIdPoolDto.class);
            for (RsvpLspIdPoolDto pool : pools) {
                for (RsvpLspDto rsvpLsp : pool.getUsers()) {
                    if (rsvpLsp.getName() != null) {
                        results.add(rsvpLsp);
                    }
                }
            }
            return results;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static List<RsvpLspDto> getRsvpLspsOn(PortDto port) {
        try {
            CoreConnector conn = CoreConnector.getInstance();
            NaefDtoFacade facade = conn.getDtoFacade();
            Set<RsvpLspIdPoolDto> pools = facade.getRootIdPools(RsvpLspIdPoolDto.class);
            List<RsvpLspDto> result = new ArrayList<RsvpLspDto>();
            for (RsvpLspIdPoolDto pool : pools) {
                NEXT_LSP:
                for (RsvpLspDto rsvpLsp : pool.getUsers()) {
                    RsvpLspHopSeriesDto path = rsvpLsp.getHopSeries1();
                    for (PathHopDto hop : path.getHops()) {
                        if (DtoUtil.mvoEquals(port, hop.getSrcPort())) {
                            result.add(rsvpLsp);
                            continue NEXT_LSP;
                        } else if (DtoUtil.mvoEquals(port, hop.getDstPort())) {
                            result.add(rsvpLsp);
                            continue NEXT_LSP;
                        }
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static List<PseudowireDto> getPseudowiresOn(RsvpLspDto lsp) {
        List<PseudowireDto> pws = new ArrayList<PseudowireDto>();
        if (lsp == null) {
            return pws;
        }
        pws.addAll(lsp.getPseudowires());
        return pws;
    }

    public static List<RsvpLspDto> getLspsUnder(PseudowireDto pw) {
        List<RsvpLspDto> pws = new ArrayList<RsvpLspDto>();
        if (pw == null) {
            return pws;
        }
        pws.addAll(pw.getRsvpLsps());
        return pws;
    }

    public static List<RsvpLspHopSeriesDto> getPathesUnder(RsvpLspDto lsp) {
        List<RsvpLspHopSeriesDto> result = new ArrayList<RsvpLspHopSeriesDto>();
        if (lsp == null) {
            return result;
        }
        if (lsp.getHopSeries1() != null) {
            result.add(lsp.getHopSeries1());
        }
        if (lsp.getHopSeries2() != null) {
            result.add(lsp.getHopSeries2());
        }
        return result;
    }

    public static List<RsvpLspDto> getLspsOn(RsvpLspHopSeriesDto path) {
        List<RsvpLspDto> result = new ArrayList<RsvpLspDto>();
        if (path == null) {
            return result;
        }
        result.addAll(path.getRsvpLsps());
        return result;
    }

    public static NodeDto getEgressNode(RsvpLspDto lsp) {
        RsvpLspHopSeriesDto path = lsp.getHopSeries1();
        if (path == null) {
            path = lsp.getHopSeries2();
        }
        if (path == null) {
            return null;
        }
        return getEgressNode(path);
    }

    public static NodeDto getIngressNode(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        EntityDto.Desc<NodeDto> desc = lsp.get(RsvpLspDto.ExtAttr.INGRESS_NODE);
        if (desc == null) {
            return null;
        }
        return lsp.toDto(desc);
    }

    public static NodeDto getEgressNode(RsvpLspHopSeriesDto path) {
        List<PathHopDto> hops = path.getHops();
        if (hops == null || hops.size() == 0) {
            return null;
        }
        PathHopDto lastHop = hops.get(path.getHops().size() - 1);
        if (lastHop == null) {
            return null;
        } else if (lastHop.getSrcPort() == null) {
            throw new IllegalStateException();
        }
        return lastHop.getDstPort().getNode();
    }

    public static NodeDto getIngressNode(RsvpLspHopSeriesDto path) {
        List<PathHopDto> hops = path.getHops();
        if (hops == null || hops.size() == 0) {
            return null;
        }
        PathHopDto firstHop = hops.get(0);
        if (firstHop == null) {
            return null;
        } else if (firstHop.getSrcPort() == null) {
            throw new IllegalStateException();
        }
        return firstHop.getSrcPort().getNode();
    }

    public static RsvpLspHopSeriesDto getRsvpLspHopSeries(RsvpLspHopSeriesIdPoolDto pool, String name) {
        try {
            for (RsvpLspHopSeriesDto rsvpLsp : pool.getUsers()) {
                if (rsvpLsp.getName() != null && rsvpLsp.getName().equals(name)) {
                    return rsvpLsp;
                }
            }
            return null;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static boolean isPathBetween(RsvpLspHopSeriesDto path, NodeDto ingress, NodeDto egress) {
        if (Util.isNull(path, ingress, egress)) {
            throw new IllegalArgumentException("path, ingress, egress has null.");
        }
        List<PathHopDto> hops = path.getHops();
        if (hops.size() == 0) {
            return false;
        }
        PortDto firstHopPort = hops.get(0).getSrcPort();
        PortDto lastHopPort = hops.get(hops.size() - 1).getDstPort();
        return DtoUtil.isSameMvoEntity(firstHopPort.getNode(), ingress) &&
                DtoUtil.isSameMvoEntity(lastHopPort.getNode(), egress);
    }

    public static boolean isPathChanged(RsvpLspHopSeriesDto before, RsvpLspHopSeriesDto after) {
        if (before == null && after == null) {
            return false;
        } else if (before == null || after == null) {
            return true;
        }
        if (!before.getName().equals(after.getName())) {
            return true;
        }
        List<PathHopDto> beforeHops = before.getHops();
        List<PathHopDto> afterHops = after.getHops();
        if (beforeHops.size() != afterHops.size()) {
            return true;
        }
        for (int i = 0; i < beforeHops.size(); i++) {
            PathHopDto beforeHop = beforeHops.get(i);
            PathHopDto afterHop = afterHops.get(i);
            if (!DtoUtil.isSameMvoEntity(beforeHop.getSrcPort(), afterHop.getSrcPort())
                    || !DtoUtil.isSameMvoEntity(beforeHop.getDstPort(), afterHop.getDstPort())) {
                return true;
            }
        }
        return false;
    }

    public static RsvpLspHopSeriesDto getPath(RsvpLspHopSeriesIdPoolDto pool, String pathName) {
        if (pool == null) {
            throw new IllegalArgumentException("pool is null.");
        }
        for (RsvpLspHopSeriesDto user : pool.getUsers()) {
            if (user.getName().equals(pathName)) {
                return user;
            }
        }
        return null;
    }

    public static boolean isLastUser(RsvpLspHopSeriesDto path, RsvpLspDto lsp) {
        if (path == null) {
            return false;
        }
        Set<RsvpLspDto> users = path.getRsvpLsps();
        if (users != null && users.size() == 1) {
            RsvpLspDto user = users.iterator().next();
            if (DtoUtil.isSameMvoEntity(lsp, user)) {
                return true;
            }
        }
        return false;
    }

    public static RsvpLspDto getRsvpLspByInventoryID(RsvpLspIdPoolDto pool, String inventoryID) {
        try {
            for (RsvpLspDto rsvpLsp : pool.getUsers()) {
                String id = InventoryIdCalculator.getId(rsvpLsp);
                if (id.equals(inventoryID)) {
                    return rsvpLsp;
                }
            }
            return null;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static boolean hasPath(RsvpLspDto lsp, String pathName) {
        if (lsp.getHopSeries1() != null && lsp.getHopSeries1().getName().equals(pathName)) {
            return true;
        } else if (lsp.getHopSeries2() != null && lsp.getHopSeries2().getName().equals(pathName)) {
            return true;
        }
        return false;
    }

    public static boolean isLspBetween(RsvpLspDto lsp, NodeDto ingress, NodeDto egress) {
        if (Util.isNull(lsp, ingress, egress)) {
            throw new IllegalArgumentException("arg has null.");
        }
        RsvpLspHopSeriesDto primary = lsp.getHopSeries1();
        if (primary == null) {
            return false;
        } else if (!RsvpLspUtil.isPathBetween(primary, ingress, egress)) {
            return false;
        }
        RsvpLspHopSeriesDto secondary = lsp.getHopSeries2();
        if (secondary == null) {
            return false;
        } else if (!RsvpLspUtil.isPathBetween(secondary, ingress, egress)) {
            return false;
        }
        return true;
    }

    public static boolean isSamePathConfiguration(RsvpLspDto lsp1, RsvpLspDto lsp2) {
        if (lsp1 == null || lsp2 == null) {
            throw new IllegalStateException();
        }
        return isSamePathConfiguration(lsp1.getHopSeries1(), lsp2.getHopSeries1())
                && isSamePathConfiguration(lsp1.getHopSeries2(), lsp2.getHopSeries2());
    }

    public static boolean isSamePathConfiguration(RsvpLspHopSeriesDto path1, RsvpLspHopSeriesDto path2) {
        if (path1 == null && path2 == null) {
            return true;
        } else if (path1 != null && path2 != null) {
            return true;
        } else {
            return false;
        }
    }
}