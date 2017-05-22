package voss.discovery.iolib.simpletelnet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.console.ConsoleException;
import voss.model.Protocol;

import java.io.*;
import java.net.*;

public class RealTelnetSocket extends RealTerminalSocket {
    private final static Logger log = LoggerFactory.getLogger(RealTelnetSocket.class);
    private static boolean isTestMode = false;
    private PushbackInputStream input;
    private Socket socket;

    public RealTelnetSocket(InetAddress inetAddress, int port, int timeout)
            throws SocketException {
        super.setIpAddress(inetAddress.getHostAddress());
        super.setPort(port);
        this.TIMEOUT_TIME = timeout;
    }

    public RealTelnetSocket(String ipAddress, int port, int timeout)
            throws SocketException {
        super.setIpAddress(ipAddress);
        super.setPort(port);
        this.TIMEOUT_TIME = timeout;
    }

    public byte read(int timeout) throws IOException, ConsoleException {
        socket.setSoTimeout(timeout);
        InputStream input = getInputStream();
        int c = -1;
        try {
            c = input.read();
        } catch (SocketTimeoutException ste) {
            throw new ReceivingTimeoutException();
        }
        if (c == -1) {
            throw new RemoteClosedException();
        }
        return (byte) c;
    }

    public void send(byte[] value) throws IOException {
        OutputStream out = socket.getOutputStream();
        out.write(value);
        out.flush();
    }

    public PushbackInputStream getInputStream() throws IOException {
        if (input == null) {
            input = new PushbackInputStream(socket.getInputStream(), 3);
        }
        return input;
    }

    public void connect() throws IOException, ConsoleException {
        if (getIpAddress() == null) {
            throw new IllegalStateException("ipAddress is null");
        }
        createSocket().connect(new InetSocketAddress(getIpAddress(), getPort()));
        negotiate();
        log.info("connected: " + getIpAddress() + ":" + getPort());
    }

    public void close() throws IOException {
        if (!socket.isClosed()) {
            socket.close();
        }
    }

    protected Socket createSocket() throws SocketException {
        if (isTestMode) {
            return socket;
        }
        socket = new Socket();
        input = null;
        socket.setSoTimeout(0);
        return socket;
    }

    public Protocol getProtocol() {
        return Protocol.TELNET;
    }

    public void negotiate() throws SocketException, ConsoleException,
            IOException, ReceivingTimeoutException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while (readIAC(bos)) {
            byte[] buffer = bos.toByteArray();
            log.debug("rcvd:" + VossTelnetOption.decodeNegotiation(buffer));

            if (isVt100() && VossTelnetOption.matches(buffer,
                    VossTelnetOption.ASK_TERMINAL_TYPE)) {
                send(VossTelnetOption.WILL_TERMINAL_TYPE);
                log.trace("send:" + VossTelnetOption
                        .decodeNegotiation(VossTelnetOption.WILL_TERMINAL_TYPE));

            } else if (isVt100() && VossTelnetOption.matches(buffer,
                    VossTelnetOption.ASK_TERMINAL_SUBOPTION)) {
                send(VossTelnetOption.BEGIN_TERMINAL_SUBOPTION_VT100);
                log.trace("send:"
                        + VossTelnetOption.decodeNegotiation(VossTelnetOption.BEGIN_TERMINAL_SUBOPTION_VT100));

                send(VossTelnetOption.END_SUBOPTION);
                log.trace("send:"
                        + VossTelnetOption.decodeNegotiation(VossTelnetOption.END_SUBOPTION));

            } else if (buffer[1] == VossTelnetOption.DO) {
                buffer[1] = VossTelnetOption.WONT;
                send(buffer);
                log.trace("send:" + VossTelnetOption.decodeNegotiation(buffer));
            }
            bos = new ByteArrayOutputStream();
        }
        bos.close();
    }

    private boolean readIAC(ByteArrayOutputStream bos) throws SocketException,
            ConsoleException, IOException, ReceivingTimeoutException {
        if (bos == null) {
            throw new IllegalArgumentException();
        }
        byte b1 = read(TIMEOUT_TIME);
        if (b1 != VossTelnetOption.IAC) {
            unread(b1);
            return false;
        }
        bos.write(b1);

        byte b2 = read(TIMEOUT_TIME);
        bos.write(b2);
        if (b2 == VossTelnetOption.SUBOPTION_BEGIN) {
            while (b2 != VossTelnetOption.SUBOPTION_END) {
                b2 = read(TIMEOUT_TIME);
                bos.write(b2);
            }
            return true;
        }
        byte b3 = read(TIMEOUT_TIME);
        bos.write(b3);
        return true;
    }

    private void unread(byte value) throws IOException {
        getInputStream().unread(value);
    }

}