package voss.discovery.agent.atmbridge.exatrax;

import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EXAtraxInterface {
    String tokens[];
    int idx = -1;
    ConsoleAccess telnet;

    EXAtraxInterface(ConsoleAccess telnet) {
        this.telnet = telnet;
        tokens = new String[0];
    }

    EXAtraxInterface() {
        this(null);
    }

    void execute(ConsoleCommand command) throws IOException, ConsoleException,
            AbortedException {
        String res = telnet.getResponse(command);
        tokens = res.split("\n");
        idx = 0;
    }

    String getNext() {
        if (idx < tokens.length)
            return tokens[idx++].trim();
        else
            return null;
    }

    String search(String str, String pStr) {
        Pattern p = Pattern.compile(pStr);

        while (true) {
            Matcher m = p.matcher(str);

            if (m.lookingAt()) {
                return str;
            }

            str = getNext();
            if (str == null) {
                return null;
            }
        }
    }

    String search(String pStr) {
        Pattern p = Pattern.compile(pStr);
        String str = null;

        while (true) {
            str = getNext();
            if (str == null) {
                return null;
            }

            Matcher m = p.matcher(str);

            if (m.lookingAt()) {
                return str;
            }

        }
    }

    String skipto(String str, String pStr) {
        search(str, pStr);
        return getNext();
    }

    String skipto(String pStr) {
        search(pStr);
        return getNext();
    }

    static int rateStrToInt(String str) {
        int rate = -1;
        Pattern p = Pattern.compile("(\\d+)\\s*([kKmMgG])");
        Matcher m = p.matcher(str);

        if (m.lookingAt()) {
            try {
                rate = Integer.parseInt(m.group(1));
            } catch (Exception e) {
                return -1;
            }

            String unitStr = m.group(2);
            if (unitStr.equalsIgnoreCase("K")) {
                return rate;
            } else if (unitStr.equalsIgnoreCase("M")) {
                return rate * 1000;
            } else if (unitStr.equalsIgnoreCase("G")) {
                return rate * 1000000;
            }
        }

        return -1;
    }
}