package voss.discovery.agent.cisconexus;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.ConfigElement;
import voss.discovery.agent.common.ConfigElementPathBuilder;
import voss.discovery.agent.common.ConfigurationStructure;
import voss.discovery.agent.dsl.VlanModelBuilder;
import voss.discovery.agent.mib.InterfaceMib;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CiscoNxOsConfigAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(CiscoNxOsConfigAnalyzer.class);


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
                    String ifname = CiscoNxOsCommandParseUtil.getInterfaceName(interfaceElement.getId());
                    ifname = CiscoNxOsCommandParseUtil.getIfName(ifname);

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
                String ifname = CiscoNxOsCommandParseUtil.getInterfaceName(loopback.getId());
                ifname = CiscoNxOsCommandParseUtil.getIfName(ifname);
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
                        String ip = CiscoNxOsCommandParseUtil.getIpAddress(line);
                        int maskLen = CiscoNxOsCommandParseUtil.getIpAddressMaskLength(line);
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

    public static void buildVlan(MplsVlanDevice device, DeviceAccess access, ConfigurationStructure config)
            throws IOException, AbortedException {
        VlanModelBuilder builder = new VlanModelBuilder(device);
        String path = new ConfigElementPathBuilder().append("interface .*").append(CiscoNxOsCommandParseUtil.vlanIDRegex).toString();
        List<ConfigElement> interfacesWithVlan = config.getsByAttribute(path);

        try {
            Map<String, Integer> ifnameToIfindex = new HashMap<String, Integer>();
            List<StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(access.getSnmpAccess(), InterfaceMib.ifName);
            for (StringSnmpEntry entry : entries) {
                String ifname = entry.getValue();
                Integer ifIndex = Integer.valueOf(entry.getLastOIDIndex().intValue());
                if (ifnameToIfindex.keySet().contains(ifname)) {
                    continue;
                }
                ifnameToIfindex.put(ifname, ifIndex);
            }

            for (ConfigElement interfaceWithVlan : interfacesWithVlan) {
                String ifname = interfaceWithVlan.getId();
                log.debug("found vlan sub-interface: " + ifname);
                String boundIfName = CiscoNxOsCommandParseUtil.getParentInterfaceName(ifname);
                String ifName = CiscoNxOsCommandParseUtil.getIfName(boundIfName);
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
                Integer subInterfaceID = Integer.decode(CiscoNxOsCommandParseUtil.getSubInterfaceId(ifname));
                String encapsulationLine = interfaceWithVlan.getAttribute(
                        CiscoNxOsCommandParseUtil.vlanIDRegex);
                String vlanID_ = CiscoNxOsCommandParseUtil.getVlanID(encapsulationLine);
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

    public static void buildMplsLspHops(MplsVlanDevice device, DeviceAccess access,
                                        ConfigurationStructure config) throws IOException, AbortedException {
        String path = new ConfigElementPathBuilder().append("ip explicit-path .*").toString();
        List<ConfigElement> ipExplicitPathes = config.getsByPath(path);
        for (ConfigElement ipExplicitPath : ipExplicitPathes) {
            Map<String, String> options =
                    CiscoNxOsCommandParseUtil.getIpExplicitPathOptions(ipExplicitPath.getId());
            LabelSwitchedPathEndPoint lsp = new LabelSwitchedPathEndPoint();
            lsp.initDevice(device);
            if (options.get(CiscoNxOsCommandParseUtil.IP_EXPLICIT_PATH_NAME) != null) {
                lsp.setLspName(options.get(CiscoNxOsCommandParseUtil.IP_EXPLICIT_PATH_NAME));
            }
            if (options.get(CiscoNxOsCommandParseUtil.IP_EXPLICIT_PATH_STATUS) != null) {
                if (options.get(CiscoNxOsCommandParseUtil.IP_EXPLICIT_PATH_STATUS).
                        equals(CiscoNxOsCommandParseUtil.IP_EXPLICIT_PATH_ENABLE)) {
                    lsp.setLspStatus(true);
                } else {
                    lsp.setLspStatus(false);
                }
            }
            List<String> hops = ipExplicitPath.getAttributes(CiscoNxOsCommandParseUtil.NEXT_ADDRESS + ".*");
            for (String hop : hops) {
                String nextip = CiscoNxOsCommandParseUtil.getNextAddress(hop);
                lsp.addLspHop(nextip);
            }
        }

    }
}