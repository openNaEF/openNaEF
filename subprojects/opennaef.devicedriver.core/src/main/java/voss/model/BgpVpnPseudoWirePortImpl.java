package voss.model;


import java.net.InetAddress;

@SuppressWarnings("serial")
public class BgpVpnPseudoWirePortImpl extends AbstractLogicalPort implements PseudoWirePort {
    private String pwName;
    private String routeDistinguisher;
    private String routeTarget;
    private String siteName;
    private PseudoWireType type;
    private InetAddress peerIpAddress;
    private long peerPwId;
    private PseudoWireOperStatus adminStatus;
    private PseudoWireOperStatus operStatus;
    private Port attachedCircuitPort;
    private PseudoWireEmulationPort emulationPort;
    private MplsTunnel transmitLsp = null;
    private String receiveLspName = null;
    private boolean controlWord = false;

    private transient PseudoWirePort remotePseudoWirePort = null;

    public L2VpnType getVpnType() {
        return L2VpnType.BGP_VPN;
    }

    public synchronized void setVcIndex(long vcIndex) {
    }

    public synchronized long getVcIndex() {
        return 0;
    }

    public synchronized void initPseudoWireID(long ID) {
    }

    public synchronized long getPseudoWireID() {
        return 0;
    }

    public synchronized void setPwName(String name) {
        this.pwName = name;
    }

    public synchronized String getPwName() {
        return this.pwName;
    }

    public synchronized InetAddress getPeerIpAddress() {
        return peerIpAddress;
    }

    public synchronized void setPeerIpAddress(InetAddress peerIpAddress) {
        this.peerIpAddress = peerIpAddress;
    }

    public synchronized long getPeerPwId() {
        return peerPwId;
    }

    public synchronized void setPeerPwId(long peerId) {
        this.peerPwId = peerId;
    }

    public synchronized void setPseudoWireType(PseudoWireType type) {
        this.type = type;
    }

    public synchronized PseudoWireType getType() {
        return this.type;
    }

    public synchronized void setPseudoWireAdminStatus(PseudoWireOperStatus status) {
        this.adminStatus = status;
    }

    public synchronized PseudoWireOperStatus getPseudoWireAdminStatus() {
        return this.adminStatus;
    }

    public synchronized void setPseudoWireOperStatus(PseudoWireOperStatus status) {
        this.operStatus = status;
    }

    public synchronized PseudoWireOperStatus getPseudoWireOperStatus() {
        return this.operStatus;
    }

    public synchronized void setEmulationPort(PseudoWireEmulationPort emulationPort) {
        this.emulationPort = emulationPort;
    }

    public synchronized PseudoWireEmulationPort getEmulationPort() {
        return this.emulationPort;
    }

    public synchronized void setNeighbor(PseudoWirePort remote) {
        this.remotePseudoWirePort = remote;
    }

    public synchronized PseudoWirePort getNeighbor() {
        return this.remotePseudoWirePort;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getDevice().getDeviceName()).append(":");
        sb.append(getVpnType().name());
        sb.append(":PWname=").append(this.pwName);
        sb.append(", rd='").append(getRouteDistinguisher());
        sb.append(", rt='").append(getRouteTarget());
        sb.append(", ifName='").append(getIfName());
        sb.append("', type=").append(this.type.toString());
        sb.append(", Peer=").append(this.peerIpAddress.getHostAddress()).append(":" + this.peerPwId);
        sb.append(", adminStatus=").append(this.getAdminStatus());
        sb.append(", operStatus=").append(this.getOperationalStatus());
        return sb.toString();
    }


    public synchronized Port getAttachedCircuitPort() {
        return this.attachedCircuitPort;
    }

    public synchronized void setAttachedCircuitPort(Port port) {
        this.attachedCircuitPort = port;
    }

    public MplsTunnel getTransmitLsp() {
        return transmitLsp;
    }

    public void setTransmitLsp(MplsTunnel tranmitLsp) {
        this.transmitLsp = tranmitLsp;
    }

    public String getReceiveLspName() {
        return receiveLspName;
    }

    public void setReceiveLspName(String receiveLspName) {
        this.receiveLspName = receiveLspName;
    }


    public String getRouteDistinguisher() {
        return this.routeDistinguisher;
    }

    public void setRouteDistinguisher(String rd) {
        this.routeDistinguisher = rd;
    }

    public String getRouteTarget() {
        return this.routeTarget;
    }

    public void setRouteTarget(String rt) {
        this.routeTarget = rt;
    }

    public String getSiteName() {
        return this.siteName;
    }

    public void setSiteName(String site) {
        this.siteName = site;
    }

    @Override
    public AlcatelPipeType getAlcatelPipeType() {
        return null;
    }

    @Override
    public void setAlcatelPipeType(AlcatelPipeType pipeType) {
    }

    @Override
    public boolean hasControlWord() {
        return this.controlWord;
    }

    @Override
    public void setControlWord(boolean value) {
        this.controlWord = value;
    }

    @Override
    public int getAlcatelSdpId() {
        return 0;
    }

    @Override
    public void setAlcatelSdpId(int id) {
    }
}