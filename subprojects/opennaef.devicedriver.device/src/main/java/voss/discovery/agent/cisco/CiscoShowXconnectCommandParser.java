package voss.discovery.agent.cisco;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CiscoShowXconnectCommandParser {
    private static final Logger log = LoggerFactory.getLogger(CiscoShowXconnectCommandParser.class);

    private final String result;
    private final MplsVlanDevice device;

    public CiscoShowXconnectCommandParser(String result, MplsVlanDevice device) {
        this.result = result;
        this.device = device;
    }

    private final Pattern acPattern =
            Pattern.compile("([A-Z]+) (.+) ac +([A-Za-z0-9/:.]+) +([A-Za-z]+) +mpls ([^ ]+) +([A-Z]+)");

    private static final int PW_STATUS = 1;

    @SuppressWarnings("unused")
    private static final int PW_PRIORITY = 2;

    private static final int PW_ATTACHED = 3;
    private static final int PW_ATTACHED_STATE = 4;
    private static final int PW_PEER = 5;

    @SuppressWarnings("unused")
    private static final int PW_PEER_STATE = 6;

    private final String omitRegexp = "\\([^)]*\\)";

    public void connectPwAndAtmPvc() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new StringReader(result));
            String line = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.contains("ac")) {
                    continue;
                }
                line = line.replaceAll(omitRegexp, "");
                Matcher matcher = acPattern.matcher(line);
                if (matcher.matches()) {
                    String status = translate(matcher.group(PW_STATUS));
                    String pvcName = matcher.group(PW_ATTACHED);
                    String pvcStatus = matcher.group(PW_ATTACHED_STATE);
                    String peerInfo = matcher.group(PW_PEER);
                    String[] elem = peerInfo.split(":");
                    int pwID = Integer.parseInt(elem[1]);
                    log.debug(pvcName + "->" + peerInfo);

                    PseudoWirePort pw = device.getPseudoWirePortByPwId(pwID);
                    if (pw == null) {
                        throw new IllegalArgumentException("unknown pwID: " + pwID);
                    }

                    Port attachedTargetPort = null;
                    String[] elem2 = pvcName.split(":");
                    if (elem2.length == 1) {
                        attachedTargetPort = device.getPortByIfName(pvcName);
                    } else if (elem2.length >= 2) {
                        String ifName = CiscoIosCommandParseUtil.getParentInterfaceName(elem2[0]);
                        String pvcID = elem2[1];
                        Port atm_ = device.getPortByIfName(ifName);
                        if (!(atm_ instanceof AtmPort)) {
                            throw new IllegalStateException("non ATM Port: " + ifName);
                        }
                        AtmPort atm = (AtmPort) atm_;

                        String[] vpivci = pvcID.split("/");
                        Integer vpi = null;
                        Integer vci = null;
                        if (vpivci.length == 1) {
                            vpi = Integer.valueOf(pvcID);
                            attachedTargetPort = atm.getVp(vpi);
                            assert attachedTargetPort != null : "attachedTargetPort(VP) is null";
                        } else if (vpivci.length == 2) {
                            vpi = Integer.valueOf(vpivci[0]);
                            vci = Integer.valueOf(vpivci[1]);
                            attachedTargetPort = atm.getVp(vpi).getPvc(vci);
                            assert attachedTargetPort != null : "attachedTargetPort(PVC) is null";
                        } else {
                            throw new IllegalStateException("illegal attached port name:" + pvcName);
                        }
                    } else {
                        throw new IllegalStateException("illegal attached port name:" + pvcName);
                    }

                    pw.setAttachedCircuitPort(attachedTargetPort);
                    pw.setStatus(status);
                    if (pvcStatus.equals("administrative down")) {
                        pw.setPseudoWireAdminStatus(PseudoWireOperStatus.down);
                    } else {
                        pw.setPseudoWireAdminStatus(PseudoWireOperStatus.up);
                    }
                    pw.setPseudoWireOperStatus(translatePseudoWireOperStatus(pvcStatus));
                    pw.setIfDescr(pvcName);
                    pw.setSystemDescription(pvcName);
                }
            }
        } catch (IOException e) {
            log.error("", e);
        }
    }

    private String translate(String s) {
        if (s == null) {
            return s;
        } else if (s.equals("DN")) {
            return "down";
        } else if (s.equals("UP")) {
            return "up";
        } else if (s.equals("AD")) {
            return "administrative down";
        } else if (s.equals("IA")) {
            return "inactive";
        } else if (s.equals("NH")) {
            return "no hardware";
        } else {
            return s;
        }
    }

    private PseudoWireOperStatus translatePseudoWireOperStatus(String s) {
        if (s == null) {
            return null;
        } else if (s.equals("DN")) {
            return PseudoWireOperStatus.down;
        } else if (s.equals("UP")) {
            return PseudoWireOperStatus.up;
        } else if (s.equals("AD")) {
            return PseudoWireOperStatus.down;
        } else if (s.equals("IA")) {
            return PseudoWireOperStatus.down;
        } else if (s.equals("NH")) {
            return PseudoWireOperStatus.notPresent;
        } else {
            return PseudoWireOperStatus.undefined;
        }
    }

}