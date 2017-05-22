package voss.discovery.iolib.simpletelnet;

import voss.discovery.iolib.console.ConsoleException;
import voss.model.Protocol;

import java.io.IOException;
import java.net.SocketException;

public abstract class TerminalSocket {

    private static final MoreExecutor NULL_MORE_EXECUTOR = new NullMoreExecutor();
    private int port = 23;
    private String ipAddress;

    public abstract String receiveTo(String toString, MoreExecutor more)
            throws SocketException, ConsoleException, IOException,
            ReceivingTimeoutException;

    public abstract ReceiveResult receiveTo(String[] toString, MoreExecutor more)
            throws SocketException, ConsoleException, IOException,
            ReceivingTimeoutException;

    protected abstract String receiveToPossible(MoreExecutor more)
            throws SocketException, ReceivingTimeoutException,
            ConsoleException, IOException;

    public abstract void send(byte[] message) throws IOException;

    public void setIpAddress(String ipAddress) {
        if (ipAddress == null) {
            throw new IllegalArgumentException("ipAdress is null");
        }
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return this.port;
    }

    public String receiveTo(String toString) throws SocketException,
            ReceivingTimeoutException, ConsoleException, IOException {
        return receiveTo(toString, NULL_MORE_EXECUTOR);
    }

    public void sendln(String message) throws IOException {
        send(message + "\r\n");
    }

    public abstract byte read(int timeout) throws IOException, ConsoleException;

    public void send(String message) throws IOException {
        send(message.getBytes());
    }

    public abstract void connect() throws IOException, ConsoleException;

    public abstract void close() throws IOException;

    public abstract Protocol getProtocol();

}