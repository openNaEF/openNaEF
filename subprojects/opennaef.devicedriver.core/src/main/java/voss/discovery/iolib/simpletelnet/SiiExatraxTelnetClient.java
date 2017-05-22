package voss.discovery.iolib.simpletelnet;

import voss.discovery.iolib.console.ConsoleClient;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.utils.ByteArrayUtil;
import voss.model.NodeInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.SocketException;

public class SiiExatraxTelnetClient extends RealTelnetClient {

    private static final MoreExecutor MY_MORE_EXECUTOR = new MyMoreExecutor();

    private String promptPrefix;
    private String currentMode = MODE_NOT_LOGIN;

    public SiiExatraxTelnetClient(TerminalSocket socket, NodeInfo nodeinfo) {
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

    private String fixExatraxBug(String original) {
        byte[] illegalPattern = {0x0a, 0x00, 0x0a};
        String illegal = new String(illegalPattern);

        byte[] illegalPattern2 = {0x0d, 0x00, 0x0a};
        String illegal2 = new String(illegalPattern2);

        byte[] validPattern = {0x0d, 0x0a};
        String valid = new String(validPattern);

        String result = original.replaceAll(illegal, valid);
        result = result.replaceAll(illegal2, valid);
        return result;
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
            receiveTo("passwd:");
            sendln(getNodeInfo().getUserPassword());
        } else {
            throw new ConsoleException("Unexpected Response");
        }
        setPromptPrefix(receiveTo("> "));
        sendln("");
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
        if (currentMode.equals(changingMode)) {
            sendln("");
            return translate(recieveToPrompt());
        }

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
        sendln("su");
        receiveTo("passwd:");
        sendln(getNodeInfo().getAdminPassword());
        currentMode = MODE_ENABLE;
        return translate(recieveToPrompt());
    }

    private String changeEnableToLoginMode() throws IOException,
            ConsoleException {
        sendln("logout");
        currentMode = MODE_LOGIN;
        return translate(recieveToPrompt());
    }

    public void logout() throws ConsoleException, IOException {
        if (currentMode.equals(MODE_NOT_LOGIN)) {
            return;
        }
        if (currentMode.equals(MODE_ENABLE)) {
            sendln("console off");
            sendln("logout");
            currentMode = MODE_LOGIN;
        }
        sendln("console off");
        sendln("logout");
        close();
        currentMode = MODE_NOT_LOGIN;
    }

    protected final MoreExecutor getMoreExecutor() {
        return MY_MORE_EXECUTOR;
    }

    protected final String translateNewLine(String value) throws IOException {
        return fixExatraxBug(value);
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
            return ByteArrayUtil.contains(value, "--More--".getBytes());
        }

    }

}