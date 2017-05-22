package voss.discovery.agent.atmbridge.exatrax;

import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.simpletelnet.GlobalMode;

import java.io.IOException;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShowPort extends EXAtraxInterface {
    int port = -1;
    LinkedList<String> vlanList = null;
    LinkedList<Integer> isTagList = null;
    String name = null;
    int adminState = -1;
    int operState = -1;
    int pvid = -1;
    int tagType = -1;

    public ShowPort(ConsoleAccess telnet, int port) throws IOException,
            ConsoleException, AbortedException {
        super(telnet);
        this.port = port;

        vlanList = new LinkedList<String>();
        isTagList = new LinkedList<Integer>();

        show_port(port);
    }

    ShowPort(int port) throws Exception {
        this(null, port);
    }

    void test() {
        System.out.println(this);
    }

    public String toString() {
        String str = new String();
        str += "BridgePort " + port + "\n";

        str += " VLANs which contain this port : ";
        for (int i = 0; i < vlanList.size(); i++) {
            int isTag = isTagList.get(i).intValue();
            switch (isTag) {
                case 0:
                    str += "U:";
                    break;
                case 1:
                    str += "T:";
                    break;
                default:
                    str += "!:";
            }

            str += vlanList.get(i);

            if (i < vlanList.size() - 1) {
                str += " ";
            }
        }

        return str;
    }

    public LinkedList<String> getVlanList() {
        return vlanList;
    }

    public int getAdminState() {
        return adminState;
    }

    public int getOperState() {
        return operState;
    }

    boolean show_port(int port) throws IOException, ConsoleException,
            AbortedException {
        ConsoleCommand command = new ConsoleCommand(new GlobalMode(),
                "show port " + port);
        execute(command);

        String str = search("^\\s*Name");
        if (str == null)
            return false;

        {
            Pattern p = Pattern.compile("^\\s*Name\\s*:\\s*\"([\\w_]+)\"");
            Matcher m = p.matcher(str);

            if (m.lookingAt()) {
                name = m.group(1);
            }
        }

        str = search("^\\s*Enable\\s*/\\s*Disable");
        if (str == null)
            return false;

        {
            Pattern p = Pattern
                    .compile("^\\s*Enable\\s*/\\s*Disable\\s*:\\s(\\W*)");
            Matcher m = p.matcher(str);
            if (m.lookingAt()) {
                String adminStatusStr = m.group(1);
                if (adminStatusStr.equalsIgnoreCase("enable")) {
                    adminState = 1;
                } else if (adminStatusStr.equalsIgnoreCase("disable")) {
                    adminState = 0;
                }
            }
        }

        str = search("^\\s*State");
        if (str == null)
            return false;

        {
            Pattern p = Pattern.compile("^\\s*State\\s*:\\s*(\\W*)");
            Matcher m = p.matcher(str);
            if (m.lookingAt()) {
                String operStatusStr = m.group(1);
                if (operStatusStr.equalsIgnoreCase("up")) {
                    operState = 1;
                } else if (operStatusStr.equalsIgnoreCase("down")) {
                    operState = 0;
                }
            }
        }

        str = search("^\\s*PVID\\s");
        if (str == null)
            return false;

        {
            Pattern p = Pattern.compile("^\\s*PVID\\s*:\\s*(\\d+)");
            Matcher m = p.matcher(str);
            if (m.lookingAt()) {
                pvid = Integer.parseInt(m.group(1));
            }
        }

        str = search("^\\s*TAG\\s*Type\\s*\\(\\s*tagtype\\s*\\)");
        if (str == null)
            return false;

        {
            Pattern p = Pattern
                    .compile("^\\s*TAG\\s*Type\\s*\\(\\s*tagtype\\s*\\)\\s*:\\s*(\\d+)");
            Matcher m = p.matcher(str);
            if (m.lookingAt()) {
                tagType = Integer.parseInt(m.group(1), 16);
            }
        }

        str = search("^\\s*VLAN\\s*VLAN-ID");
        if (str == null) {
            return false;
        }

        Pattern p = Pattern.compile("^\\s*([\\w_]+)\\s*(\\d+)\\s*(\\w+)");
        while (true) {
            Matcher m = p.matcher(str);

            if (m.lookingAt()) {
                vlanList.add(m.group(1));

                String isTagStr = m.group(3);
                if (isTagStr.equalsIgnoreCase("on")) {
                    isTagList.add(new Integer(1));
                } else if (isTagStr.equalsIgnoreCase("off")) {
                    isTagList.add(new Integer(0));
                }
            }

            str = getNext();
            if (str == null) {
                break;
            }
        }

        return true;
    }

}