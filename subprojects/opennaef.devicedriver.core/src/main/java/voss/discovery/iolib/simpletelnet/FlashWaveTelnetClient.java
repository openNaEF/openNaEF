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

public class FlashWaveTelnetClient extends RealTelnetClient {
    public static final String MODE_CONFIG = "config";
    private static final MoreExecutor MY_MORE_EXECUTOR = new MyMoreExecutor();

    private String promptPrefix;
    private String currentMode = MODE_NOT_LOGIN;

    public FlashWaveTelnetClient(TerminalSocket socket, NodeInfo nodeinfo) {
        super(nodeinfo);
        setTelnetSocket(socket);
    }

    public String changeMode(String changingMode) throws IOException, ConsoleException {
        Logger log = LoggerFactory.getLogger(getClass());
        log.debug("mode-change: " + this.currentMode + " to " + changingMode);
        if (MODE_ENABLE.equals(currentMode) && changingMode.startsWith(MODE_CONFIG)) {
            sendln("config");
            currentMode = changingMode;
            return translate(receiveToPrompt());
        } else if (currentMode.startsWith(MODE_CONFIG) && changingMode.startsWith(MODE_ENABLE)) {
            sendln("exit");
            currentMode = changingMode;
            return translate(receiveToPrompt());
        } else {
            return "";
        }
    }

    public String execute(String command) throws IOException, ConsoleException {
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
        return result.substring(0, result
                .lastIndexOf('\n', result.length() - 2) + 1);
    }

    public final static String PROMPT_LOGIN = "Login:";
    public final static String PROMPT_PASSWORD = "Password:";

    public void loginBySsh() throws IOException, ConsoleException {
        connect();
        setPromptPrefix(receiveTo("#"));
        currentMode = ConsoleClient.MODE_ENABLE;
    }

    private static final String PROMPT_SUFFIX_ENABLE = "#";

    public void loginByTelnet() throws IOException, ConsoleException {
        connect();
        ReceiveResult result = receiveTo(new String[]{PROMPT_LOGIN,
                PROMPT_PASSWORD});
        if (PROMPT_PASSWORD.equals(result.getReceiveKey())) {
            sendln(getNodeInfo().getUserPassword());
        } else if (PROMPT_LOGIN.equals(result.getReceiveKey())) {
            sendln(getNodeInfo().getAdminAccount());
            receiveTo(PROMPT_PASSWORD);
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
        sendln("logout");
        close();
        currentMode = MODE_NOT_LOGIN;
    }

    protected final MoreExecutor getMoreExecutor() {
        return MY_MORE_EXECUTOR;
    }

    protected final String translateNewLine(String value) throws IOException {
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
            return "#";
        } else if (MODE_CONFIG.equals(currentMode)) {
            return "(config)#";
        }
        throw new UnsupportedOperationException("Current:" + currentMode);
    }

    private static class MyMoreExecutor implements MoreExecutor {
        private final static String MORE_PROMPT;

        static {
            byte[] _MORE = "(Q to quit)".getBytes();
            byte[] __MORE = new byte[_MORE.length + 1];
            System.arraycopy(_MORE, 0, __MORE, 0, _MORE.length);
            __MORE[__MORE.length - 1] = (byte) 0x00;
            MORE_PROMPT = new String(__MORE);
        }

        public String getString() {
            return "\r\n";
        }

        public byte[] getSendMoreBytes(String value) {
            return "\r\n".getBytes();
        }

        public boolean isMoreInput(byte[] value) {
            return ByteArrayUtil.contains(value, MORE_PROMPT.getBytes());
        }

    }

}