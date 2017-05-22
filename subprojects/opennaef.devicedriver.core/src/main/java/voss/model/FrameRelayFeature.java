package voss.model;

import java.util.List;

public interface FrameRelayFeature extends Feature {
    void initPhysicalPort(SerialPort phy);

    boolean hasEndPoint(FrameRelayDLCIEndPoint ep);

    void addEndPoint(FrameRelayDLCIEndPoint ep);

    boolean removeEndPoint(FrameRelayDLCIEndPoint ep);

    FrameRelayDLCIEndPoint getEndPoint(int dlci);

    List<FrameRelayDLCIEndPoint> getEndPoints();

    void setEncapsulationType(FrameRelayEncapsulationType mode);

    FrameRelayEncapsulationType getEncapsulationType();

    public static enum FrameRelayEncapsulationType {
        CISCO,
        IETF,;
    }
}