package voss.model;


import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class MplsTunnel extends AbstractLogicalPort {
    private static final long serialVersionUID = 1L;

    private MplsTunnelKey key;
    Map<Integer, LabelSwitchedPathEndPoint> memberHops = new HashMap<Integer, LabelSwitchedPathEndPoint>();

    private LabelSwitchedPathEndPoint activeHops = null;

    Map<BigInteger, LspHopSeries> temporaryHops = new HashMap<BigInteger, LspHopSeries>();

    private String from;
    private String to;
    private String destinationIpAddress;
    private boolean fastReroute = false;
    private boolean mergePermitted = false;
    private boolean isPersistent = false;
    private boolean isPinned = false;
    private boolean isComputed = false;
    private boolean recordRoute = false;

    public void setTunnelKey(MplsTunnelKey key) {
        this.key = key;
    }

    public MplsTunnelKey getKey() {
        return this.key;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setTunnelDestinationIpAddress(String ipAddress) {
        assert ipAddress != null;
        this.destinationIpAddress = ipAddress;
    }

    public String getTunnelDestinationIpAddress() {
        assert this.destinationIpAddress != null;
        return this.destinationIpAddress;
    }

    public LabelSwitchedPathEndPoint getActiveHops() {
        return this.activeHops;
    }

    public void setActiveHops(LabelSwitchedPathEndPoint activeHops) {
        LabelSwitchedPathEndPoint found = null;
        for (LabelSwitchedPathEndPoint hops : this.memberHops.values()) {
            if (hops.equals(activeHops)) {
                found = hops;
            }
        }
        if (found == null) {
            throw new IllegalArgumentException("activeHops is not member of memberHops.");
        }
        ;
        this.activeHops = activeHops;
    }

    public void addMemberLsp(Integer priority, LabelSwitchedPathEndPoint lsp) {
        if (priority == null || lsp == null) {
            throw new IllegalArgumentException();
        }
        memberHops.put(priority, lsp);
    }

    public Map<Integer, LabelSwitchedPathEndPoint> getMemberLsps() {
        Map<Integer, LabelSwitchedPathEndPoint> result = new HashMap<Integer, LabelSwitchedPathEndPoint>();
        result.putAll(memberHops);
        return result;
    }

    public LabelSwitchedPathEndPoint getHopsByName(String ifName) {
        for (LabelSwitchedPathEndPoint hops : this.memberHops.values()) {
            if (hops.getIfName() != null && hops.getIfName().equals(ifName)) {
                return hops;
            }
        }
        return null;
    }

    public void putLspHopSeries(BigInteger priority, LspHopSeries hops) {
        this.temporaryHops.put(priority, hops);
    }

    public Map<BigInteger, LspHopSeries> getLspHopSeries() {
        return this.temporaryHops;
    }

    public void clearMemberLsps() {
        this.memberHops.clear();
    }

    public boolean isFastReroute() {
        return fastReroute;
    }

    public void setFastReroute(boolean fastReroute) {
        this.fastReroute = fastReroute;
    }

    public boolean isMergePermitted() {
        return mergePermitted;
    }

    public void setMergePermitted(boolean mergePermitted) {
        this.mergePermitted = mergePermitted;
    }

    public boolean isPersistent() {
        return isPersistent;
    }

    public void setPersistent(boolean isPersistent) {
        this.isPersistent = isPersistent;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean isPinned) {
        this.isPinned = isPinned;
    }

    public boolean isComputed() {
        return isComputed;
    }

    public void setComputed(boolean isComputed) {
        this.isComputed = isComputed;
    }

    public boolean isRecordRoute() {
        return recordRoute;
    }

    public void setRecordRoute(boolean recordRoute) {
        this.recordRoute = recordRoute;
    }

}