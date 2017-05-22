package voss.model;


import java.net.InetAddress;

@SuppressWarnings("serial")
public class PseudoWirePortImpl extends AbstractLogicalPort implements PseudoWirePort {
    private long vcIndex;
    private String pwName;
    private PseudoWireType type;
    private InetAddress peerIpAddress;
    private Long pseudoWireID;
    private long peerPwId;
    private PseudoWireOperStatus adminStatus;
    private PseudoWireOperStatus operStatus;
    private Port attachedCircuitPort;
    private PseudoWireEmulationPort emulationPort;
    private MplsTunnel transmitLsp = null;
    private String receiveLspName = null;
    private AlcatelPipeType alcatelPipeType = null;
    private boolean controlWord = false;
    private int alcatelSdpId;

    private transient PseudoWirePort remotePseudoWirePort = null;

    public L2VpnType getVpnType() {
        return L2VpnType.PWE3;
    }

    public synchronized void setVcIndex(long vcIndex) {
        this.vcIndex = vcIndex;
    }

    public synchronized long getVcIndex() {
        return this.vcIndex;
    }

    public synchronized void initPseudoWireID(long ID) {
        if (this.pseudoWireID != null) {
            throw new AlreadyInitializedException(this.pseudoWireID, ID);
        }
        this.pseudoWireID = Long.valueOf(ID);
    }

    public synchronized long getPseudoWireID() {
        if (this.pseudoWireID == null) {
            throw new NotInitializedException();
        }
        return this.pseudoWireID.longValue();
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
        if (status != null) {
            super.setOperationalStatus(status.name());
        }
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
        sb.append(":PW=").append(this.pseudoWireID.intValue());
        sb.append("(VcIndex:").append(this.vcIndex).append(")");
        sb.append(", ifName='").append(getIfName());
        sb.append("', name=").append(this.pwName);
        sb.append(", type=").append(this.type.toString());
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
        return null;
    }

    public void setRouteDistinguisher(String rd) {
    }

    public String getRouteTarget() {
        return null;
    }

    public void setRouteTarget(String rt) {
    }

    public String getSiteName() {
        return null;
    }

    public void setSiteName(String site) {
    }

    @Override
    public AlcatelPipeType getAlcatelPipeType() {
        return alcatelPipeType;
    }

    @Override
    public void setAlcatelPipeType(AlcatelPipeType alcatelPipeType) {
        this.alcatelPipeType = alcatelPipeType;
    }

    @Override
    public boolean hasControlWord() {
        return this.controlWord;
    }

    @Override
    public void setControlWord(boolean value) {
        this.controlWord = value;
    }

    public synchronized int getAlcatelSdpId() {
        return alcatelSdpId;
    }

    public synchronized void setAlcatelSdpId(int alcatelSdpId) {
        this.alcatelSdpId = alcatelSdpId;
    }

}