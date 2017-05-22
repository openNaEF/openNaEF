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

public class ShowAtmPvc extends EXAtraxInterface {

    LinkedList<Vp> vpList;

    LinkedList<Pvc> pvcList;

    public ShowAtmPvc(ConsoleAccess telnet, int slot, int port)
            throws IOException, ConsoleException, AbortedException {
        super(telnet);
        vpList = new LinkedList<Vp>();
        pvcList = new LinkedList<Pvc>();

        show_atm_pvc(slot, port);
    }

    ShowAtmPvc(int slot, int port) throws Exception {
        this(null, slot, port);
    }

    public LinkedList<Vp> getVpList() {
        return vpList;
    }

    public LinkedList<Pvc> getPvcList() {
        return pvcList;
    }

    void test() {
        System.out.println("VP List");
        for (int i = 0; i < vpList.size(); i++) {
            Vp vp = vpList.get(i);
            System.out.println(" VP " + vp.slot + "/" + vp.port + "/" + vp.vpi
                    + " : PCR " + vp.pcr);

        }

        System.out.println("PVC List");
        for (int i = 0; i < pvcList.size(); i++) {
            Pvc pvc = pvcList.get(i);
            System.out.println(" PVC " + pvc.slot + "/" + pvc.port + "/"
                    + pvc.vpi + "/" + pvc.vci + " : PCR " + pvc.pcr
                    + ", Admin State " + pvc.adminState + ", Associate Port "
                    + pvc.associatePort);
        }
    }

    boolean show_atm_pvc(int slot, int phyPort) throws IOException,
            ConsoleException, AbortedException {
        ConsoleCommand command = new ConsoleCommand(new GlobalMode(),
                "show atm " + slot + "/" + phyPort + " pvc");
        execute(command);

        Pattern pVpi = Pattern
                .compile("^.*VPI\\s*:\\s*(\\d+)\\s*PCR\\s*:\\s*(\\S+)");

        String str;

        while (true) {
            str = search("^.*VPI\\s*:");
            if (str == null)
                return false;

            Vp vp;
            Matcher m = pVpi.matcher(str);
            if (m.lookingAt()) {
                int vpi;
                vpi = Integer.parseInt(m.group(1));
                vpList.add(vp = new Vp(slot, phyPort, vpi));

                vp.pcr = rateStrToInt(m.group(2));
            } else {
                return false;
            }

            str = getNext();
            if (str.length() == 0) {
                continue;
            }

            str = skipto(str, "^\\s*VCI\\s*PCR\\s*STATUS\\s*PORT");
            if (str == null) {
                return false;
            }

            Pattern pVci = Pattern
                    .compile("^\\s*(\\d+)\\s*([\\d]+[mMkKgG])\\s*(\\w+)\\s*([\\d-]+)");
            Pattern pDelim = Pattern.compile("^\\s*(\\S*)\\s*$");

            while (true) {
                Matcher mVci = pVci.matcher(str);
                if (mVci.lookingAt()) {
                    int vci;
                    vci = Integer.parseInt(mVci.group(1));

                    Pvc pvc = new Pvc(slot, phyPort, vp.vpi, vci);
                    pvcList.add(pvc);

                    pvc.pcr = rateStrToInt(mVci.group(2));

                    String stateStr = mVci.group(3);
                    if (stateStr.equalsIgnoreCase("enable")) {
                        pvc.adminState = 1;
                    } else if (stateStr.equalsIgnoreCase("disable")) {
                        pvc.adminState = 0;
                    }

                    String portStr = mVci.group(4);
                    if (portStr.equals("---")) {
                        pvc.associatePort = 0;
                    } else {
                        pvc.associatePort = Integer.parseInt(portStr);
                    }

                    str = getNext();
                    if (str == null) {
                        return true;
                    }

                    Matcher mDelim = pDelim.matcher(str);
                    if (mDelim.lookingAt()) {
                        if (mDelim.group(1).length() > 0) {
                            str = getNext();
                            if (str == null) {
                                return true;
                            }
                            continue;
                        } else {
                            break;
                        }
                    }
                } else {
                    return false;
                }
            }
        }
    }
}