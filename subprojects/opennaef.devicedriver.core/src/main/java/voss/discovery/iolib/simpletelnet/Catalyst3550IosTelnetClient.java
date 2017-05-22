package voss.discovery.iolib.simpletelnet;

import voss.discovery.iolib.console.ConsoleException;
import voss.model.NodeInfo;

import java.io.IOException;

public class Catalyst3550IosTelnetClient extends IosTelnetClient {

    public Catalyst3550IosTelnetClient(TerminalSocket socket, NodeInfo nodeinfo) {
        super(socket, nodeinfo);
    }

    public String changeMode(String changingMode) throws IOException, ConsoleException {
        if (MODE_GLOBAL_CONFIG.equals(currentMode) && changingMode.startsWith(MODE_CONFIG_VLAN)) {
            sendln("vlan " + changingMode.substring(MODE_CONFIG_VLAN.length()));
            currentMode = changingMode;
            return translate(receiveToPrompt());
        } else if (currentMode.startsWith(MODE_CONFIG_VLAN) && changingMode.startsWith(MODE_GLOBAL_CONFIG)) {
            sendln("exit");
            currentMode = changingMode;
            return translate(receiveToPrompt());
        } else if (currentMode.startsWith(MODE_CONFIG_VLAN) && changingMode.startsWith(MODE_INTERFACE_CONFIG)) {
            sendln("interface " + changingMode.substring(MODE_INTERFACE_CONFIG.length()));
            currentMode = changingMode;
            return translate(receiveToPrompt());
        } else {
            return super.changeMode(changingMode);
        }
    }

    protected String getPromptSuffix() {
        if (currentMode.startsWith(MODE_CONFIG_VLAN)) {
            return "(config-vlan)#";
        } else {
            return super.getPromptSuffix();
        }
    }


}