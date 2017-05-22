package voss.model;

import java.util.List;


public interface VplsInstance extends LogicalPort {
    void initVplsID(String id);

    String getVplsID();

    void addAttachmentPort(Port port);

    boolean hasAttachmentPort(Port port);

    void removeAttachmentPort(Port port);

    List<Port> getAttachmentPorts();
}