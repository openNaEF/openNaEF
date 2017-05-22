package voss.model;


public interface RouterVlanIf extends VlanIf {
    void initRouterPort(LogicalEthernetPort port);

    LogicalEthernetPort getRouterPort();
}