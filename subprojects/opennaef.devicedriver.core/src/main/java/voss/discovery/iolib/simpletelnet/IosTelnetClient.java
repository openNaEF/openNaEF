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

public class IosTelnetClient extends RealTelnetClient {
    private static final Logger log = LoggerFactory.getLogger(IosTelnetClient.class);

    private static final String LOGIN_PROMPT = "[A-Za-z0-9-_.]+>";
    private static final String ENABLE_PROMPT = "[A-Za-z0-9-_.]+#";
    private static final char PROMPT_SUFFIX_LOGIN = '>';
    private static final char PROMPT_SUFFIX_ENABLE = '#';
    private static final MoreExecutor MY_MORE_EXECUTOR = new MyMoreExecutor();

    public static final String MODE_VLAN_DB = "vlan database";
    public static final String MODE_CONFIG_VLAN = "config-vlan";
    public static final String MODE_GLOBAL_CONFIG = "global config";
    public static final String MODE_INTERFACE_CONFIG = "interface config";

    private String promptPrefix;
    protected String currentMode = MODE_NOT_LOGIN;

    public IosTelnetClient(TerminalSocket socket, NodeInfo telnet) {
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
        return result.substring(0, result.lastIndexOf('\n', result.length() - 2) + 1);
    }

    public void loginBySsh() throws IOException, ConsoleException {
        connect();
        ReceiveResult result = receiveTo(new String[]{LOGIN_PROMPT, ENABLE_PROMPT,});
        if (result.getReceiveKey().equals(LOGIN_PROMPT)) {
            setPromptPrefix(result.getResult(), PROMPT_SUFFIX_LOGIN);
            currentMode = ConsoleClient.MODE_LOGIN;
            return;
        } else if (result.getReceiveKey().equals(ENABLE_PROMPT)) {
            setPromptPrefix(result.getResult(), PROMPT_SUFFIX_ENABLE);
            currentMode = ConsoleClient.MODE_ENABLE;
            return;
        } else {
            throw new ConsoleException("Unexpected Response");
        }
    }

    public void loginByTelnet() throws IOException, ConsoleException {
        connect();
        ReceiveResult result = receiveTo(new String[]{"Password: ",
                "Username: ", LOGIN_PROMPT, ENABLE_PROMPT});
        if ("Password: ".equals(result.getReceiveKey())) {
            sendln(getNodeInfo().getUserPassword());
        } else if ("Username: ".equals(result.getReceiveKey())) {
            sendln(getNodeInfo().getUserAccount());
            receiveTo("Password: ");
            sendln(getNodeInfo().getUserPassword());
        } else if (result.getReceiveKey().equals(LOGIN_PROMPT)) {
            setPromptPrefix(result.getResult(), PROMPT_SUFFIX_LOGIN);
            currentMode = ConsoleClient.MODE_LOGIN;
            return;
        } else if (result.getReceiveKey().equals(ENABLE_PROMPT)) {
            setPromptPrefix(result.getResult(), PROMPT_SUFFIX_ENABLE);
            currentMode = ConsoleClient.MODE_ENABLE;
            return;
        } else {
            throw new ConsoleException("Unexpected Response");
        }

        result = receiveTo(new String[]{LOGIN_PROMPT, ENABLE_PROMPT});
        if (result.getReceiveKey().equals(LOGIN_PROMPT)) {
            setPromptPrefix(result.getResult(), PROMPT_SUFFIX_LOGIN);
            currentMode = ConsoleClient.MODE_LOGIN;
            return;
        } else if (result.getReceiveKey().equals(ENABLE_PROMPT)) {
            setPromptPrefix(result.getResult(), PROMPT_SUFFIX_ENABLE);
            currentMode = ConsoleClient.MODE_ENABLE;
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
        promptPrefix = value.substring(startIndex, lastIndex);
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
        } else if (MODE_ENABLE.equals(currentMode)
                && MODE_VLAN_DB.equals(changingMode)) {
            sendln("vlan database");
            currentMode = MODE_VLAN_DB;
            return translate(receiveToPrompt());
        } else if (MODE_VLAN_DB.equals(currentMode)
                && MODE_ENABLE.equals(changingMode)) {
            sendln("exit");
            currentMode = MODE_ENABLE;
            return translate(receiveToPrompt());
        } else if (MODE_ENABLE.equals(currentMode)
                && MODE_GLOBAL_CONFIG.equals(changingMode)) {
            sendln("configure terminal");
            currentMode = MODE_GLOBAL_CONFIG;
            return translate(receiveToPrompt());
        } else if (MODE_GLOBAL_CONFIG.equals(currentMode)
                && changingMode.startsWith(MODE_INTERFACE_CONFIG)) {
            sendln("interface "
                    + changingMode.substring(MODE_INTERFACE_CONFIG.length()));
            currentMode = changingMode;
            return translate(receiveToPrompt());
        } else if (currentMode.startsWith(MODE_INTERFACE_CONFIG)
                && changingMode.startsWith(MODE_INTERFACE_CONFIG)) {
            if (currentMode.equals(changingMode)) {
                return "";
            }
            sendln("interface "
                    + changingMode.substring(MODE_INTERFACE_CONFIG.length()));
            currentMode = changingMode;
            return translate(receiveToPrompt());
        } else if (currentMode.startsWith(MODE_INTERFACE_CONFIG)
                && MODE_GLOBAL_CONFIG.equals(changingMode)) {
            sendln("exit");
            currentMode = changingMode;
            return translate(receiveToPrompt());
        } else if (MODE_GLOBAL_CONFIG.equals(currentMode)
                && MODE_ENABLE.equals(changingMode)) {
            sendln("exit");
            currentMode = MODE_ENABLE;
            return translate(receiveToPrompt());
        } else {
            throw new UnsupportedOperationException("Current:" + currentMode
                    + " To:" + changingMode);
        }
    }

    private String changeLoginToEnableMode() throws IOException,
            ConsoleException {
        sendln("enable");
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
        if (currentMode.startsWith(MODE_INTERFACE_CONFIG)) {
            changeMode(MODE_GLOBAL_CONFIG);
        }
        if (MODE_GLOBAL_CONFIG.equals(currentMode)
                || MODE_VLAN_DB.equals(currentMode)) {
            changeMode(MODE_ENABLE);
        }
        sendln("exit");
        close();
        currentMode = MODE_NOT_LOGIN;
    }

    protected MoreExecutor getMoreExecutor() {
        return MY_MORE_EXECUTOR;
    }

    protected final String translateNewLine(String value) throws IOException {
        return simpleTranslate(value);
    }

    protected String receiveToPrompt() throws SocketException,
            ReceivingTimeoutException, IOException, ConsoleException {
        String expected = getPrompt();
        log.trace("receiveToPrompt():" + expected);
        return receiveTo(expected);
    }

    public String getPrompt() {
        String expected = getPromptPrefix() + getPromptSuffix();
        return expected;
    }

    protected String getPromptSuffix() {
        if (MODE_LOGIN.equals(currentMode)) {
            return ">";
        }
        if (MODE_ENABLE.equals(currentMode)) {
            return "#";
        }
        if (MODE_VLAN_DB.equals(currentMode)) {
            return "(vlan)#";
        }
        if (MODE_CONFIG_VLAN.equals(currentMode)) {
            return "(config-vlan)#";
        }
        if (MODE_GLOBAL_CONFIG.equals(currentMode)) {
            return "(config)#";
        }
        if (currentMode.startsWith(MODE_INTERFACE_CONFIG)) {
            return "(config-if)#";
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
            return ByteArrayUtil.contains(value, " --More-- ".getBytes());
        }

    }

}