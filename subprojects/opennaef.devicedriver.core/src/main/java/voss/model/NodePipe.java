package voss.model;


public interface NodePipe<T extends Port> extends LogicalPort {
    String getPipeName();

    void setPipeName(String name);

    T getAttachmentCircuit1();

    void setAttachmentCircuit1(T port);

    T getAttachmentCircuit2();

    void setAttachmentCircuit2(T port);
}