package voss.model;

import java.io.Serializable;

public class ProtocolPort implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final ProtocolPort TELNET = new ProtocolPort(Protocol.TELNET, 23);
    public static final ProtocolPort SSH2 = new ProtocolPort(Protocol.SSH2, 22);
    public static final ProtocolPort FTP = new ProtocolPort(Protocol.FTP, 21);
    public static final ProtocolPort TFTP = new ProtocolPort(Protocol.TFTP, 69);
    public static final ProtocolPort SNMP_V1 = new ProtocolPort(Protocol.SNMP_V1, 161);
    public static final ProtocolPort SNMP_V2C_GETBULK = new ProtocolPort(Protocol.SNMP_V2C_GETBULK, 161);
    public static final ProtocolPort SNMP_V2C_GETNEXT = new ProtocolPort(Protocol.SNMP_V2C_GETNEXT, 161);

    private int portNumber;
    private Protocol protocol;

    public ProtocolPort() {
    }

    public ProtocolPort(final Protocol protocol) {
        setProtocol(protocol);
        setPort(protocol.getDefaultPort());
    }

    public ProtocolPort(final Protocol protocol, final int port) {
        setPort(port);
        setProtocol(protocol);
    }

    public int getPort() {
        return portNumber;
    }

    public void setPort(final int port) {
        this.portNumber = port;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    @Override
    public String toString() {
        return this.protocol.toString() + ":" + this.portNumber;
    }

}