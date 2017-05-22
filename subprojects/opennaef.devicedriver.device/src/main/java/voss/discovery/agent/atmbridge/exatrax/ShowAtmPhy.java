package voss.discovery.agent.atmbridge.exatrax;

import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.simpletelnet.GlobalMode;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShowAtmPhy extends EXAtraxInterface {
    int operState = -1;

    int slot = -1;

    int port = -1;

    public ShowAtmPhy(ConsoleAccess telnet, int slot, int port)
            throws IOException, ConsoleException, AbortedException {
        super(telnet);
        this.slot = slot;
        this.port = port;
        show_atm_phy(slot, port);
    }

    ShowAtmPhy(int slot, int port) throws IOException, ConsoleException, AbortedException {
        this(null, slot, port);
    }

    void test() {
        System.out.println(this);
    }

    public String toString() {
        String str = "ATM Port " + slot + "/" + port + "\n"
                + " OperState : " + operState;
        return str;
    }

    public int getOperState() {
        return operState;
    }

    boolean show_atm_phy(int slot, int port) throws IOException,
            ConsoleException, AbortedException {
        ConsoleCommand command = new ConsoleCommand(new GlobalMode(),
                "show atm " + slot + "/" + port + " phy");
        execute(command);

        String str = search("^\\s*Mode\\s*:");
        if (str == null) {
            return false;
        }

        Pattern p = Pattern
                .compile("^\\s*Mode\\s*:\\s*\\w*\\([\\w-]*\\)\\s*TxClkSrc\\s*:\\s*\\w*\\s*\\w*\\(\\w*\\s*\\w*\\)\\s*State\\s*:\\s*(\\w+)");
        Matcher m = p.matcher(str);

        if (m.lookingAt()) {
            String stateStr = m.group(1);
            if (stateStr.equalsIgnoreCase("up")) {
                operState = 1;
            } else if (stateStr.equalsIgnoreCase("down")) {
                operState = 0;
            }
        } else {
            return false;
        }

        return true;
    }
}