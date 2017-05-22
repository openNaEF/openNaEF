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

public class ShowVlanSummary extends EXAtraxInterface {
    String vlanName = null;
    int vlanId = -1;

    LinkedList<Integer> taggedPortList = null;
    LinkedList<Integer> unTaggedPortList = null;

    public ShowVlanSummary(ConsoleAccess telnet, String vlan) throws IOException, ConsoleException, AbortedException {
        super(telnet);
        vlanName = vlan;

        taggedPortList = new LinkedList<Integer>();
        unTaggedPortList = new LinkedList<Integer>();

        show_vlan_summary(vlan);
    }

    ShowVlanSummary(String vlan) throws IOException, ConsoleException, AbortedException {
        this(null, vlan);
    }

    void test() {
        System.out.println(this);
    }

    public String toString() {
        StringBuilder str = new StringBuilder();

        str.append("VLAN ").append(vlanName).append("\n");
        str.append(" VLAN ID : ").append(vlanId).append("\n");
        str.append(" TaggedPort : ");
        for (int i = 0; i < taggedPortList.size(); i++) {
            str.append(taggedPortList.get(i));
            if (i < taggedPortList.size() - 1) {
                str.append(" ");
            }
        }

        str.append("\n");

        str.append(" UntaggedPort : ");
        for (int i = 0; i < unTaggedPortList.size(); i++) {
            str.append(unTaggedPortList.get(i));
            if (i < unTaggedPortList.size() - 1) {
                str.append(" ");
            }
        }

        return str.toString();
    }

    public int getVlanId() {
        return vlanId;
    }

    public LinkedList<Integer> getTaggedPortList() {
        return taggedPortList;
    }

    public LinkedList<Integer> getUnTaggedPortList() {
        return unTaggedPortList;
    }

    boolean show_vlan_summary(String vlan) throws IOException, ConsoleException, AbortedException {
        ConsoleCommand command = new ConsoleCommand(new GlobalMode(),
                "show vlan " + vlan + " summary");
        execute(command);

        String str = skipto("^\\s*VlanName");
        if (str == null) {
            return false;
        }

        Pattern pVlan = Pattern.compile("^\\s*(\\w+)\\s*(\\d+)\\s*(.*)");

        Matcher mVlan = pVlan.matcher(str);

        if (!mVlan.lookingAt())
            throw new IOException();

        vlanId = Integer.parseInt(mVlan.group(2));
        String portsStr = mVlan.group(3);

        boolean taggedCont = true;
        boolean unTaggedCont = true;

        Pattern pPorts = Pattern.compile("\\s*([\\d,-]*)\\s*([\\d,-]*)");

        while (true) {
            Matcher m = pPorts.matcher(portsStr);

            if (m.lookingAt()) {
                int group = 1;

                if (taggedCont) {
                    String taggedPortsStr = m.group(group++);
                    if (taggedPortsStr.equals("-")) {
                        taggedCont = false;
                    } else {
                        taggedCont = addPortsToList(taggedPortsStr,
                                taggedPortList);
                    }
                }

                if (unTaggedCont) {
                    String unTaggedPortsStr = m.group(group);
                    if (unTaggedPortsStr.equals("-")) {
                        unTaggedCont = false;
                    } else {
                        unTaggedCont = addPortsToList(unTaggedPortsStr,
                                unTaggedPortList);
                    }
                }

                if (taggedCont || unTaggedCont) {
                    portsStr = getNext();
                    if (portsStr == null) {
                        throw new IOException();
                    }
                } else {
                    break;
                }
            } else {
                throw new IOException();
            }
        }
        return true;
    }

    private boolean addPortsToList(String ports, LinkedList<Integer> list)
            throws IOException, ConsoleException, AbortedException {
        if (ports.length() == 0)
            return false;

        String port[] = ports.split(",");
        for (int i = 0; i < port.length; i++) {
            list.add(Integer.parseInt(port[i]));
        }

        if (ports.charAt(ports.length() - 1) == ',')
            return true;

        return false;
    }
}