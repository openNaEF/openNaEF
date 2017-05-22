package voss.discovery.agent.fortigate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.ConfigElement;
import voss.discovery.agent.common.ConfigElementPathBuilder;
import voss.discovery.agent.common.ConfigurationStructure;
import voss.model.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFortigateEntityFactory implements FortigateEntityFactory {
    private static final Logger log = LoggerFactory.getLogger(AbstractFortigateEntityFactory.class);

    @Override
    public void createVdomLink(ConfigurationStructure config, MplsVlanDevice device) throws IOException {
        List<String> vdomLinks = new ArrayList<String>();
        String path1 = new ConfigElementPathBuilder()
                .append("config global")
                .append("config system vdom-link")
                .append("edit \"rt/v[0-9]+-link\"").toString();
        List<ConfigElement> elements1 = config.getsByPath(path1);
        for (ConfigElement element : elements1) {
            String linkName = element.getId().replace("edit ", "");
            linkName = strip(linkName);
            vdomLinks.add(linkName);
            log.debug("vdom link found: " + linkName);
        }

        for (String linkName : vdomLinks) {
            log.debug("evaluating: " + linkName);
            ConfigElement port0 = getVdomLinkPortConfigElement(config, linkName + "0");
            ConfigElement port1 = getVdomLinkPortConfigElement(config, linkName + "1");
            if (port0 == null || port1 == null) {
                log.warn("- illegal vdom link port configuration found.");
                continue;
            }
            Device device0 = getVdomLinkEndDevice(port0, device);
            Device device1 = getVdomLinkEndDevice(port1, device);
            if (device0 == null || device1 == null) {
                log.warn("- illegal vdom link: end vdom not found.");
                continue;
            }
            EthernetPort eth0 = createVdomLinkPort(port0, device0);
            EthernetPort eth1 = createVdomLinkPort(port1, device1);
            Link link = new LinkImpl();
            link.initPorts(eth0, eth1);
            log.debug("- vdom link created: " + eth0.getFullyQualifiedName() + ", " + eth1.getFullyQualifiedName());
        }
    }

    private ConfigElement getVdomLinkPortConfigElement(ConfigurationStructure config, String portName) {
        if (portName == null) {
            return null;
        }
        String path = new ConfigElementPathBuilder()
                .append("config global")
                .append("config system interface")
                .append("edit \"" + portName + "\"").toString();
        ConfigElement element = config.getByPath(path);
        if (element == null) {
            log.debug("- port not found: " + portName);
            return null;
        }
        return element;
    }

    private Device getVdomLinkEndDevice(ConfigElement portConfig, MplsVlanDevice device) {
        if (portConfig == null) {
            throw new IllegalArgumentException();
        }
        String vdomName = portConfig.getAttributeValue("set vdom \"(.*)\"");
        if (vdomName == null || vdomName.isEmpty()) {
            log.debug("- no vdom attribute found: " + portConfig.getId());
            return null;
        }
        if (vdomName.equals("root")) {
            return device;
        } else {
            for (Device vdom : device.getVirtualDevices()) {
                if (vdomName.equals(vdom.getDeviceName())) {
                    return vdom;
                }
            }
            log.debug("- no vdom found: " + vdomName);
            return null;
        }
    }

    private EthernetPort createVdomLinkPort(ConfigElement portConfig, Device device) throws IOException {
        if (portConfig == null || device == null) {
            throw new IllegalArgumentException();
        }
        String ifName = strip(portConfig.getId().replace("edit ", ""));
        int portIndex = getPortIndex(ifName);
        EthernetPort eth = new EthernetPortImpl();
        eth.initDevice(device);
        eth.initIfName(ifName);
        eth.initPortIndex(portIndex);
        log.debug("- vdom link port created: " + device.getDeviceName() + ":" + ifName);
        String ip = portConfig.getAttributeValue("set ip ([0-9a-f.:]+) [0-9a-f.:]+");
        String mask = portConfig.getAttributeValue("set ip [0-9a-f.:]+ ([0-9a-f.:]+)");
        if (ip != null && mask != null) {
            InetAddress addr = InetAddress.getByName(ip);
            int maskLen = getMaskLen(mask);
            CidrAddress addressMask = new CidrAddress(addr, maskLen);
            device.addIpAddressToPort(addressMask, eth);
            log.debug("- vdom link port address: " + ip + "/" + mask);
        }
        return eth;
    }

    private String strip(String s) {
        if (s == null) {
            return null;
        }
        return s.replace("\"", "");
    }

    private int getPortIndex(String ifName) {
        String[] elements = ifName.split("-");
        int value = 0;
        for (String element : elements) {
            value = value * 1000;
            int i = toInt(element);
            if (i != 0) {
                value = value + i;
            }
        }
        value = value + 100000;
        return value;
    }

    private int toInt(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            int ch = s.codePointAt(i);
            if (ch < '0' || '9' < ch) {
                continue;
            }
            int val = ch - '0';
            sb.append(val);
        }
        if (sb.length() == 0) {
            return 0;
        }
        return Integer.parseInt(sb.toString());
    }

    private int getMaskLen(String mask) {
        String[] elements = mask.split("\\.");
        int maskLength = 0;
        for (String element : elements) {
            int length = getBitLength(element);
            if (length == -1) {
                return 32;
            }
            maskLength = maskLength + length;
            if (length != 8) {
                break;
            }
        }
        return maskLength;
    }

    private int getBitLength(String s) {
        if (s == null) {
            throw new IllegalArgumentException();
        } else if (s.equals("0")) {
            return 0;
        }
        int value = Integer.parseInt(s);
        if (value > 255) {
            throw new IllegalStateException("subnet mask value larger than 255 found: " + s);
        }
        int length = 0;
        int base = 256;
        for (int i = 0; i < 9; i++) {
            length++;
            base = base / 2;
            value = value - base;
            if (value == 0) {
                return length;
            }
        }
        return -1;
    }
}