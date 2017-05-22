package voss.discovery.agent.atmbridge.exatrax;

import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.simpletelnet.GlobalMode;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShowAtmVp extends EXAtraxInterface {

    LinkedList<Vp> vpList = null;

    public ShowAtmVp(ConsoleAccess telnet, int slot, int port) throws Exception {
        super(telnet);
        vpList = new LinkedList<Vp>();

        show_atm_vp(slot, port);
    }

    ShowAtmVp(int slot, int port) throws Exception {
        this(null, slot, port);
    }

    void test() {
        System.out.println("VP List");
        for (int i = 0; i < vpList.size(); i++) {
            Vp vp = vpList.get(i);
            System.out.println(" VP " + vp.slot + "/" + vp.port + "/" + vp.vpi
                    + " : PCR " + vp.pcr + ", OverSubscription " + vp.overSub);
        }
    }

    boolean show_atm_vp(int slot, int port) throws Exception {
        ConsoleCommand command = new ConsoleCommand(new GlobalMode(),
                "show atm " + slot + "/" + port + " vp");
        execute(command);

        String str = skipto("^\\s*VPI\\s*PCR");
        if (str == null) {
            return false;
        }

        Pattern p = Pattern
                .compile("^\\s*(\\d+)\\s*(\\S+)\\s*\\S+\\s*\\d+\\s*(\\S*)");

        while (true) {
            Matcher m = p.matcher(str);
            Vp vp;

            if (m.lookingAt()) {
                int vpi = Integer.parseInt(m.group(1));
                ;
                int pcr = rateStrToInt(m.group(2));

                vpList.add(vp = new Vp(slot, port, vpi));

                vp.pcr = pcr;

                if (m.group(3).equalsIgnoreCase("OverSubscription")) {
                    vp.overSub = 1;
                } else {
                    vp.overSub = 0;
                }
            } else {
                return false;
            }

            str = getNext();
            if (str == null) {
                break;
            }
        }

        return true;
    }
}