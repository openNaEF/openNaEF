package voss.discovery.iolib.simpletelnet;

import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.utils.ByteArrayUtil;
import voss.model.NodeInfo;

import java.io.IOException;

public class TestSshClient extends RealTelnetClient {
    private static final MoreExecutor MY_MORE_EXECUTOR = new MyMoreExecutor();

    public TestSshClient(TerminalSocket socket, NodeInfo nodeinfo) {
        super(nodeinfo);
        setTelnetSocket(socket);
    }

    @Override
    public String changeMode(String mode) throws IOException, ConsoleException {
        return null;
    }

    @Override
    public String execute(String command) throws IOException, ConsoleException {
        super.sendln(command);
        return "sent.";
    }

    @Override
    public void logout() throws IOException, ConsoleException {
        super.close();
    }

    @Override
    protected MoreExecutor getMoreExecutor() {
        return MY_MORE_EXECUTOR;
    }

    @Override
    protected String translateNewLine(String value) throws IOException {
        return value;
    }

    @Override
    public void loginBySsh() throws IOException, ConsoleException {
        connect();
    }

    @Override
    public void loginByTelnet() throws IOException, ConsoleException {
        throw new IllegalStateException("not supported.");
    }

    @Override
    public String getPrompt() {
        return "#";
    }

    private static class MyMoreExecutor implements MoreExecutor {
        public String getString() {
            return " ";
        }

        public byte[] getSendMoreBytes(String value) {
            return " ".getBytes();
        }

        public boolean isMoreInput(byte[] value) {
            return ByteArrayUtil.contains(value, "-- More --".getBytes());
        }
    }
}