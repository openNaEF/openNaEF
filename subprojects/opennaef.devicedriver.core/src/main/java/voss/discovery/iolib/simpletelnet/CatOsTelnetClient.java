package voss.discovery.iolib.simpletelnet;

import voss.discovery.iolib.console.ConsoleClient;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.utils.ByteArrayUtil;
import voss.model.NodeInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.SocketException;


public class CatOsTelnetClient extends RealTelnetClient {

    private static final MoreExecutor MY_MORE_EXECUTOR = new MyMoreExecutor();

    private String promptPrefix;
    private String currentMode = MODE_NOT_LOGIN;

    public CatOsTelnetClient(TerminalSocket socket, NodeInfo nodeinfo) {
        super(nodeinfo);
        setTelnetSocket(socket);
    }

    public String execute(String command) throws IOException, ConsoleException {
        sendln(command);
        return translate(recieveToPrompt());
    }

    public void executeWriteMemoryWithY() throws SocketException, ReceivingTimeoutException, IOException, ConsoleException {
        sendln("write memory");
        receiveTo("[n]?");
        sendln("y");
        recieveToPrompt();
    }

    private String translate(String value) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(value));
        StringBuffer temp = new StringBuffer();
        String lineTemp = null;
        reader.readLine();
        reader.readLine();
        while ((lineTemp = reader.readLine()) != null) {
            temp.append(lineTemp);
            temp.append('\n');
        }
        String result = temp.toString();
        return result.substring(0, result.lastIndexOf('\n', result.length() - 2) + 1);
    }

    public static final String PROMPT_SUFFIX_LOGIN = ">";
    public static final String PROMPT_SUFFIX_ENABLE = "#";

    public void loginBySsh() throws IOException, ConsoleException {
        connect();
        ReceiveResult result = receiveTo(new String[]{PROMPT_SUFFIX_LOGIN, PROMPT_SUFFIX_ENABLE,});
        if (PROMPT_SUFFIX_LOGIN.equals(result.getReceiveKey())) {
            setPromptPrefix(receiveTo(PROMPT_SUFFIX_LOGIN));
            currentMode = ConsoleClient.MODE_LOGIN;
        } else if (PROMPT_SUFFIX_ENABLE.equals(result.getReceiveKey())) {
            setPromptPrefix(receiveTo(PROMPT_SUFFIX_ENABLE));
            currentMode = ConsoleClient.MODE_ENABLE;
        } else {
            throw new ConsoleException("Unexpected Response");
        }
    }

    public void loginByTelnet() throws IOException, ConsoleException {
        connect();
        ReceiveResult result = receiveTo(new String[]{"password: ", "Username: ", PROMPT_SUFFIX_LOGIN, PROMPT_SUFFIX_ENABLE,});
        if ("password: ".equals(result.getReceiveKey())) {
            sendln(getNodeInfo().getUserPassword());
            setPromptPrefix(receiveTo(PROMPT_SUFFIX_LOGIN));
            currentMode = ConsoleClient.MODE_LOGIN;
        } else if ("Username: ".equals(result.getReceiveKey())) {
            sendln(getNodeInfo().getUserAccount());
            receiveTo("Password: ");
            sendln(getNodeInfo().getUserPassword());
            setPromptPrefix(receiveTo(PROMPT_SUFFIX_LOGIN));
            currentMode = ConsoleClient.MODE_LOGIN;
        } else if (PROMPT_SUFFIX_LOGIN.equals(result.getReceiveKey())) {
            setPromptPrefix(receiveTo(PROMPT_SUFFIX_LOGIN));
            currentMode = ConsoleClient.MODE_LOGIN;
        } else if (PROMPT_SUFFIX_ENABLE.equals(result.getReceiveKey())) {
            setPromptPrefix(receiveTo(PROMPT_SUFFIX_ENABLE));
            currentMode = ConsoleClient.MODE_ENABLE;
        } else {
            throw new ConsoleException("Unexpected Response");
        }
    }

    private void setPromptPrefix(String value) throws IOException {
        int startIndex = value.lastIndexOf('\n') + 1;
        int lastIndex = value.lastIndexOf('>');
        promptPrefix = value.substring(startIndex, lastIndex);
    }

    String getPromptPrefix() {
        return promptPrefix;
    }

    public String changeMode(String changingMode) throws IOException, ConsoleException {
        if (MODE_LOGIN.equals(currentMode) && MODE_ENABLE.equals(changingMode)) {
            return changeLoginToEnableMode();
        } else if (MODE_LOGIN.equals(currentMode) && MODE_LOGIN.equals(changingMode)) {
            return "";
        } else if (MODE_ENABLE.equals(currentMode) && MODE_ENABLE.equals(changingMode)) {
            return "";
        } else {
            throw new UnsupportedOperationException("Current:" + currentMode
                    + " To:" + changingMode);
        }
    }

    private String changeLoginToEnableMode() throws IOException, ConsoleException {
        sendln("enable");
        receiveTo(" password: ");
        sendln(getNodeInfo().getAdminPassword());
        currentMode = MODE_ENABLE;
        return translate(recieveToPrompt());
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
        if (MODE_LOGIN.equals(currentMode)) {
            return "> ";
        }
        if (MODE_ENABLE.equals(currentMode)) {
            return "> (enable) ";
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