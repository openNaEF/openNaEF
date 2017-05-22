package voss.discovery.iolib.simpletelnet;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.InteractiveCallback;
import com.trilead.ssh2.Session;
import com.trilead.ssh2.StreamGobbler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.console.ConsoleException;
import voss.model.NodeInfo;
import voss.model.Protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class RealSsh2Socket extends RealTerminalSocket {
    private final static Logger log = LoggerFactory.getLogger(RealSsh2Socket.class);
    private final NodeInfo nodeinfo;
    private final Protocol method;
    private PushbackInputStream input;
    private OutputStream output;
    private Connection connection;
    private Session session;
    private boolean connected = false;

    public RealSsh2Socket(NodeInfo nodeinfo, InetAddress inetAddress, int port,
                          int timeout) throws IOException {
        if (nodeinfo == null) {
            throw new IllegalArgumentException("no node-info.");
        }
        super.setIpAddress(inetAddress.getHostAddress());
        super.setPort(port);
        this.TIMEOUT_TIME = timeout;
        this.nodeinfo = nodeinfo;
        this.method = nodeinfo.getPreferredConsoleProtocol();
    }

    public RealSsh2Socket(NodeInfo nodeinfo, String ipAddress, int port,
                          int timeout) throws IOException {
        if (nodeinfo == null) {
            throw new IllegalArgumentException("no node-info.");
        }
        super.setIpAddress(ipAddress);
        super.setPort(port);
        this.TIMEOUT_TIME = timeout;
        this.nodeinfo = nodeinfo;
        this.method = nodeinfo.getPreferredConsoleProtocol();
    }

    @Override
    public void send(byte[] value) throws IOException {
        OutputStream output = this.session.getStdin();
        output.write(value);
        output.flush();
    }

    public byte read(int timeout) throws IOException, ConsoleException {
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

    public PushbackInputStream getInputStream() throws IOException {
        if (input == null) {
            throw new IOException(
                    "login session isn't opened or is already closed.");
        }
        return input;
    }

    public void connect() throws IOException {
        connect(nodeinfo.getUserAccount(), nodeinfo.getUserPassword());
        this.connected = true;
    }

    public void connect(final String username, final String password) throws IOException {
        if (getIpAddress() == null) {
            throw new IllegalStateException("ipAddress is null");
        }
        this.connection = new Connection(getIpAddress(), getPort());
        this.connection.connect();
        log.info("connected: " + getIpAddress() + ":" + getPort());

        boolean isAuthenticated = false;
        switch (this.method) {
            case SSH2:
                isAuthenticated = connection.authenticateWithPassword(username, password);
                break;
            case SSH2_INTERACTIVE:
                InteractiveCallback cb = new SimpleInteractiveCallback(password);
                isAuthenticated = connection.authenticateWithKeyboardInteractive(username, cb);
                break;
            case SSH2_PUBLICKEY:
            default:
                throw new IllegalStateException("unsupported method: " + this.method.name());
        }
        if (isAuthenticated == false) {
            throw new IOException("Authentication failed.");
        }
        this.session = this.connection.openSession();

        if (isVt100()) {
            this.session.requestPTY("vt100");
        } else {
            this.session.requestDumbPTY();
        }

        this.session.startShell();

        this.input = new PushbackInputStream(new StreamGobbler(this.session.getStdout()));
        this.output = this.session.getStdin();

        new StreamGobbler(this.session.getStderr());

        log.info("login: " + getIpAddress() + ":" + getPort());
    }

    public void close() throws IOException {
        if (!this.connected) {
            return;
        }
        this.input.close();
        this.output.close();
        this.session.close();
        this.connection.close();
        this.input = null;
        this.output = null;
        this.connected = false;
        log.info("disconnected: " + getIpAddress() + ":" + getPort());
    }

    public Protocol getProtocol() {
        return Protocol.SSH2;
    }

    private class SimpleInteractiveCallback implements InteractiveCallback {
        private final String password;

        public SimpleInteractiveCallback(String password) {
            this.password = password;
        }

        @Override
        public String[] replyToChallenge(String name, String instruction,
                                         int numberOfPrompt, String[] prompts, boolean[] echos) throws Exception {
            StringBuilder sb = new StringBuilder();
            sb.append("name=").append(name)
                    .append(";instruction=").append(instruction)
                    .append(";numPrompt=").append(numberOfPrompt)
                    .append(";prompt={");
            for (int i = 0; i < numberOfPrompt; i++) {
                sb.append("[").append(i).append("]=").append(prompts[i]).append(";");
            }
            sb.append("};echo={");
            for (int i = 0; i < echos.length; i++) {
                sb.append("[").append(i).append("]=").append(String.valueOf(echos[i])).append(";");
            }
            sb.append("}");
            log.debug("server challenge: " + sb.toString());
            String[] reply = new String[numberOfPrompt];
            if (numberOfPrompt == 1) {
                reply[0] = password;
            }
            log.debug("response: " + Arrays.toString(reply));
            return reply;
        }
    }
}