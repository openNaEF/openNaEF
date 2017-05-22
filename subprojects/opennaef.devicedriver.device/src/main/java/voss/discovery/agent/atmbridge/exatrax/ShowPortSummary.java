package voss.discovery.agent.atmbridge.exatrax;

import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.simpletelnet.GlobalMode;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShowPortSummary extends EXAtraxInterface {

    BridgePort bridge = null;

    public ShowPortSummary(ConsoleAccess telnet, int port) throws IOException, ConsoleException, AbortedException {
        super(telnet);
        show_port_summary(port);
    }

    ShowPortSummary(int port) throws Exception {
        this(null, port);
    }

    void test() {
        System.out.println(this);
    }

    public String toString() {
        if (bridge == null)
            return "BridgePort";
        String str = "BridgePort " + bridge.port + "\n";
        str += " AdminState : " + bridge.adminState + "\n";
        str += " OperState : " + bridge.operState + "\n";
        str += " Name : " + bridge.name + "\n";
        str += " PVID " + bridge.pvid;

        return str;
    }

    public BridgePort getBridgePort() {
        return bridge;
    }

    public String getName() {
        if (bridge != null)
            return bridge.name;
        return null;
    }

    public int getAdminState() {
        if (bridge != null)
            return bridge.adminState;
        return -1;
    }

    public int getOperState() {
        if (bridge != null)
            return bridge.operState;
        return -1;
    }

    boolean show_port_summary(int port) throws IOException, ConsoleException,
            AbortedException {
        ConsoleCommand command = new ConsoleCommand(new GlobalMode(),
                "show port " + port + " summary");
        execute(command);

        String str = skipto("^\\s*port\\s*Ena\\s*St\\s");
        if (str == null)
            return false;

        Pattern p = Pattern
                .compile("^\\s*(\\d+)\\s*(\\w+)\\s*(\\w+)\\s*(\\w+)\\s*([\\d\\/]+)\\s*(\\S+)\\s*(\\S+)\\s*(\\S+)");
        Matcher m = p.matcher(str);

        if (m.lookingAt()) {

            bridge = new BridgePort(port);

            String enaStr = m.group(2);
            if (enaStr.equalsIgnoreCase("ena"))
                bridge.adminState = 1;
            else if (enaStr.equalsIgnoreCase("dis"))
                bridge.adminState = 0;

            String stStr = m.group(3);
            if (stStr.equalsIgnoreCase("up"))
                bridge.operState = 1;
            else if (stStr.equalsIgnoreCase("down"))
                bridge.operState = 0;

            int nameGrp = 6;
            if (m.group(6).equalsIgnoreCase("(gbe)")) {
                nameGrp++;
            }

            bridge.name = m.group(nameGrp);

            bridge.pvid = Integer.parseInt(m.group(nameGrp + 1));

            return true;
        }
        return false;
    }

}