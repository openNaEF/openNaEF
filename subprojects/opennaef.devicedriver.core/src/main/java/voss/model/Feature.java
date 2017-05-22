package voss.model;


public interface Feature extends LogicalPort {
    Port getParentPort();

    void setParentPort(Port port);

}