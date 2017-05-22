package voss.discovery.iolib.simpletelnet;

import voss.discovery.iolib.console.ConsoleClient;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.utils.ByteArrayUtil;
import voss.model.NodeInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.SocketException;

public class AlaxalaTelnetClient extends RealTelnetClient {

    private static final String LOGIN_PROMPT = "[A-Za-z0-9-_.]+>";
    private static final String ENABLE_PROMPT = "[A-Za-z0-9-_.]+#";
    private static final MoreExecutor MY_MORE_EXECUTOR = new MyMoreExecutor();

    public static final String MODE_VLAN_DB = "vlan database";
    public static final String MODE_CONFIG_VLAN = "config-vlan";
    public static final String MODE_GLOBAL_CONFIG = "global config";
    public static final String MODE_INTERFACE_CONFIG = "interface config";

    private String promptPrefix;
    protected String currentMode = MODE_NOT_LOGIN;

    public AlaxalaTelnetClient(TerminalSocket socket, NodeInfo nodeinfo) {
        super(nodeinfo);
        setTelnetSocket(socket);
    }

    public String execute(String command) throws IOException, ConsoleException {
        sendln(command);
        return translate(recieveToPrompt());
    }

    protected String translate(String value) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(value));
        StringBuffer temp = new StringBuffer();
        String lineTemp = null;
        reader.readLine();
        while ((lineTemp = reader.readLine()) != null) {
            temp.append(lineTemp).append('\n');
        }
        String result = temp.toString();
        return result.substring(0, result
                .lastIndexOf('\n', result.length() - 2) + 1);
    }

    private final static String ASK_USER_NAME = "login: ";
    private final static String ASK_PASSWORD = "Password:";

    public void loginBySsh() throws IOException, ConsoleException {
        connect();
        ReceiveResult result = receiveTo(new String[]{LOGIN_PROMPT, ENABLE_PROMPT});
        if (result.getReceiveKey().equals(LOGIN_PROMPT)) {
            setPromptPrefix(result.getResult(), '>');
            currentMode = ConsoleClient.MODE_LOGIN;
        } else if (result.getReceiveKey().equals(ENABLE_PROMPT)) {
            setPromptPrefix(result.getResult(), '#');
            currentMode = ConsoleClient.MODE_ENABLE;
        } else {
            throw new ConsoleException("Unexpected Response");
        }
    }

    public void loginByTelnet() throws IOException, ConsoleException {
        connect();
        ReceiveResult result = receiveTo(new String[]{ASK_PASSWORD,
                ASK_USER_NAME, LOGIN_PROMPT, ENABLE_PROMPT});
        if (ASK_PASSWORD.equals(result.getReceiveKey())) {
            sendln(getNodeInfo().getUserPassword());
        } else if (ASK_USER_NAME.equals(result.getReceiveKey())) {
            sendln(getNodeInfo().getUserAccount());
            ReceiveResult next = receiveTo(new String[]{ASK_PASSWORD, LOGIN_PROMPT,});
            if (next.getReceiveKey().equals(ASK_PASSWORD)) {
                sendln(getNodeInfo().getUserPassword());
            }
        } else if (result.getReceiveKey().equals(LOGIN_PROMPT)) {
            setPromptPrefix(result.getResult(), '>');
            currentMode = ConsoleClient.MODE_LOGIN;
            return;
        } else if (result.getReceiveKey().equals(ENABLE_PROMPT)) {
            setPromptPrefix(result.getResult(), '#');
            currentMode = ConsoleClient.MODE_ENABLE;
            return;
        } else {
            throw new ConsoleException("Unexpected Response");
        }
        setPromptPrefix(receiveTo(">"));
        currentMode = ConsoleClient.MODE_LOGIN;
    }

    private void setPromptPrefix(String value) throws IOException {
        int startIndex = value.lastIndexOf('\n');
        if (startIndex == -1) {
            startIndex = 0;
        } else {
            startIndex = startIndex + 1;
        }
        int lastIndex = value.lastIndexOf('>');
        promptPrefix = value.substring(startIndex, lastIndex);
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

    public String changeMode(String changingMode) throws IOException,
            ConsoleException {
        if (currentMode.equals(changingMode)) {
            return "";
        }

        if (MODE_GLOBAL_CONFIG.equals(currentMode)
                && changingMode.startsWith(MODE_CONFIG_VLAN)) {
            sendln("vlan " + changingMode.substring(MODE_CONFIG_VLAN.length()));
            currentMode = changingMode;
            return translate(recieveToPrompt());
        } else if (currentMode.startsWith(MODE_CONFIG_VLAN)
                && changingMode.startsWith(MODE_GLOBAL_CONFIG)) {
            sendln("exit");
            currentMode = changingMode;
            return translate(recieveToPrompt());
        } else if (currentMode.startsWith(MODE_CONFIG_VLAN)
                && changingMode.startsWith(MODE_INTERFACE_CONFIG)) {
            sendln("interface "
                    + changingMode.substring(MODE_INTERFACE_CONFIG.length()));
            currentMode = changingMode;
            return translate(recieveToPrompt());
        } else if (MODE_LOGIN.equals(currentMode)
                && MODE_ENABLE.equals(changingMode)) {
            return changeLoginToEnableMode();
        } else if (MODE_ENABLE.equals(currentMode)
                && MODE_VLAN_DB.equals(changingMode)) {
            sendln("vlan database");
            currentMode = MODE_VLAN_DB;
            return translate(recieveToPrompt());
        } else if (MODE_VLAN_DB.equals(currentMode)
                && MODE_ENABLE.equals(changingMode)) {
            sendln("exit");
            currentMode = MODE_ENABLE;
            return translate(recieveToPrompt());
        } else if (MODE_ENABLE.equals(currentMode)
                && MODE_GLOBAL_CONFIG.equals(changingMode)) {
            sendln("configure terminal");
            currentMode = MODE_GLOBAL_CONFIG;
            return translate(recieveToPrompt());
        } else if (MODE_GLOBAL_CONFIG.equals(currentMode)
                && changingMode.startsWith(MODE_INTERFACE_CONFIG)) {
            sendln("interface "
                    + changingMode.substring(MODE_INTERFACE_CONFIG.length()));
            currentMode = changingMode;
            return translate(recieveToPrompt());
        } else if (currentMode.startsWith(MODE_INTERFACE_CONFIG)
                && changingMode.startsWith(MODE_INTERFACE_CONFIG)) {
            if (currentMode.equals(changingMode)) {
                return "";
            }
            sendln("interface "
                    + changingMode.substring(MODE_INTERFACE_CONFIG.length()));
            currentMode = changingMode;
            return translate(recieveToPrompt());
        } else if (currentMode.startsWith(MODE_INTERFACE_CONFIG)
                && MODE_GLOBAL_CONFIG.equals(changingMode)) {
            sendln("exit");
            currentMode = changingMode;
            return translate(recieveToPrompt());
        } else if (MODE_GLOBAL_CONFIG.equals(currentMode)
                && MODE_ENABLE.equals(changingMode)) {
            sendln("exit");
            currentMode = MODE_ENABLE;
            return translate(recieveToPrompt());
        } else {
            throw new UnsupportedOperationException("Current:" + currentMode
                    + " To:" + changingMode);
        }
    }

    private String changeLoginToEnableMode() throws IOException,
            ConsoleException {
        sendln("enable");
        receiveTo(ASK_PASSWORD);
        sendln(getNodeInfo().getAdminPassword());
        currentMode = MODE_ENABLE;
        return translate(recieveToPrompt());
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
        return translateAlaxalaResponse(value);
    }

    private static final byte[] MORE_NEXT_BYTES = {0x1b, 0x5b, 0x00, 0x3b,
            0x31, 0x48, 0x1b, 0x5b, 0x4b,};

    private static final byte[] MORE_NEXT_SIGNIFICANT_BYTE = {0x1b, 0x5b,
            0x4b,};
    private static final String MORE_NEXT_SIGNIFICANT = new String(
            MORE_NEXT_SIGNIFICANT_BYTE);

    private String translateAlaxalaResponse(String str) throws IOException {
        System.err.println("->: <" + str + ">");

        StringBuffer sb = new StringBuffer();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new StringReader(str));
            String line = null;
            boolean firstline = true;
            while ((line = reader.readLine()) != null) {
                if (line.contains(MORE_NEXT_SIGNIFICANT)) {
                    int index = line.lastIndexOf(MORE_NEXT_SIGNIFICANT)
                            + MORE_NEXT_SIGNIFICANT.getBytes().length;
                    String mayBeEscapeSequence = line.substring(0, index);
                    if (ByteArrayUtil.mayBeContains(mayBeEscapeSequence.getBytes(),
                            MORE_NEXT_BYTES)) {
                        line = line.substring(index);
                    }
                }
                if (firstline) {
                    firstline = false;
                } else {
                    sb.append("\r\n");
                }
                sb.append(line);
            }

        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        System.err.println("<-: [" + sb.toString() + "]");
        return sb.toString();
    }

    protected String recieveToPrompt() throws SocketException,
            ReceivingTimeoutException, IOException, ConsoleException {
        String expected = getPrompt();
        System.err.println("xx" + expected);
        return receiveTo(expected);
    }

    @Override
    public String getPrompt() {
        return getPromptPrefix() + getPromptSuffix();
    }

    protected String getPromptSuffix() {
        if (currentMode.startsWith(MODE_CONFIG_VLAN)) {
            return "(config-vlan)#";
        } else if (MODE_LOGIN.equals(currentMode)) {
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

    private static final byte[] MORE_END_BYTES = {0x1b, 0x5b, 0x6d};

    private static class MyMoreExecutor implements MoreExecutor {

        public String getString() {
            return " ";
        }

        public byte[] getSendMoreBytes(String value) {
            return " ".getBytes();
        }

        public boolean isMoreInput(byte[] value) {
            return ByteArrayUtil.mayBeContains(value, MORE_END_BYTES);
        }
    }
}