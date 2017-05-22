package voss.discovery.iolib.simpletelnet;

import voss.discovery.iolib.console.ConsoleClient;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.utils.ByteArrayUtil;
import voss.model.NodeInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.SocketException;

public class ApresiaTelnetClient extends RealTelnetClient {

    private static final MoreExecutor MY_MORE_EXECUTOR = new MyMoreExecutor();

    private String promptPrefix;
    private String currentMode = MODE_NOT_LOGIN;

    public ApresiaTelnetClient(TerminalSocket socket, NodeInfo nodeinfo) {
        super(nodeinfo);
        setTelnetSocket(socket);
    }

    public String changeMode(String changingMode) throws IOException, ConsoleException {
        throw new RuntimeException("No need to change mode: " + changingMode);
    }

    public String execute(String command) throws IOException, ConsoleException {
        sendln(command);
        return translate(recieveToPrompt());
    }

    public void executeWriteMemoryWithY(String target) throws SocketException, ReceivingTimeoutException, IOException, ConsoleException {
        if (target == null) {
            target = "";
        } else if (!"primary".equals(target) || !"secondary".equals(target)) {
            throw new IllegalStateException("invalid target: " + target);
        }
        sendln("write configuration " + target);
        receiveTo("(y/n):");
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
        return result.substring(0, result.lastIndexOf('\n', result.length() - 2) + 1);
    }

    private static final String PROMPT_SUFFIX_ENABLE = "#";

    public void loginBySsh() throws IOException, ConsoleException {
        getTerminalSocket().connect();
        setPromptPrefix(receiveTo(PROMPT_SUFFIX_ENABLE));
        currentMode = ConsoleClient.MODE_ENABLE;
    }

    public void loginByTelnet() throws IOException, ConsoleException {
        connect();
        ReceiveResult result = receiveTo(new String[]{"login: ", "Password:"});
        if ("Password:".equals(result.getReceiveKey())) {
            sendln(getNodeInfo().getUserPassword());
        } else if ("login: ".equals(result.getReceiveKey())) {
            sendln(getNodeInfo().getAdminAccount());
            receiveTo("Password:");
            sendln(getNodeInfo().getAdminPassword());
        } else {
            throw new ConsoleException("Unexpected Response");
        }
        setPromptPrefix(receiveTo(PROMPT_SUFFIX_ENABLE));
        currentMode = ConsoleClient.MODE_ENABLE;
    }

    private void setPromptPrefix(String value) throws IOException {
        int startIndex = value.lastIndexOf('\n') + 1;
        int lastIndex = value.lastIndexOf(PROMPT_SUFFIX_ENABLE);
        promptPrefix = value.substring(startIndex, lastIndex).replaceAll("\\*", "");
    }

    String getPromptPrefix() {
        return promptPrefix;
    }

    public void logout() throws ConsoleException, IOException {
        if (currentMode.equals(MODE_NOT_LOGIN)) {
            return;
        }
        sendln("exit");
        close();
        currentMode = MODE_NOT_LOGIN;
    }

    protected final MoreExecutor getMoreExecutor() {
        return MY_MORE_EXECUTOR;
    }

    protected final String translateNewLine(String value) throws IOException {
        return simpleTranslate(value);
    }

    private String recieveToPrompt() throws SocketException, ReceivingTimeoutException, IOException, ConsoleException {
        return receiveTo(getPrompt());
    }

    @Override
    public String getPrompt() {
        return getPromptPrefix() + getPromptSuffix();
    }

    private String getPromptSuffix() {
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
            return ByteArrayUtil.contains(value, "--More--".getBytes());
        }

    }

}