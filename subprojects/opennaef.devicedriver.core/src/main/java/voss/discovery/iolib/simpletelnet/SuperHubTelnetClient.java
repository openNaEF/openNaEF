package voss.discovery.iolib.simpletelnet;

import voss.discovery.iolib.console.ConsoleClient;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.utils.ByteArrayUtil;
import voss.model.NodeInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.SocketException;

public class SuperHubTelnetClient extends RealTelnetClient {

    private static final MoreExecutor MY_MORE_EXECUTOR = new MyMoreExecutor();

    private String promptPrefix;
    private String currentMode = MODE_NOT_LOGIN;

    public SuperHubTelnetClient(TerminalSocket socket, NodeInfo nodeinfo) {
        super(nodeinfo);
        setTelnetSocket(socket);
    }

    public String execute(String command) throws IOException, ConsoleException {
        sendln(command);
        return translate(recieveToPrompt());
    }

    public void executeWriteMemoryWithY(String target) throws SocketException,
            ReceivingTimeoutException, IOException, ConsoleException {
        String writeCommand = "write";
        if (target != null) {
            writeCommand = writeCommand + " " + target;
        }
        sendln(writeCommand);
        receiveTo("[y/n] ? ");
        sendln("y");
        recieveToPrompt();
    }

    private String translate(String value) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(value));
        StringBuffer temp = new StringBuffer();
        String lineTemp = null;
        reader.readLine();
        while ((lineTemp = reader.readLine()) != null) {
            temp.append(lineTemp);
            temp.append('\n');
        }
        String result = temp.toString();
        return result.substring(0, result
                .lastIndexOf('\n', result.length() - 2) + 1);
    }

    public void loginBySsh() throws IOException, ConsoleException {
        connect();
        setPromptPrefix(receiveTo("> "));
        currentMode = ConsoleClient.MODE_LOGIN;
    }

    public void loginByTelnet() throws IOException, ConsoleException {
        connect();
        ReceiveResult result = receiveTo(new String[]{"login :"});
        if ("login :".equals(result.getReceiveKey())) {
            sendln(getNodeInfo().getUserAccount());
            receiveTo("Password:");
            sendln(getNodeInfo().getUserPassword());
        } else {
            throw new ConsoleException("Unexpected Response");
        }
        setPromptPrefix(receiveTo("> "));
        currentMode = ConsoleClient.MODE_LOGIN;
    }

    private void setPromptPrefix(String value) throws IOException {
        int startIndex = value.lastIndexOf('\n') + 1;
        int lastIndex = value.lastIndexOf('>');
        promptPrefix = value.substring(startIndex, lastIndex);
    }

    String getPromptPrefix() {
        return promptPrefix;
    }

    public String changeMode(String changingMode) throws IOException,
            ConsoleException {
        if (MODE_LOGIN.equals(currentMode) && MODE_ENABLE.equals(changingMode)) {
            return changeLoginToEnableMode();
        } else if (MODE_ENABLE.equals(currentMode) && MODE_LOGIN.equals(changingMode)) {
            return changeEnableToLoginMode();

        } else {
            throw new UnsupportedOperationException("Current:" + currentMode
                    + " To:" + changingMode);
        }
    }

    private String changeLoginToEnableMode() throws IOException,
            ConsoleException {
        sendln("ena");
        receiveTo("Password:");
        sendln(getNodeInfo().getAdminPassword());
        currentMode = MODE_ENABLE;
        return translate(recieveToPrompt());
    }

    private String changeEnableToLoginMode() throws IOException,
            ConsoleException {
        sendln("exit");
        currentMode = MODE_LOGIN;
        return translate(recieveToPrompt());
    }

    public void logout() throws ConsoleException, IOException {
        if (currentMode.equals(MODE_NOT_LOGIN)) {
            return;
        }
        if (currentMode.equals(MODE_ENABLE)) {
            sendln("exit");
            currentMode = MODE_LOGIN;
        }
        sendln("exit");
        close();
        currentMode = MODE_NOT_LOGIN;
    }

    protected final MoreExecutor getMoreExecutor() {
        return MY_MORE_EXECUTOR;
    }

    protected final String translateNewLine(String value) throws IOException {
        return value;
    }

    private String recieveToPrompt() throws SocketException,
            ReceivingTimeoutException, IOException, ConsoleException {
        return receiveTo(getPrompt());
    }

    @Override
    public String getPrompt() {
        return getPromptPrefix() + getPromptSuffix();
    }

    private String getPromptSuffix() {
        if (MODE_LOGIN.equals(currentMode)) {
            return "> ";
        }
        if (MODE_ENABLE.equals(currentMode)) {
            return "# ";
        }
        throw new UnsupportedOperationException("Current:" + currentMode);
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