package voss.model;

public enum Protocol {
    SNMP_V1(161, "SNMPv1"),
    SNMP_V2C_GETNEXT(161, "SNMPv2"),
    SNMP_V2C_GETBULK(161, "SNMPv2(Bulk)"),
    TELNET(23, "TELNET"),
    SSH2(22, "SSH"),
    SSH2_INTERACTIVE(22, "SSH(KeyboardInteractive)"),
    SSH2_PUBLICKEY(22, "SSH(PublicKey)"),
    FTP(21, "FTP"),
    TFTP(69, "TFTP"),
    UNKNOWN(0, "??"),;

    public final int defaultPort;
    public final String caption;

    private Protocol(int port, String caption) {
        this.defaultPort = port;
        this.caption = caption;
    }

    public int getDefaultPort() {
        return this.defaultPort;
    }

    public static Protocol getByCaption(String caption) {
        for (Protocol instance : values()) {
            if (instance.caption.equals(caption)) {
                return instance;
            }
        }
        return UNKNOWN;
    }
}