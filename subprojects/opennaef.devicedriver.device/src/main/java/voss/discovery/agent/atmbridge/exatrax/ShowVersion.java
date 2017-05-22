package voss.discovery.agent.atmbridge.exatrax;

import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleException;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShowVersion extends EXAtraxInterface {
    String swDate = null;

    String swVersion = null;

    String localMacAddr = null;

    int[] extBoard = null;

    String bootSystem = null;

    String startupConfig = null;

    public ShowVersion(ConsoleAccess telnet) throws IOException, ConsoleException, AbortedException {
        super(telnet);
        extBoard = new int[2 + 1];
        for (int i = 0; i < extBoard.length; i++) {
            extBoard[i] = -1;
        }
        show_version();
    }

    ShowVersion() throws Exception {
        this(null);
    }

    void test() {
        System.out.println(this);
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("show version\n");
        str.append(" Date : ").append(getSwDate()).append("\n");
        str.append(" Version : ").append(getSwVersion()).append("\n");
        str.append(" ExtBoard(1) : ").append(isExtBoardPresence(1)).append("\n");
        str.append(" ExtBoard(2) : ").append(isExtBoardPresence(2)).append("\n");
        str.append(" LocalMacAddr : ").append(getLocalMacAddr()).append("\n");
        str.append(" BootSystemName : ").append(getBootSystemName()).append("\n");
        str.append(" StartupConfigName : ").append(getStartupConfigName());

        return str.toString();
    }

    public String getSwDate() {
        return swDate;
    }

    public String getSwVersion() {
        return swVersion;
    }

    public String getLocalMacAddr() {
        return localMacAddr;
    }

    public int isExtBoardPresence(int slot) {
        if (slot < 1 || slot >= extBoard.length) {
            return 0;
        }

        return extBoard[slot];
    }

    public String getBootSystemName() {
        return bootSystem;
    }

    public String getStartupConfigName() {
        return startupConfig;
    }

    void show_version() throws IOException, ConsoleException, AbortedException {
        execute(EXAtraxDiscovery.show_version);

        String str = search("\\s*#\\s*NS-6100");
        if (str == null)
            return;

        {
            Pattern p = Pattern
                    .compile("^\\s*#\\s*NS-6100\\s*series\\s*System\\s*Software\\s*([\\d\\.]*)\\s*\\(Ver\\s*([^\\)]*)\\)");
            Matcher m = p.matcher(str);
            if (m.lookingAt()) {
                swDate = m.group(1);
                swVersion = m.group(2);
            }
        }

        str = search(str, "^\\s*Local\\s*MAC\\s*Address");
        if (str == null)
            return;

        {
            Pattern p = Pattern
                    .compile("^\\s*Local\\s*MAC\\s*Address\\s*:\\s*([\\da-fA-F:]*)");
            Matcher m = p.matcher(str);
            if (m.lookingAt()) {
                localMacAddr = m.group(1);
            }
        }

        str = search(str, "\\s*Slot\\s*\\d+");
        if (str == null)
            return;

        {
            Pattern p = Pattern.compile("\\s*Slot\\s*(\\d+)\\s*:\\s*(.*)");
            Pattern pB = Pattern
                    .compile("Ext\\.\\s*board\\s*\\(ATM[^)]*\\)\\s*detected");

            for (int i = 1; i < extBoard.length; i++) {
                Matcher m = p.matcher(str);

                if (!m.lookingAt())
                    break;

                int slot = -1;
                slot = Integer.parseInt(m.group(1));

                String board = m.group(2);

                Matcher mB = pB.matcher(board);
                if (mB.lookingAt()) {
                    extBoard[slot] = 1;
                } else {
                    extBoard[slot] = 0;
                }

                str = getNext();
                if (str == null)
                    return;
            }
        }

        str = search(str, "^\\s*Boot\\s*System");

        {
            Pattern p = Pattern
                    .compile("^\\s*Boot\\s*System\\s*:\\s*([^,]*),\\s*Startup\\s*(\\d*)");
            Matcher m = p.matcher(str);
            if (m.lookingAt()) {
                bootSystem = m.group(1);
                startupConfig = m.group(2);
            }
        }
    }
}