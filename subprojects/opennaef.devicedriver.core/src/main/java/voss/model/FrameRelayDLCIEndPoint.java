package voss.model;


public interface FrameRelayDLCIEndPoint extends LogicalPort {
    void setDLCI(int dlci);

    int getDLCI();

    void initParentPort(FrameRelayFeature parent);

    FrameRelayFeature getParentPort();
}