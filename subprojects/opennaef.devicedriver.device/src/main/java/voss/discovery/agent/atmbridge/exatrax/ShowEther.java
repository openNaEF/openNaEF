package voss.discovery.agent.atmbridge.exatrax;

import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.simpletelnet.GlobalMode;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShowEther extends EXAtraxInterface {
    EtherPort etherPort = null;

    public ShowEther(ConsoleAccess telnet, int port) throws IOException, ConsoleException, AbortedException {
        super(telnet);
        show_ether(port);
    }

    ShowEther(int port) throws IOException, ConsoleException, AbortedException {
        this(null, port);
    }

    void test() {
        System.out.println(this);
    }

    public String toString() {
        String str = "EtherPort " + etherPort.port + "\n";
        str += " GBIC : " + etherPort.gbic + "\n";
        str += " AutoNego : " + etherPort.autoNego + "\n";
        str += " AdminSpeed : " + etherPort.adminSpeed + "\n";
        str += " AdminDuplex : " + etherPort.adminDuplex + "\n";
        str += " OperState : " + etherPort.operState + "\n";
        str += " OperSpeed : " + etherPort.operSpeed + "\n";
        str += " OperDuplex : " + etherPort.operDuplex + "\n";
        str += " PortType : " + etherPort.portType + "\n";
        str += " Presence : " + etherPort.presence;

        return str;
    }

    public EtherPort getEtherPort() {
        return etherPort;
    }

    public int isGbic() {
        if (etherPort != null)
            return etherPort.gbic;
        return -1;
    }

    public int getOperDuplex() {
        if (etherPort != null)
            return etherPort.operDuplex;
        return -1;
    }

    public String getPortType() {
        if (etherPort != null)
            return etherPort.portType;
        return null;
    }

    public int getAdminSpeed() {
        if (etherPort != null)
            return etherPort.adminSpeed;
        return -1;
    }

    public int getOperState() {
        if (etherPort != null)
            return etherPort.operState;
        return -1;
    }

    public int getOperSpeed() {
        if (etherPort != null)
            return etherPort.operSpeed;
        return -1;
    }

    public int isPresence() {
        if (etherPort != null)
            return etherPort.presence;
        return -1;
    }

    public int getAutoNego() {
        if (etherPort != null)
            return etherPort.autoNego;
        return -1;
    }

    boolean show_ether(int port) throws IOException, ConsoleException, AbortedException {
        ConsoleCommand cmd = new ConsoleCommand(new GlobalMode(), "show ether " + port);
        execute(cmd);

        String str = getNext();
        if (str == null)
            return false;

        {
            Pattern p = Pattern
                    .compile("^\\s*([\\w&&[^E]]*)\\s*Ether\\s*Port\\s*(\\d+)");
            Matcher m = p.matcher(str);

            if (m.lookingAt()) {
                etherPort = new EtherPort(port);
                if (m.group(1).equalsIgnoreCase("gigabit")) {
                    etherPort.gbic = 1;
                } else {
                    etherPort.presence = 1;
                    etherPort.portType = "10/100BASE-TX";
                    etherPort.gbic = 0;
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
            Pattern p = Pattern.compile("^\\s*Auto\\s*Nego\\s*:\\s*([\\S]*)");
            Matcher m = p.matcher(str);

            if (m.lookingAt()) {
                String autoNegoStr = m.group(1);
                if (autoNegoStr.equalsIgnoreCase("enable")) {
                    etherPort.autoNego = 1;
                } else if (autoNegoStr.equalsIgnoreCase("disable")) {
                    etherPort.autoNego = 0;
                }
            } else {
                return false;
            }
        }

        str = getNext();
        if (str == null) {
            return false;
        }

        if (etherPort.autoNego == 0) {
            if ((etherPort.adminSpeed = show_ether_speed(str)) < 0) {
                return false;
            }

            str = getNext();
            if (str == null) {
                return false;
            }

            if ((etherPort.adminDuplex = show_ether_duplex(str)) < 0) {
                return false;
            }

            str = getNext();
            if (str == null) {
                return false;
            }
        }

        str = skipto(str, "^\\s*Current\\s*Status");
        if (str == null) {
            return false;
        }

        {
            Pattern p = Pattern.compile("^\\s*Link\\s*(\\S*)");
            Matcher m = p.matcher(str);

            if (m.lookingAt()) {
                String state = m.group(1);
                if (state.equalsIgnoreCase("up")) {
                    etherPort.operState = 1;
                } else if (state.equalsIgnoreCase("down")) {
                    etherPort.operState = 0;
                }
            } else {
                return false;
            }
        }

        str = getNext();
        if (str == null) {
            return false;
        }

        if (etherPort.operState == 1) {
            if ((etherPort.operSpeed = show_ether_speed(str)) < 0) {
                return false;
            }

            str = getNext();
            if (str == null) {
                return false;
            }

            if ((etherPort.operDuplex = show_ether_duplex(str)) < 0) {
                return false;
            }
        }

        if (etherPort.gbic == 1) {
            str = skipto(str, "\\s*GBIC\\s*Status");
            if (str == null) {
                return false;
            }

            {
                Pattern p = Pattern.compile("^\\s*State\\s*:\\s*(\\S*)");
                String[] gbicStateStr = {"active", "no present", "error"};
                Matcher m = p.matcher(str);

                if (m.lookingAt()) {
                    String state = m.group(1);
                    int i;
                    for (i = 0; i < gbicStateStr.length; i++) {
                        if (state.equalsIgnoreCase(gbicStateStr[i])) {
                            break;
                        }
                    }

                    switch (i) {
                        case 0:
                            etherPort.presence = 1;
                            break;

                        default:
                            etherPort.presence = 0;
                    }
                }
            }

            str = getNext();
            if (str == null) {
                return false;
            }

            {
                Pattern p = Pattern.compile("^\\s*Tx\\s*Mode\\s*:\\s*(\\S*)");
                Matcher m = p.matcher(str);

                if (m.lookingAt()) {
                    etherPort.portType = m.group(1);
                }
            }
        }

        return true;
    }

    int show_ether_speed(String str) {
        int speed = -1;
        Pattern p = Pattern.compile("^\\s*Speed\\s*([\\d]*)\\s*Mbps");
        Matcher m = p.matcher(str);

        if (m.lookingAt()) {
            speed = Integer.parseInt(m.group(1)) * 1000;
        }

        return speed;
    }

    int show_ether_duplex(String str) {
        int duplex = -1;

        Pattern p = Pattern.compile("^\\s*Duplex\\s*(\\S*)");
        Matcher m = p.matcher(str);

        if (m.lookingAt()) {
            String duplexStr = m.group(1).toLowerCase();
            if (duplexStr.equalsIgnoreCase("full")) {
                duplex = 1;
            } else if (duplexStr.equalsIgnoreCase("half")) {
                duplex = 0;
            }
        }

        return duplex;
    }
}