package voss.model;


public interface SerialPort extends LogicalPort, FeatureCapability {
    boolean isAsyncMode();

    void setAsyncMode(boolean async);
}