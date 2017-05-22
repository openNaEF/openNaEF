package voss.nms.inventory.diff.network;

import java.io.Serializable;

public class PolicyViolation implements Serializable {
    private static final long serialVersionUID = 1L;

    public PolicyViolation() {
    }

    public PolicyViolation(String id, String nodeName, String ipAddress, String matter, String event) {
        this.inventoryID = id;
        this.nodeName = nodeName;
        this.ipAddress = ipAddress;
        this.matter = matter;
        this.eventContents = event;
    }

    public String nodeName = null;
    public String ipAddress = null;
    public String inventoryID = null;
    public String matter = null;
    public String eventContents = null;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("inventoryID=").append(inventoryID);
        sb.append(", nodeName=").append(nodeName);
        sb.append("(").append(ipAddress);
        sb.append(") matter=[").append(matter);
        sb.append("] eventContents=[").append(eventContents).append("]");
        return sb.toString();
    }
}