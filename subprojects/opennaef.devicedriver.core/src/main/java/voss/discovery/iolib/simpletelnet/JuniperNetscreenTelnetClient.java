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
import java.util.ArrayList;
import java.util.List;

public class JuniperNetscreenTelnetClient extends RealTelnetClient {
    private static final Logger log = LoggerFactory.getLogger(JuniperNetscreenTelnetClient.class);
    private static final MoreExecutor MY_MORE_EXECUTOR = new MyMoreExecutor();

    private String promptPrefix;
    private String currentMode = MODE_NOT_LOGIN;
    private boolean terminalInitialized;
    private List<String> initializeCommands;

    public JuniperNetscreenTelnetClient(TerminalSocket socket, NodeInfo nodeinfo) {
        super(nodeinfo);
        setTelnetSocket(socket);
        initializeCommands = getinitializeCommands();
    }

    private List<String> getinitializeCommands() {
        List<String> commands = new ArrayList<String>();
        return commands;
    }

    private void sendTerminalInitCommands() throws IOException, ConsoleException {
        for (String command : initializeCommands) {
            sendln(command);
            receiveToPrompt();
        }
        terminalInitialized = true;
    }

    public String changeMode(String changingMode) throws IOException,
            ConsoleException {
        return "";
    }

    public String execute(String command) throws IOException, ConsoleException {
        if (!terminalInitialized) {
            sendTerminalInitCommands();
        }
        sendln(command);
        return translate(receiveToPrompt());
    }

    public void executeWriteMemoryWithY(String target) throws SocketException,
            ReceivingTimeoutException, IOException, ConsoleException {
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

    private static final String PROMPT_SUFFIX_ENABLE = ">";

    public void loginBySsh() throws IOException, ConsoleException {
        connect();
        setPromptPrefix(receiveTo(PROMPT_SUFFIX_ENABLE));
        currentMode = ConsoleClient.MODE_ENABLE;
    }

    public void loginByTelnet() throws IOException, ConsoleException {
        connect();
        ReceiveResult result = receiveTo(new String[]{"login: ", "password:"});
        if ("Password:".equals(result.getReceiveKey())) {
            sendln(getNodeInfo().getUserPassword());
        } else if ("login: ".equals(result.getReceiveKey())) {
            sendln(getNodeInfo().getAdminAccount());
            receiveTo("password:");
            sendln(getNodeInfo().getAdminPassword());
        } else {
            throw new ConsoleException("Unexpected Response");
        }
        setPromptPrefix(receiveTo(PROMPT_SUFFIX_ENABLE));
        currentMode = ConsoleClient.MODE_ENABLE;
    }

    private void setPromptPrefix(String value) throws IOException {
        int startIndex = value.lastIndexOf('\n');
        if (startIndex == -1) {
            startIndex = 0;
        } else {
            startIndex = startIndex + 1;
        }
        int lastIndex = value.lastIndexOf('>');
        promptPrefix = value.substring(startIndex, lastIndex).replaceAll("\\*", "");
    }

    String getPromptPrefix() {
        return promptPrefix;
    }

    public void logout() throws ConsoleException, IOException {
        if (currentMode.equals(MODE_NOT_LOGIN)) {
            return;
        }
        try {
            sendln("exit");
            receiveTo("(y/n) ?");
            sendln("y");
        } catch (Exception e) {
            log.debug("disconnected. cause=" + e.getMessage());
        } finally {
            close();
        }
        currentMode = MODE_NOT_LOGIN;
    }

    protected final MoreExecutor getMoreExecutor() {
        return MY_MORE_EXECUTOR;
    }

    protected final String translateNewLine(String value) throws IOException {
        return juniperTranslate(value);
    }

    protected static final String juniperTranslate(String value)
            throws IOException {
        StreamTranslator translator = new StreamTranslatorCrDeleteType();
        String result = translator.translate(value);
        return simpleTranslate(result);
    }

    private String receiveToPrompt() throws SocketException,
            ReceivingTimeoutException, IOException, ConsoleException {
        return receiveTo(getPrompt());
    }

    @Override
    public String getPrompt() {
        return getPromptPrefix() + getPromptSuffix();
    }

    private String getPromptSuffix() {
        if (MODE_ENABLE.equals(currentMode)) {
            return "> ";
        }
        throw new UnsupportedOperationException("Current:" + currentMode);
    }

    private static class MyMoreExecutor implements MoreExecutor {
        private final byte[] morePrompt = ".*--- more ---".getBytes();

        public String getString() {
            return " ";
        }

        public byte[] getSendMoreBytes(String value) {
            return " ".getBytes();
        }

        public boolean isMoreInput(byte[] value) {
            return ByteArrayUtil.matches(value, morePrompt);
        }

    }

}