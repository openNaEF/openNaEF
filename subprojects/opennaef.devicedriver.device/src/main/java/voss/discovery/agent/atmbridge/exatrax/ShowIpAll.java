package voss.discovery.agent.atmbridge.exatrax;

import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShowIpAll extends EXAtraxInterface {
    LinkedList<IpAddress> defaultAddresses = null;

    public ShowIpAll(ConsoleAccess telnet) throws IOException,
            ConsoleException, AbortedException {
        super(telnet);

        defaultAddresses = new LinkedList<IpAddress>();

        show_ip_all();
    }

    ShowIpAll() throws IOException, ConsoleException, AbortedException {
        this(null);
    }

    void test() {
        System.out.println(this);
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("default address\n");

        for (int i = 0; i < defaultAddresses.size(); i++) {
            str.append(defaultAddresses.get(i));
            if (i < defaultAddresses.size() - 1) {
                str.append("\n");
            }
        }

        return str.toString();
    }

    public IpAddress getDefaultAddress(int i) {
        if (i >= 0 && i < defaultAddresses.size()) {
            return defaultAddresses.get(i);
        }

        return null;
    }

    boolean show_ip_all() throws IOException, ConsoleException,
            AbortedException {
        execute(EXAtraxDiscovery.show_ip_all);

        String str = skipto("^\\s*IP\\s*\"default\"");
        if (str == null) {
            return false;
        }

        str = search("^\\s*IP\\s*address\\s*\\(1\\)");
        if (str == null) {
            return false;
        }

        Pattern p = Pattern
                .compile("^\\s*IP\\s*address\\s*\\(1\\)\\s*([^\\(]+)\\s*\\(2\\)\\s*(.*)");
        {
            Matcher m = p.matcher(str);

            if (m.lookingAt()) {
                Pattern pAddr = Pattern
                        .compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)\\s*/\\s*(\\d+\\.\\d+\\.\\d+\\.\\d+)");
                for (int i = 1; i <= 2; i++) {
                    Matcher mAddr = pAddr.matcher(m.group(i));

                    if (mAddr.lookingAt()) {
                        defaultAddresses.add(new IpAddress(mAddr.group(1),
                                mAddr.group(2)));
                    }
                }
            }
        }
        return true;
    }
}