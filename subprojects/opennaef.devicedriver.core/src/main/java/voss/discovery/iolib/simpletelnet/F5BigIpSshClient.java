package voss.discovery.iolib.simpletelnet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.console.ConsoleClient;
import voss.discovery.iolib.console.ConsoleException;
import voss.model.NodeInfo;
import voss.util.VossMiscUtility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class F5BigIpSshClient extends RealTelnetClient {
    public static final String MODE_ROOT = "(tmos)";
    public static final String MODE_SYS_CONFIG = "(tmos.sys.config)";

    private final MoreExecutor MY_MORE_EXECUTOR = new MyMoreExecutor();
    private final Logger log;
    private String promptPrefix;
    private String currentMode = MODE_NOT_LOGIN;
    private boolean terminalInitialized;
    private List<String> initializeCommands;

    public F5BigIpSshClient(TerminalSocket socket, NodeInfo nodeinfo) {
        super(nodeinfo);
        this.log = LoggerFactory.getLogger(getClass());
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
        if (currentMode.equals(changingMode)) {
            return "";
        }
        log.debug("mode change: " + this.currentMode + " -> " + changingMode);
        if (MODE_ROOT.equals(currentMode)
                && MODE_SYS_CONFIG.equals(changingMode)) {
            sendln("sys");
            sendln("config");
            currentMode = MODE_SYS_CONFIG;
            return translate(receiveToPrompt());
        } else if (MODE_SYS_CONFIG.equals(currentMode)
                && MODE_ROOT.equals(changingMode)) {
            sendln("exit");
            sendln("exit");
            currentMode = MODE_ROOT;
            return translate(receiveToPrompt());
        } else {
            throw new UnsupportedOperationException("Current:" + currentMode
                    + " To:" + changingMode);
        }
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

    private static final String PROMPT_SUFFIX_ENABLE = "# ";

    public void loginBySsh() throws IOException, ConsoleException {
        connect();
        setPromptPrefix(receiveTo(PROMPT_SUFFIX_ENABLE));
        currentMode = MODE_ROOT;
    }

    public void loginByTelnet() throws IOException, ConsoleException {
        throw new IllegalArgumentException("not supported.");
    }

    private void setPromptPrefix(String value) throws IOException {
        int startIndex = value.lastIndexOf('\n');
        if (startIndex == -1) {
            startIndex = 0;
        } else {
            startIndex = startIndex + 1;
        }
        int lastIndex = value.lastIndexOf('#');
        this.promptPrefix = value.substring(startIndex, lastIndex).replaceAll("\\*", "");
        this.promptPrefix = promptPrefix.replace(MODE_ROOT, "");
    }

    String getPromptPrefix() {
        return promptPrefix;
    }

    public void logout() throws ConsoleException, IOException {
        if (currentMode.equals(MODE_NOT_LOGIN)) {
            return;
        }
        sendln("quit");
        close();
        currentMode = MODE_NOT_LOGIN;
    }

    protected final MoreExecutor getMoreExecutor() {
        return MY_MORE_EXECUTOR;
    }

    protected final String translateNewLine(String value) throws IOException {
        return translateBigIpResponse(value);
    }

    private static final byte[] MORE_END_SIGNIFICANT_BYTE = {0x1b, 0x5b, 0x6d, 0x1b, 0x5b, 0x4b,};
    private static final String MORE_END_SIGNIFICANT = new String(MORE_END_SIGNIFICANT_BYTE).intern();
    private static final byte[] MORE_ERASE_SIGNIFICANT_BYTE = {0x1b, 0x5b, 0x4b,};
    private static final String MORE_ERASE_SIGNIFICANT = new String(MORE_ERASE_SIGNIFICANT_BYTE).intern();
    private static final byte[] UNKNOWN_SIGNIFICANT1_BYTE = {0x1b, 0x5b, 0x3f, 0x31, 0x68, 0x1b, 0x3d};
    private static final String UNKNOWN_SIGNIFICANT1 = new String(UNKNOWN_SIGNIFICANT1_BYTE).intern();

    private static final byte[] LESS_END_BYTE = {0x1b, 0x5b, 0x37, 0x6d,
            '(', 'E', 'N', 'D', ')', 0x1b, 0x5b, 0x6d, 0x1b, 0x5b, 0x4b,};
    private static final String LESS_END = new String(LESS_END_BYTE).intern();

    private static final String CONFIRM = " items? (y/n) ";

    protected String translateBigIpResponse(String str) throws IOException {
        StringBuffer sb = new StringBuffer();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new StringReader(str));
            String line = null;
            while ((line = reader.readLine()) != null) {
                log.trace("< " + VossMiscUtility.showDetail(line));
                if (line.isEmpty()) {
                    log.trace("> CR+LF");
                    sb.append("\r\n");
                    continue;
                }
                if (line.charAt(0) == 0x00) {
                    line = line.substring(1);
                    if (line.isEmpty()) {
                        log.trace("> __DO_NOTHING__");
                        continue;
                    }
                }
                if (line.contains(CONFIRM) || line.contains(UNKNOWN_SIGNIFICANT1)) {
                    log.trace("> __DO_NOTHING_ABOUT_CONFIRM__");
                    continue;
                }
                if (line.contains(MORE_END_SIGNIFICANT)) {
                    log.trace("> __DO_NOTHING_ABOUT_LESS__");
                    continue;
                }
                if (line.contains(MORE_ERASE_SIGNIFICANT)) {
                    int index = line.lastIndexOf(MORE_ERASE_SIGNIFICANT)
                            + MORE_ERASE_SIGNIFICANT.getBytes().length;
                    line = line.substring(index);
                }
                line = line + "\r\n";
                log.trace("> " + VossMiscUtility.showDetail(line));
                sb.append(line);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        String result = sb.toString();
        if (result.endsWith("\r\n")) {
            result = result.substring(0, result.length() - 2);
        }
        return result;
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
        if (MODE_ROOT.equals(currentMode)) {
            return MODE_ROOT + PROMPT_SUFFIX_ENABLE;
        } else if (MODE_SYS_CONFIG.equals(currentMode)) {
            return MODE_SYS_CONFIG + PROMPT_SUFFIX_ENABLE;
        }
        throw new UnsupportedOperationException("Current:" + currentMode);
    }

    private class MyMoreExecutor implements MoreExecutor {
        public String getString() {
            return " ";
        }

        public byte[] getSendMoreBytes(String value) {
            if (value.endsWith(LESS_END)) {
                byte[] esc = {'q'};
                return esc;
            } else if (value.endsWith(CONFIRM)) {
                byte[] yes = {'y'};
                return yes;
            }
            return " ".getBytes();
        }

        public boolean isMoreInput(byte[] value) {
            String line = new String(value);
            return line.endsWith(MORE_END_SIGNIFICANT)
                    || line.endsWith(LESS_END)
                    || line.endsWith(CONFIRM);
        }
    }

    @SuppressWarnings("serial")
    public static final class BigIpSysConfigMode implements ModeChanger {

        @Override
        public void changeMode(ConsoleClient client) throws IOException, ConsoleException {
            client.execute("sys");
            client.execute("config");
        }

        @Override
        public String getModeName() {
            return MODE_SYS_CONFIG;
        }
    }
}