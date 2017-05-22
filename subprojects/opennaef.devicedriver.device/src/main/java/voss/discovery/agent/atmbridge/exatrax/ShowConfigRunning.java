package voss.discovery.agent.atmbridge.exatrax;

import voss.discovery.iolib.console.ConsoleAccess;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShowConfigRunning extends EXAtraxInterface {

    LinkedList<IpAddress> trapHostList = null;
    LinkedList<IpAddress> syslogHostList = null;
    String hostname = null;

    public ShowConfigRunning(ConsoleAccess telnet) throws Exception {
        super(telnet);
        trapHostList = new LinkedList<IpAddress>();
        syslogHostList = new LinkedList<IpAddress>();

        show_config_running();
    }

    ShowConfigRunning() throws Exception {
        this(null);
    }

    void test() {
        System.out.println(this);
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("hostname : ").append(hostname).append("\n");
        str.append("trap host : ");
        for (int i = 0; i < trapHostList.size(); i++) {
            IpAddress host = (IpAddress) trapHostList.get(i);
            str.append(host.getAddressStr());
            if (i < trapHostList.size() - 1) {
                str.append(", ");
            }
        }
        str.append("\n");

        str.append("syslog host : ");
        for (int i = 0; i < syslogHostList.size(); i++) {
            IpAddress host = (IpAddress) syslogHostList.get(i);
            str.append(host.getAddressStr());
            if (i < syslogHostList.size() - 1) {
                str.append(", ");
            }
        }

        return str.toString();
    }

    public LinkedList<IpAddress> getTrapReceiverList() {
        return trapHostList;
    }

    public LinkedList<IpAddress> getSyslogHostList() {
        return syslogHostList;
    }

    public String getHostName() {
        return hostname;
    }

    boolean show_config_running() throws Exception {
        execute(EXAtraxDiscovery.show_config_running);

        String str = search("^\\s*set\\s*hostname");
        if (str != null) {
            Pattern pHost = Pattern.compile("^\\s*set\\s*hostname\\s*(.+)");
            Matcher mHost = pHost.matcher(str);
            if (mHost.lookingAt()) {
                hostname = mHost.group(1);
            }
        } else {
            execute(EXAtraxDiscovery.show_config_running);
        }

        Pattern pTrap = Pattern
                .compile("^\\s*set\\s*trap\\s*\\d+\\s*manager\\s*([\\d\\.]+)");

        while (true) {
            str = search("^\\s*set\\s*trap\\s*\\d+\\s*manager");
            if (str == null)
                break;

            Matcher m = pTrap.matcher(str);
            if (m.lookingAt()) {
                trapHostList.add(new IpAddress(m.group(1)));
            } else {
                break;
            }
        }

        execute(EXAtraxDiscovery.show_config_running);

        Pattern pSyslog = Pattern
                .compile("^\\s*set\\s*syslog\\s*host\\s*([\\d\\.]+)");

        while (true) {
            str = search("^\\s*set\\s*syslog\\s*host");
            if (str == null)
                break;

            Matcher m = pSyslog.matcher(str);
            if (m.lookingAt()) {
                syslogHostList.add(new IpAddress(m.group(1)));
            } else {
                break;
            }
        }

        return true;
    }
}