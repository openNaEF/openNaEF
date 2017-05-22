package voss.discovery.agent.atmbridge.exatrax;

import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.simpletelnet.GlobalMode;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShowPvc extends EXAtraxInterface {
    Pvc pvc = null;

    public ShowPvc(ConsoleAccess telnet, int slot, int port, int vpi, int vci)
            throws IOException, ConsoleException, AbortedException {
        super(telnet);
        show_pvc(slot, port, vpi, vci);
    }

    ShowPvc(int slot, int port, int vpi, int vci) throws Exception {
        this(null, slot, port, vpi, vci);
    }

    void test() {
        if (pvc != null)
            System.out.println(this);
    }

    public String toString() {
        StringBuilder str = new StringBuilder();

        str.append("PVC ").append(pvc.slot).append("/")
                .append(pvc.port).append("/").append(pvc.vpi)
                .append("/").append(pvc.vci).append("\n");
        if (pvc.overSub == 1)
            str.append(" OverSubscription\n");
        str.append(" PCR : " + pvc.pcr / 1000 + "M\n");
        if (pvc.overSub == 1)
            str.append(" mCR : ").append(pvc.mcr).append("K\n");
        str.append(" AdminState : ").append(pvc.adminState).append("\n");
        str.append(" AssociatePort : ").append(pvc.associatePort).append("\n");
        str.append(" AssociatePortName : ").append(pvc.associatePortName).append("\n");
        str.append(" AssociatePortOperState : ").append(pvc.bridgeOperState).append("\n");
        str.append(" AssociatePortAdminState : ").append(pvc.bridgeAdminState);

        return str.toString();
    }

    public int getPcr() {
        if (pvc != null) {
            return pvc.pcr;
        }
        return -1;
    }

    public int getMcr() {
        if (pvc != null) {
            return pvc.mcr;
        }
        return -1;
    }

    public int getAssociatePortNumber() {
        if (pvc != null) {
            return pvc.associatePort;
        }
        return -1;
    }

    public int getAdminState() {
        if (pvc != null) {
            return pvc.adminState;
        }
        return -1;
    }

    public int getBridgeOperState() {
        if (pvc != null) {
            return pvc.bridgeOperState;
        }
        return -1;
    }

    boolean show_pvc(int slot, int port, int vpi, int vci) throws IOException,
            ConsoleException, AbortedException {
        ConsoleCommand command = new ConsoleCommand(new GlobalMode(),
                "show pvc " + slot + "/" + port + "/" + vpi + "/" + vci);
        execute(command);

        String str = search("^\\s*Slot\\s*:");
        if (str == null) {
            return false;
        }

        pvc = new Pvc(slot, port, vpi, vci);
        {
            Pattern p = Pattern.compile("OverSubscription");
            Matcher m = p.matcher(str);

            if (m.find()) {
                pvc.overSub = 1;
            } else {
                pvc.overSub = 0;
            }

        }

        str = search("^\\s*PCR\\s*:");
        if (str == null) {
            return false;
        }

        {
            Pattern p = Pattern
                    .compile("^\\s*PCR\\s*:\\s*(\\w+)\\(\\w*\\)\\s*PVC\\s*State\\s*:\\s*(\\w+)");
            Matcher m = p.matcher(str);

            if (m.lookingAt()) {
                pvc.pcr = rateStrToInt(m.group(1));
                String stateStr = m.group(2);

                if (stateStr.equalsIgnoreCase("enable")) {
                    pvc.adminState = 1;
                } else if (stateStr.equalsIgnoreCase("disable")) {
                    pvc.adminState = 0;
                } else {
                    return false;
                }
            } else {
                return false;
            }

            str = getNext();
            if (str == null) {
                return false;
            }
        }

        if (pvc.overSub == 1) {
            Pattern p = Pattern.compile("^\\s*mCR\\s*:\\s*(\\d+[kKmM])");
            Matcher m = p.matcher(str);

            if (m.lookingAt()) {
                pvc.mcr = rateStrToInt(m.group(1));
            } else {
                return false;
            }
        }

        str = search(str, "^\\s*Port\\s*number\\s*:");
        if (str == null) {
            return false;
        }

        {
            Pattern p = Pattern
                    .compile("^\\s*Port\\s*number\\s*:\\s*([\\d-]+)\\s*Port\\s*name\\s*:([\\w-]+)");
            Matcher m = p.matcher(str);

            if (m.lookingAt()) {
                String associatePortStr = m.group(1);
                if (associatePortStr.equals("---")) {
                    pvc.associatePort = 0;
                } else {
                    pvc.associatePort = Integer.parseInt(m.group(1));
                    pvc.associatePortName = m.group(2);
                }
            }
        }

        if (pvc.associatePort == 0) {
            return true;
        }

        str = search("^\\s*Operational-state\\s*:");
        if (str == null) {
            return false;
        }

        {
            Pattern p = Pattern
                    .compile("^\\s*Operational-state\\s*:\\s*(\\w+)\\s*Desired-state\\s*:\\s*(\\w+)");
            Matcher m = p.matcher(str);

            if (m.lookingAt()) {
                String operStateStr = m.group(1);
                String adminStateStr = m.group(2);

                if (operStateStr.equalsIgnoreCase("up")) {
                    pvc.bridgeOperState = 1;
                } else if (operStateStr.equalsIgnoreCase("down")) {
                    pvc.bridgeOperState = 0;
                } else if (operStateStr.equalsIgnoreCase("linkdown")) {
                    pvc.bridgeOperState = 0;
                } else {
                    return false;
                }

                if (adminStateStr.equalsIgnoreCase("up")) {
                    pvc.bridgeAdminState = 1;
                } else if (adminStateStr.equalsIgnoreCase("down")) {
                    pvc.bridgeAdminState = 0;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        return true;
    }
}