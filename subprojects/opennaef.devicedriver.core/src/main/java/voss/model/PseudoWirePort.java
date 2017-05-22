package voss.model;


import java.net.InetAddress;

public interface PseudoWirePort extends LogicalPort {

    L2VpnType getVpnType();

    void setVcIndex(long vcIndex);

    long getVcIndex();

    void setPwName(String name);

    String getPwName();

    InetAddress getPeerIpAddress();

    void setPeerIpAddress(InetAddress peerIpAddress);

    long getPseudoWireID();

    void initPseudoWireID(long pwID);

    long getPeerPwId();

    void setPeerPwId(long peerId);

    void setPseudoWireType(PseudoWireType type);

    PseudoWireType getType();

    void setAlcatelPipeType(AlcatelPipeType pipeType);

    AlcatelPipeType getAlcatelPipeType();

    void setPseudoWireAdminStatus(PseudoWireOperStatus status);

    PseudoWireOperStatus getPseudoWireAdminStatus();

    void setPseudoWireOperStatus(PseudoWireOperStatus status);

    PseudoWireOperStatus getPseudoWireOperStatus();

    void setEmulationPort(PseudoWireEmulationPort emulationPort);

    PseudoWireEmulationPort getEmulationPort();

    void setNeighbor(PseudoWirePort remote);

    PseudoWirePort getNeighbor();

    Port getAttachedCircuitPort();

    void setAttachedCircuitPort(Port port);

    MplsTunnel getTransmitLsp();

    void setTransmitLsp(MplsTunnel lsp);

    String getReceiveLspName();

    void setReceiveLspName(String lsp);

    String getRouteDistinguisher();

    void setRouteDistinguisher(String rd);

    String getRouteTarget();

    void setRouteTarget(String rt);

    String getSiteName();

    void setSiteName(String site);

    boolean hasControlWord();

    void setControlWord(boolean value);

    int getAlcatelSdpId();

    void setAlcatelSdpId(int id);
}