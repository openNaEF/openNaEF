package voss.discovery.agent.atmbridge.exatrax;

import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleException;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShowIpRoute extends EXAtraxInterface {
    String defaultGateway = null;

    public ShowIpRoute(ConsoleAccess telnet) throws IOException, ConsoleException, AbortedException {
        super(telnet);
        show_ip_route();
    }

    ShowIpRoute() throws IOException, ConsoleException, AbortedException {
        this(null);
    }

    void test() {
        System.out.println(this);
    }

    public String toString() {
        String str = "default gateway : " + defaultGateway;
        return str;
    }

    public String getDefaultGateway() {
        return defaultGateway;
    }

    boolean show_ip_route() throws IOException, ConsoleException, AbortedException {
        execute(EXAtraxDiscovery.show_ip_route);

        String str = skipto("^\\s*destination");
        if (str == null) {
            return false;
        }

        Pattern p = Pattern
                .compile("^\\s*0\\.0\\.0\\.0\\s*[\\da-fA-F]+\\s*(\\d*\\.*\\d*\\.*\\d*\\.*\\d*)");
        while (true) {
            Matcher m = p.matcher(str);

            if (m.lookingAt()) {
                defaultGateway = m.group(1);
                break;
            } else {
                str = getNext();
                if (str == null) {
                    return false;
                }
            }
        }

        return true;
    }
}