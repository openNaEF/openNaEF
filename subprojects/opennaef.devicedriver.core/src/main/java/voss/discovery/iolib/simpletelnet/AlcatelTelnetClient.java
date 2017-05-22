package voss.discovery.iolib.simpletelnet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.console.ConsoleClient;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.utils.ByteArrayUtil;
import voss.model.NodeInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.SocketException;

public class AlcatelTelnetClient extends RealTelnetClient {
    private static final Logger log = LoggerFactory.getLogger(AlcatelTelnetClient.class);

    private static final String PROMPT = "#";
    private static final char PROMPT_SUFFIX = '#';
    private static final String PROMPT_PREFIX_ENABLE = "A:";
    private static final MoreExecutor MY_MORE_EXECUTOR = new MyMoreExecutor();

    private String promptPrefix;
    protected String currentMode = MODE_NOT_LOGIN;

    public AlcatelTelnetClient(TerminalSocket socket, NodeInfo telnet) {
        super(telnet);
        setTelnetSocket(socket);
    }

    public String execute(String command) throws IOException, ConsoleException {
        sendln(command);
        return translate(receiveToPrompt());
    }

    protected String translate(String value) throws IOException {
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

    private String getCurrentMode(String prompt) {
        if (prompt.startsWith(PROMPT_PREFIX_ENABLE)) {
            return ConsoleClient.MODE_ENABLE;
        } else {
            return ConsoleClient.MODE_LOGIN;
        }
    }

    public void loginBySsh() throws IOException, ConsoleException {
        connect();
        ReceiveResult result = receiveTo(new String[]{PROMPT,});

        if (result.getReceiveKey().equals(PROMPT)) {
            currentMode = getCurrentMode(result.getResult());
            setPromptPrefix(result.getResult(), PROMPT_SUFFIX);
            sendln("environment no more");
            receiveToPrompt();
            return;
        } else {
            throw new ConsoleException("Unexpected Response");
        }
    }

    public void loginByTelnet() throws IOException, ConsoleException {

        connect();
        ReceiveResult result = receiveTo(new String[]{"Password: ", "Login: ", PROMPT,});
        if ("Password: ".equals(result.getReceiveKey())) {
            sendln(getNodeInfo().getUserPassword());
        } else if ("Login: ".equals(result.getReceiveKey())) {
            sendln(getNodeInfo().getUserAccount());
            receiveTo("Password: ");
            sendln(getNodeInfo().getUserPassword());
        } else if (result.getReceiveKey().equals(PROMPT)) {
            setPromptPrefix(result.getResult(), PROMPT_SUFFIX);
            currentMode = getCurrentMode(result.getResult());
            sendln("environment no more");
            receiveToPrompt();
            return;
        } else {
            throw new ConsoleException("Unexpected Response" + result.getReceiveKey());
        }

        result = receiveTo(new String[]{PROMPT});
        if (result.getReceiveKey().equals(PROMPT)) {
            setPromptPrefix(result.getResult(), PROMPT_SUFFIX);
            currentMode = getCurrentMode(result.getResult());
            sendln("environment no more");
            receiveToPrompt();
            return;
        } else {
            throw new ConsoleException("Unexpected Response");
        }
    }

    private void setPromptPrefix(String value, char endChar) {
        int startIndex = value.lastIndexOf('\n');
        if (startIndex == -1) {
            startIndex = 0;
        } else {
            startIndex = startIndex + 1;
        }
        int lastIndex = value.lastIndexOf(endChar);
        value = value.substring(startIndex, lastIndex).replaceAll("\\*", "").trim();
        promptPrefix = ".*" + value;
    }

    String getPromptPrefix() {
        return promptPrefix;
    }

    public String changeMode(String changingMode) throws IOException, ConsoleException {
        if (currentMode.equals(changingMode)) {
            return "";
        }

        if (MODE_LOGIN.equals(currentMode) && MODE_ENABLE.equals(changingMode)) {
            return changeLoginToEnableMode();
        } else {
            throw new UnsupportedOperationException("Current:" + currentMode
                    + " To:" + changingMode);
        }
    }

    private String changeLoginToEnableMode() throws IOException, ConsoleException {
        sendln("enable-admin");
        receiveTo("Password: ");
        log.trace("changeLoginToEnableMode(): sent: "
                + getNodeInfo().getAdminPassword());
        sendln(getNodeInfo().getAdminPassword());
        currentMode = MODE_ENABLE;
        return translate(receiveToPrompt());
    }

    public void logout() throws IOException, ConsoleException {
        if (currentMode.equals(MODE_NOT_LOGIN)) {
            return;
        }
        sendln("logout");
        close();
        currentMode = MODE_NOT_LOGIN;
    }

    protected MoreExecutor getMoreExecutor() {
        return MY_MORE_EXECUTOR;
    }

    protected final String translateNewLine(String value) throws IOException {
        return simpleTranslate(value);
    }

    protected String receiveToPrompt() throws SocketException, ReceivingTimeoutException, IOException, ConsoleException {
        String expected = getPrompt();
        log.trace("receiveToPrompt():" + expected);
        return receiveTo(expected);
    }

    @Override
    public String getPrompt() {
        return getPromptPrefix() + getPromptSuffix();
    }

    protected String getPromptSuffix() {
        if (MODE_LOGIN.equals(currentMode)) {
            return "#";
        }
        if (MODE_ENABLE.equals(currentMode)) {
            return "#";
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
            return ByteArrayUtil.contains(value, "Press any key to continue".getBytes());
        }

    }

}