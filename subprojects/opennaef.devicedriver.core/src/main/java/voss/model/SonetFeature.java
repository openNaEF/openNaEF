package voss.model;

import java.util.List;


public interface SonetFeature extends FeatureCapability, LogicalPort {
    SonetClass getSonetClass();

    void setSonetClass(SonetClass sonetClass);

    List<SerialPort> getCircuitSerialPorts();

    void addSerialPort(SerialPort circuit);

    void addSerialPorts(List<SerialPort> circuits);

    void clearSerialPorts();
}