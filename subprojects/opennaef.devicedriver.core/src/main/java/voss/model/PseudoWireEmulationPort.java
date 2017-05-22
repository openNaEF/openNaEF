package voss.model;


public interface PseudoWireEmulationPort extends LogicalPort {
    void initPseudoWirePort(PseudoWirePort pw);

    PseudoWirePort getPseudWirePort();

}