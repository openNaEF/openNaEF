package voss.discovery.iolib.simpletelnet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.console.ConsoleClient;
import voss.discovery.iolib.console.ConsoleException;
import voss.model.NodeInfo;
import voss.model.Protocol;

import java.io.IOException;
import java.net.SocketException;


public abstract class RealTelnetClient implements ConsoleClient {
    private static final Logger log = LoggerFactory.getLogger(RealTelnetClient.class);
    private boolean isConnected = false;
    private TerminalSocket socket;
    private NodeInfo nodeinfo;

    protected RealTelnetClient() {
    }

    protected RealTelnetClient(NodeInfo nodeinfo) {
        this.nodeinfo = nodeinfo;
    }

    protected void setTelnetSocket(TerminalSocket socket) {
        this.socket = socket;
    }

    protected TerminalSocket getTerminalSocket() {
        return socket;
    }

    protected abstract MoreExecutor getMoreExecutor();

    protected abstract String translateNewLine(String value) throws IOException;

    void connect() throws IOException, ConsoleException {
        socket.connect();
        isConnected = true;
    }

    public void login() throws IOException, ConsoleException {
        if (getNodeInfo().isSupported(Protocol.SSH2)) {
            loginBySsh();
        } else if (getNodeInfo().isSupported(Protocol.SSH2_INTERACTIVE)) {
            loginBySsh();
        } else if (getNodeInfo().isSupported(Protocol.SSH2_PUBLICKEY)) {
            loginBySsh();
        } else if (getNodeInfo().isSupported(Protocol.TELNET)) {
            loginByTelnet();
        } else {
            throw new IllegalStateException("no supported protocol");
        }
    }

    abstract public void loginBySsh() throws IOException, ConsoleException;

    abstract public void loginByTelnet() throws IOException, ConsoleException;

    @Override
    abstract public String getPrompt();

    public String receiveTo(String toString) throws SocketException, ReceivingTimeoutException, IOException, ConsoleException {
        return translateNewLine(socket.receiveTo(toString, getMoreExecutor()));
    }

    public ReceiveResult receiveTo(String[] toString) throws SocketException, ReceivingTimeoutException, IOException, ConsoleException {
        ReceiveResult result = socket.receiveTo(toString, getMoreExecutor());
        return result;
    }

    public void sendln(String command) throws ConsoleException, IOException {
        if (log.isTraceEnabled()) {
            log.trace("sent: [" + command + "]");
        }
        socket.sendln(command);
    }

    String receiveToPossible(boolean withNothing) throws SocketException, IOException, ConsoleException {
        try {
            return translateNewLine(socket.receiveToPossible(getMoreExecutor()));
        } catch (ReceivingTimeoutException ste) {
            if (withNothing) {
                return "";
            } else {
                throw ste;
            }
        }
    }

    @Override
    public void breakConnection() throws IOException {
        close();
    }

    void close() throws IOException {
        socket.close();
        this.isConnected = false;
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    protected static final String simpleTranslate(String value) {
        StringBuffer result = new StringBuffer();
        char previous = '\0';
        for (int i = 0; i < value.length(); i++) {
            final char now = value.charAt(i);
            if (now == '\r' || (previous != '\r' && now == '\n')) {
                result.append('\n');
            } else if (previous == '\r' && now == '\n') {
            } else {
                result.append(now);
            }
            previous = now;
        }
        return result.toString();
    }

    protected final NodeInfo getNodeInfo() {
        return nodeinfo;
    }

}