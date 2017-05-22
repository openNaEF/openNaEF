package voss.multilayernms.inventory.renderer;

import java.io.Serializable;

@SuppressWarnings("serial")
public class HopUnit implements Serializable {
    private final int hop;
    private final String hopSourceName;
    private final String hopSourceIP;
    private final String hopDestName;
    private final String hopDestIP;

    public HopUnit(int hop, String hopSourceName, String hopSourceIP,
                   String hopDestName, String hopDestIP) {
        this.hop = hop;
        this.hopSourceName = hopSourceName;
        this.hopSourceIP = hopSourceIP;
        this.hopDestName = hopDestName;
        this.hopDestIP = hopDestIP;
    }

    public String getHopCount() {
        return String.valueOf(hop);
    }

    public int getHop() {
        return this.hop;
    }

    public String getHopSourceName() {
        return this.hopSourceName;
    }

    public String getHopDestName() {
        return this.hopDestName;
    }

    public String getHopSourceIP() {
        return hopSourceIP;
    }

    public String getHopDestIP() {
        return hopDestIP;
    }
}