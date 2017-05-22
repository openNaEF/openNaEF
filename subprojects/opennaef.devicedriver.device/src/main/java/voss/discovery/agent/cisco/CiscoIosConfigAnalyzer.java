package voss.discovery.agent.cisco;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.ConfigElement;
import voss.discovery.agent.common.ConfigElementPathBuilder;
import voss.discovery.agent.common.ConfigurationStructure;
import voss.discovery.agent.common.IanaIfType;
import voss.discovery.agent.dsl.VlanModelBuilder;
import voss.discovery.agent.mib.InterfaceMib;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.*;
import voss.util.ModelUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CiscoIosConfigAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(CiscoIosConfigAnalyzer.class);


    public static void buildVrf(MplsVlanDevice device, DeviceAccess deviceAccess, ConfigurationStructure config) {
        assert config != null;

        String path = new ConfigElementPathBuilder().append("ip vrf .+").toString();
        List<ConfigElement> vrfElements = config.getsByPath(path);
        for (ConfigElement vrfElement : vrfElements) {
            String vrfName = vrfElement.getId().replaceAll("ip vrf ", "");
            VrfInstance vrf = new VrfInstanceImpl();
            vrf.initDevice(device);
            vrf.initIfName(vrfName);
            vrf.initVrfID(vrfName);

            log.debug("@add vrf " + vrfName);
        }
    }

    public static void buildVrfAttachment(MplsVlanDevice device, DeviceAccess access,
                                          ConfigurationStructure config) throws IOException, AbortedException {
        assert config != null;

        InterfaceMibImpl ifmib = new InterfaceMibImpl(access.getSnmpAccess());

        try {
            Map<String, Integer> ifnameToIfindex = new HashMap<String, Integer>();
            List<StringSnmpEntry> entries;
            entries = SnmpUtil.getStringSnmpEntries(access.getSnmpAccess(), InterfaceMib.ifName);
            for (StringSnmpEntry entry : entries) {
                ifnameToIfindex.put(entry.getValue(), entry.getLastOIDIndex().intValue());
            }

            String path = new ConfigElementPathBuilder().append("interface .+").toString();
            List<ConfigElement> interfaceElements = config.getsByPath(path);
            for (ConfigElement interfaceElement : interfaceElements) {

                String vrfName = interfaceElement.getAttributeValue("ip vrf forwarding (.+)");

                if (vrfName != null) {
                    String ifname = CiscoIosCommandParseUtil.getInterfaceName(interfaceElement.getId());
                    ifname = CiscoIosCommandParseUtil.getShortIfName(ifname);

                    Port port = device.getPortByIfName(ifname);
                    if (port == null) {
                        port = new GenericLogicalPort();
                        port.initDevice(device);
                        port.initIfIndex(ifnameToIfindex.get(ifname));

                        boolean set = false;
                        try {
                            ifmib.setIfName(port);
                            set = true;
                        } catch (IOException e) {
                            log.warn("ifXTable is not supported.", e);
                        }
                        ifmib.setIfDescription(port);
                        if (!set) {
                            port.initIfName(port.getIfDescr());
                        }
                        ifmib.setIfType(port);
                        ifmib.setIfOperStatus(port);
                        ifmib.setIfAdminStatus(port);
                        ifmib.setIfAdminStatus(port);
                        log.debug("@ complemented port " + port.getIfIndex() + " ifName:" + port.getIfName());
                    }

                    VrfInstance vrf = device.getVrfByVrfKey(vrfName);
                    vrf.addAttachmentPort(port);

                    log.debug("@add " + ifname + " to vrf " + vrfName);
                }
            }
        } catch (UnknownHostException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public static void buildLoopbackInterface(MplsVlanDevice device, DeviceAccess access, ConfigurationStructure config)
            throws IOException, AbortedException {
        assert config != null;
        try {
            Map<String, Integer> ifnameToIfindex = new HashMap<String, Integer>();
            List<StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(access.getSnmpAccess(), InterfaceMib.ifName);
            for (StringSnmpEntry entry : entries) {
                ifnameToIfindex.put(entry.getValue(), entry.getLastOIDIndex().intValue());
            }

            String path = new ConfigElementPathBuilder().append("interface Loopback[0-9]+").toString();
            List<ConfigElement> loopbacks = config.getsByPath(path);
            for (ConfigElement loopback : loopbacks) {
                String ifname = CiscoIosCommandParseUtil.getInterfaceName(loopback.getId());
                ifname = CiscoIosCommandParseUtil.getShortIfName(ifname);
                LoopbackInterface loopbackInterface = new LoopbackInterface();
                loopbackInterface.initDevice(device);
                loopbackInterface.initIfName(ifname);
                int ifindex = ifnameToIfindex.get(ifname);
                loopbackInterface.initIfIndex(ifindex);

                String vrfName = loopback.getAttributeValue("ip vrf forwarding (.*)");
                VrfInstance vrf = null;
                if (vrfName != null) {
                    vrf = device.getVrfByVrfKey(vrfName);
                    if (vrf == null) {
                        throw new IllegalStateException("unknown vrf " + vrfName);
                    }
                }

                List<String> loopbackIp = loopback.getAttributes("ip address .*");
                if (loopbackIp.size() > 0) {
                    for (String line : loopbackIp) {
                        String ip = CiscoIosCommandParseUtil.getIpAddress(line);
                        int maskLen = CiscoIosCommandParseUtil.getIpAddressMaskLength(line);
                        try {
                            InetAddress addr = InetAddress.getByName(ip);
                            CidrAddress addressMask = new CidrAddress(addr, maskLen);
                            if (vrf != null) {
                                vrf.addAttachmentPort(loopbackInterface);
                                vrf.addVpnIpAddress(loopbackInterface, addressMask);
                                log.debug("@add ip " + addressMask.toString() + " to vrf " + loopbackInterface.getIfName());
                            } else {
                                device.addIpAddressToPort(addressMask, loopbackInterface);
                                log.debug("@add ip " + addressMask.toString() + " to " + loopbackInterface.getIfName());
                            }
                        } catch (UnknownHostException e) {
                            log.error(e.toString(), e);
                        }
                    }
                }
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public static void buildSerialPortOnController(MplsVlanDevice device, DeviceAccess access, ConfigurationStructure config)
            throws AbortedException, IOException {
        assert config != null;
        try {
            Map<String, Integer> ifnameToIfindex = new HashMap<String, Integer>();
            List<StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(access.getSnmpAccess(), InterfaceMib.ifName);
            for (StringSnmpEntry entry : entries) {
                String ifname = CiscoIosCommandParseUtil.getFullyQualifiedInterfaceName(entry.getValue());
                ifnameToIfindex.put(ifname, entry.getLastOIDIndex().intValue());
            }

            String path = new ConfigElementPathBuilder().append("controller T[0-9]+ .*").toString();
            List<ConfigElement> controllers = config.getsByPath(path);
            for (ConfigElement controller : controllers) {
                String controllerName = controller.getId();
                log.trace("found controller: " + controllerName);
                String portID = controllerName.replaceAll("controller T[0-9]+", "").trim();
                Container container = CiscoIosCommandParseUtil.getParentContainerByIfName(device, portID);
                if (container == null) {
                    log.warn("[IOS BUG?] no module, but port found. portID=" + portID);
                    continue;
                }

                String subPath = new ConfigElementPathBuilder().append("interface Serial" + portID + ".*").toString();
                List<ConfigElement> serialPorts = config.getsByPath(subPath);
                for (ConfigElement serialPort : serialPorts) {
                    log.trace("found serialPort on controller: " + serialPort.getId() + ", " + controllerName);
                    String ifName = CiscoIosCommandParseUtil.getInterfaceName(serialPort.getId());
                    SerialPort serial = new SerialPortImpl();
                    serial.initDevice(device);
                    serial.initIfName(ifName);
                    Integer ifIndex = getIfIndex(ifnameToIfindex, ifName);
                    if (ifIndex != null) {
                        serial.initIfIndex(ifIndex.intValue());
                    } else {
                        log.warn("no ifindex found for port[" + ifName + "]");
                    }
                    container.addPort(serial);
                    log.debug("@add port ifname='" + ifName + "' on device '" + device.getDeviceName() + "';");
                    log.debug("@set port ifname='" + ifName + "' ifIndex '" + ifIndex + "' on device '" + device.getDeviceName() + "';");
                }
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    private static Integer getIfIndex(Map<String, Integer> ifNameToIfIndex, String ifName) {
        Integer ifIndex = ifNameToIfIndex.get(ifName);
        if (ifIndex == null && ifName.indexOf(':') != -1) {
            String _ifName = ifName.substring(0, ifName.indexOf(':'));
            ifIndex = ifNameToIfIndex.get(_ifName);
        }
        if (ifIndex == null) {
            String _ifName = CiscoIosCommandParseUtil.getShortIfName(ifName);
            ifIndex = ifNameToIfIndex.get(_ifName);
        }
        return ifIndex;
    }

    public static void buildVlan(MplsVlanDevice device, DeviceAccess access, ConfigurationStructure config)
            throws IOException, AbortedException {
        VlanModelBuilder builder = new VlanModelBuilder(device);
        String path = new ConfigElementPathBuilder().append("interface .*").append(CiscoIosCommandParseUtil.vlanIDRegex).toString();
        List<ConfigElement> interfacesWithVlan = config.getsByAttribute(path);

        try {
            Map<String, Integer> ifnameToIfindex = new HashMap<String, Integer>();
            List<StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(access.getSnmpAccess(), InterfaceMib.ifName);
            for (StringSnmpEntry entry : entries) {
                String ifname = CiscoIosCommandParseUtil.getFullyQualifiedInterfaceName(entry.getValue());
                String ifname2 = CiscoIosCommandParseUtil.getShortIfName(ifname);
                Integer ifIndex = Integer.valueOf(entry.getLastOIDIndex().intValue());
                if (ifnameToIfindex.keySet().contains(ifname)) {
                    continue;
                }
                ifnameToIfindex.put(ifname, ifIndex);
                ifnameToIfindex.put(ifname2, ifIndex);
            }

            for (ConfigElement interfaceWithVlan : interfacesWithVlan) {
                String ifname = interfaceWithVlan.getId();
                log.debug("found vlan sub-interface: " + ifname);
                String boundIfName = CiscoIosCommandParseUtil.getParentInterfaceName(ifname);
                String ifName = CiscoIosCommandParseUtil.getShortIfName(boundIfName);
                Port boundPort = device.getPortByIfName(ifName);
                if (boundPort == null) {
                    log.error("boundPort is null. ifName=" + ifName);
                    continue;
                }
                if (!(boundPort instanceof EthernetPort)) {
                    throw new IllegalStateException("port "
                            + boundPort.getFullyQualifiedName()
                            + " is not EthernetPort: "
                            + boundPort.getClass().getSimpleName());
                }
                LogicalEthernetPort logical = device.getLogicalEthernetPort((EthernetPort) boundPort);
                log.debug("bound interface: " + logical.getFullyQualifiedName());
                Integer subInterfaceID = Integer.decode(CiscoIosCommandParseUtil.getSubInterfaceId(ifname));
                String encapsulationLine = interfaceWithVlan.getAttribute(
                        CiscoIosCommandParseUtil.vlanIDRegex);
                String vlanID_ = CiscoIosCommandParseUtil.getVlanID(encapsulationLine);
                if (vlanID_ == null) {
                    throw new IllegalStateException("config with unexpected syntax: " + encapsulationLine);
                }
                Integer vlanID = Integer.decode(vlanID_);
                String vlanIfName = ifName + "." + subInterfaceID;
                Integer vlanIfIndex = ifnameToIfindex.get(vlanIfName);
                if (vlanIfIndex == null) {
                    vlanIfIndex = ifnameToIfindex.get(boundIfName + "." + subInterfaceID);
                }

                VlanIf vlanIf = builder.buildRouterVlanIf(logical, null, vlanID);
                builder.setVlanName(vlanIf, vlanIfName);
                if (vlanIfIndex != null) {
                    builder.setVlanIfIndex(vlanIf, vlanIfIndex.intValue());
                }
                builder.addTaggedVlan(vlanIf, logical);
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    private static final Pattern atmPortPattern = Pattern.compile("interface (ATM[0-9/.:]+) ?.*");
    private static final Pattern atmPvcPattern = Pattern.compile("pvc (([0-9]+)/([0-9]+)) ?.*");

    public static void buildAtmPvcInterface(MplsVlanDevice device, DeviceAccess access,
                                            ConfigurationStructure config) throws IOException, AbortedException {
        Map<String, Integer> longIfNameToIfIndex = new HashMap<String, Integer>();
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(access.getSnmpAccess(), InterfaceMib.ifType);
            List<StringSnmpEntry> ifNameEntries = SnmpUtil.getStringSnmpEntries(access.getSnmpAccess(), InterfaceMib.ifName);
            for (int i = 0; i < entries.size(); i++) {
                int ifType = entries.get(i).intValue();
                if (ifType != IanaIfType.aal5.getId()) {
                    continue;
                }
                Integer ifIndex = Integer.valueOf(entries.get(i).getLastOIDIndex().intValue());
                String ifName = ifNameEntries.get(i).getValue();
                String longIfname = CiscoIosCommandParseUtil.getFullyQualifiedInterfaceName(ifName);
                String shortIfName = CiscoIosCommandParseUtil.getShortIfName(longIfname);
                longIfNameToIfIndex.put(longIfname, ifIndex);
                longIfNameToIfIndex.put(shortIfName, ifIndex);
                log.debug("ifName-to-ifIndex=" + longIfname + "->" + ifIndex);
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }

        String path = new ConfigElementPathBuilder().append("interface ATM.* point-to-point").toString();
        List<ConfigElement> interfaceATMs = config.getsByPath(path);
        for (ConfigElement interfaceATM : interfaceATMs) {
            log.debug("processing: " + interfaceATM.getId());
            Matcher matcher1 = atmPortPattern.matcher(interfaceATM.getId());
            String longIfName = null;
            String shortIfName = null;
            AtmPort atm = null;
            if (matcher1.matches()) {
                longIfName = CiscoIosCommandParseUtil.getInterfaceName(interfaceATM.getId());
                shortIfName = CiscoIosCommandParseUtil.getShortIfName(longIfName);
                String parentIfName = CiscoIosCommandParseUtil.getParentInterfaceName(shortIfName);
                Port port = device.getPortByIfName(parentIfName);
                if (port == null) {
                    log.error("parent ATM port not found: " + parentIfName);
                    continue;
                }
                atm = ModelUtils.getAtmPort(port);
                if (atm == null) {
                    log.error("not ATM port: " + parentIfName);
                    continue;
                }
            } else {
                log.error("illegal interface ATM line: " + interfaceATM.getId());
                continue;
            }

            String pvcLine = interfaceATM.getAttribute("pvc .*");
            if (pvcLine == null) {
                log.debug("no pvc config found on [" + interfaceATM.getId() + "]");
                continue;
            }
            Matcher matcher = atmPvcPattern.matcher(pvcLine);
            if (matcher.matches()) {
                int vpi = Integer.parseInt(matcher.group(2));
                int vci = Integer.parseInt(matcher.group(3));
                AtmVp vp = atm.getVp(vpi);
                if (vp == null) {
                    log.warn("vp not found: " + interfaceATM.getId() + "#" + pvcLine);
                    vp = new AtmVp(atm, vpi);
                    vp.initIfName(atm.getIfName() + "/" + vpi);
                }
                AtmPvc pvc = vp.getPvc(vci);
                if (pvc == null) {
                    pvc = new AtmPvc(vp, vci);
                }
                pvc.initIfName(shortIfName);

                log.debug("pvc ifName=" + longIfName + ", " + shortIfName);
                Integer ifIndex = longIfNameToIfIndex.get(longIfName);
                if (ifIndex != null) {
                    pvc.initIfIndex(ifIndex.intValue());
                    log.debug("\tifIndex=" + ifIndex.intValue());
                } else {
                    log.error("no ifIndex found: ifName(long)=" + longIfName);
                }
            } else {
                log.error("illegal interface ATM/pvc line: " + interfaceATM.getId() + "#" + pvcLine);
            }
        }
    }

    public static void buildMplsPseudoWire(MplsVlanDevice device, DeviceAccess access,
                                           ConfigurationStructure config) throws IOException, AbortedException {
    }

    public static void buildMplsLspHops(MplsVlanDevice device, DeviceAccess access,
                                        ConfigurationStructure config) throws IOException, AbortedException {
        String path = new ConfigElementPathBuilder().append("ip explicit-path .*").toString();
        List<ConfigElement> ipExplicitPathes = config.getsByPath(path);
        for (ConfigElement ipExplicitPath : ipExplicitPathes) {
            Map<String, String> options =
                    CiscoIosCommandParseUtil.getIpExplicitPathOptions(ipExplicitPath.getId());
            LabelSwitchedPathEndPoint lsp = new LabelSwitchedPathEndPoint();
            lsp.initDevice(device);
            if (options.get(CiscoIosCommandParseUtil.IP_EXPLICIT_PATH_NAME) != null) {
                lsp.setLspName(options.get(CiscoIosCommandParseUtil.IP_EXPLICIT_PATH_NAME));
            }
            if (options.get(CiscoIosCommandParseUtil.IP_EXPLICIT_PATH_STATUS) != null) {
                if (options.get(CiscoIosCommandParseUtil.IP_EXPLICIT_PATH_STATUS).
                        equals(CiscoIosCommandParseUtil.IP_EXPLICIT_PATH_ENABLE)) {
                    lsp.setLspStatus(true);
                } else {
                    lsp.setLspStatus(false);
                }
            }
            List<String> hops = ipExplicitPath.getAttributes(CiscoIosCommandParseUtil.NEXT_ADDRESS + ".*");
            for (String hop : hops) {
                String nextip = CiscoIosCommandParseUtil.getNextAddress(hop);
                lsp.addLspHop(nextip);
            }
        }

    }
}