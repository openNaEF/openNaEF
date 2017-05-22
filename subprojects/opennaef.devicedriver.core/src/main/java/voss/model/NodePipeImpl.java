package voss.model;


public class NodePipeImpl<T extends Port> extends AbstractLogicalPort implements NodePipe<T> {
    private static final long serialVersionUID = 1L;
    private String pipeName;

    public void setPipeName(String name) {
        this.pipeName = name;
    }

    public String getPipeName() {
        return this.pipeName;
    }

    private T attachmentCircuit1 = null;
    private T attachmentCircuit2 = null;

    public T getAttachmentCircuit1() {
        return attachmentCircuit1;
    }

    public void setAttachmentCircuit1(T attachmentCircuit1) {
        this.attachmentCircuit1 = attachmentCircuit1;
    }

    public T getAttachmentCircuit2() {
        return attachmentCircuit2;
    }

    public void setAttachmentCircuit2(T attachmentCircuit2) {
        this.attachmentCircuit2 = attachmentCircuit2;
    }

}