package voss.discovery.agent.atmbridge.exatrax;

import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShowLagAll extends EXAtraxInterface {
    LinkedList<Lag> lagList = null;

    public ShowLagAll(ConsoleAccess telnet) throws IOException, ConsoleException, AbortedException {
        super(telnet);
        lagList = new LinkedList<Lag>();
        show_lag_all();
    }

    ShowLagAll() throws Exception {
        this(null);
    }

    void test() {
        System.out.println(this);
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("LAG List\n");
        for (int i = 0; i < lagList.size(); i++) {
            Lag lag = lagList.get(i);
            str.append(" LAG ").append(lag.name).append("(").append(lag.key).append(")");
            str.append(" AggregatorPort : ").append(lag.aggregatorPort);
            str.append(" OperState : ").append(lag.operState);
            str.append(" AdminState : ").append(lag.adminState).append("\n");
            str.append("  EthernetPortList : ");
            for (int j = 0; j < lag.ethernetPortList.size(); j++) {
                str.append(lag.ethernetPortList.get(j));
                if (j < lag.ethernetPortList.size() - 1) {
                    str.append(" ");
                }
            }
            if (i < lagList.size() - 1) {
                str.append("\n");
            }
        }
        return str.toString();
    }

    public LinkedList<Lag> getLagList() {
        return lagList;
    }

    boolean show_lag_all() throws IOException, ConsoleException, AbortedException {
        execute(EXAtraxDiscovery.show_lag_all);

        String str = search("^\\s*Link\\s*Aggregation\\s*Group");
        if (str == null)
            return false;

        while (true) {
            Lag lag = null;
            {
                Pattern p = Pattern
                        .compile("^\\s*Link\\s*Aggregation\\s*Group\\s*\"([\\w_]+)\"\\s*,\\s*key\\s*:\\s*(\\d+)");
                Matcher m = p.matcher(str);

                if (m.lookingAt()) {
                    lag = new Lag(m.group(1));
                    lag.ethernetPortList = new LinkedList<Integer>();
                    lagList.add(lag);

                    lag.key = Integer.parseInt(m.group(2));
                } else {
                    return false;
                }
            }

            str = search("^\\s*Aggregator\\s*Port");
            if (str == null) {
                return false;
            }

            {
                Pattern p = Pattern
                        .compile("^\\s*Aggregator\\s*Port\\s*(\\d+)\\s*:\\s*(\\w+)/(\\w+)");
                Matcher m = p.matcher(str);

                if (m.lookingAt()) {
                    String aggregatorPortStr = m.group(1);
                    lag.aggregatorPort = Integer.parseInt(aggregatorPortStr);

                    String adminStateStr = m.group(2);
                    String operStateStr = m.group(3);

                    if (adminStateStr.equalsIgnoreCase("enable")) {
                        lag.adminState = 1;
                    } else if (adminStateStr.equalsIgnoreCase("disable")) {
                        lag.adminState = 0;
                    } else {
                        return false;
                    }

                    if (operStateStr.equalsIgnoreCase("up")) {
                        lag.operState = 1;
                    } else if (operStateStr.equalsIgnoreCase("down")) {
                        lag.operState = 0;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            str = getNext();
            if (str == null) {
                return false;
            }

            {
                Pattern p = Pattern.compile("^.*Ethernet\\s*Port\\s*(\\d+)");
                while (true) {
                    Matcher m = p.matcher(str);

                    if (m.lookingAt()) {
                        lag.ethernetPortList.add(Integer.parseInt(m.group(1)));
                    } else {
                        break;
                    }

                    str = getNext();
                    if (str == null) {
                        return true;
                    }
                }
            }
        }
    }
}