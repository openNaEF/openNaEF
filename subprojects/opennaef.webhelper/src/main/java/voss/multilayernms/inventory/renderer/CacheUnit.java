package voss.multilayernms.inventory.renderer;

import naef.dto.NetworkDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspDto;
import naef.ui.NaefDtoFacade;
import org.slf4j.LoggerFactory;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.constants.PseudoWireType;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.NodeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class CacheUnit {
    private final List<PseudowireDto> pseudowires;
    private final long cesAggregated;
    private final long etherVpnAggregated;

    public CacheUnit(RsvpLspDto lsp) {
        this.pseudowires = getPseudoWiresOn(lsp);
        this.cesAggregated = getAggregatedBandwdithBy(PseudoWireType.CPIPE);
        this.etherVpnAggregated = getAggregatedBandwdithBy(PseudoWireType.EPIPE);
        LoggerFactory.getLogger(CacheUnit.class).debug("* cesAgg=" + this.cesAggregated + ", ethAgg=" + this.etherVpnAggregated);
    }

    public CacheUnit(PortDto port) {
        this.pseudowires = getPseudoWiresOn(port);
        this.cesAggregated = getAggregatedBandwdithBy(PseudoWireType.CPIPE);
        this.etherVpnAggregated = getAggregatedBandwdithBy(PseudoWireType.EPIPE);
        LoggerFactory.getLogger(CacheUnit.class).debug("* cesAgg=" + this.cesAggregated + ", ethAgg=" + this.etherVpnAggregated);
    }

    public CacheUnit(List<PseudowireDto> pseudowires, PortDto port) {
        this.pseudowires = pseudowires;
        this.cesAggregated = getAggregatedBandwdithBy(PseudoWireType.CPIPE);
        this.etherVpnAggregated = getAggregatedBandwdithBy(PseudoWireType.EPIPE);
        LoggerFactory.getLogger(CacheUnit.class).debug("* cesAgg=" + this.cesAggregated + ", ethAgg=" + this.etherVpnAggregated);
    }

    public long getGreenAggregated() {
        return cesAggregated + etherVpnAggregated;
    }

    public long getCesAggregated() {
        return cesAggregated;
    }

    public long getEtherVpnAggregated() {
        return etherVpnAggregated;
    }

    private long getAggregatedBandwdithBy(PseudoWireType type) {
        long total = 0L;
        List<PseudowireDto> pseudowires = getPseudoWiresBy(type);
        for (PseudowireDto pw : pseudowires) {
            Long band = PseudoWireRenderer.getBandwidthAsLong(pw);
            if (band != null) {
                total += band.longValue();
            }
        }
        return total;
    }

    private List<PseudowireDto> getPseudoWiresOn(PortDto port) {
        List<PseudowireDto> result = new ArrayList<PseudowireDto>();
        IpIfDto ip = NodeUtil.getIpOn(port);
        if (ip == null) {
            return result;
        }
        try {
            NaefDtoFacade facade = DtoUtil.getNaefDtoFacade(port);
            Set<NetworkDto> networks = facade.getPortNetworks(ip);
            for (NetworkDto network : networks) {
                if (network instanceof PseudowireDto) {
                    result.add((PseudowireDto) network);
                }
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<PseudowireDto> getPseudoWiresOn(RsvpLspDto lsp) {
        List<PseudowireDto> result = new ArrayList<PseudowireDto>();
        if (lsp == null) {
            return result;
        }
        result.addAll(lsp.getPseudowires());
        return result;
    }

    private List<PseudowireDto> getPseudoWiresBy(PseudoWireType pwType) {
        if (pwType == null) {
            throw new IllegalArgumentException();
        }
        List<PseudowireDto> result = new ArrayList<PseudowireDto>();
        for (PseudowireDto pw : this.pseudowires) {
            PseudoWireType type;
            try {
                String _type = PseudoWireRenderer.getPseudoWireType(pw);
                if (_type == null) {
                    continue;
                }
                type = PseudoWireType.valueOf(_type);
                if (type == pwType) {
                    result.add(pw);
                }
            } catch (IllegalArgumentException e) {
                LoggerFactory.getLogger(RsvpLspRenderer.class)
                        .warn("no [" + MPLSNMS_ATTR.PSEUDOWIRE_TYPE + "] on " + pw.getAbsoluteName(), e);
                continue;
            }
        }
        return result;
    }

}