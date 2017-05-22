package voss.nms.inventory.diff.network;

import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.vlan.VlanIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ATTR;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.util.ConfigModelUtil;
import voss.core.server.util.Util;
import voss.model.CidrAddress;
import voss.model.Port;
import voss.model.VlanIf;
import voss.nms.inventory.util.NameUtil;

import java.net.InetAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class IpAddressHolder {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final String vpnPrefix;
    private final String ipAddress;
    private Integer maskLength;
    private final CidrAddress cidr;
    private final Set<Port> networkPorts = new HashSet<Port>();
    private final Set<IpIfDto> dbPorts = new HashSet<IpIfDto>();
    private Port lastNetworkPort;
    private IpIfDto lastDbPort;
    private boolean applyChange = true;

    public IpAddressHolder(String vpnPrefix, String ip) {
        if (ip == null) {
            throw new IllegalArgumentException();
        }
        this.vpnPrefix = Util.s2n(vpnPrefix);
        this.ipAddress = ip;
        this.cidr = null;
    }

    public IpAddressHolder(String vpnPrefix, CidrAddress cidr) {
        if (cidr == null) {
            throw new IllegalArgumentException();
        }
        this.vpnPrefix = Util.s2n(vpnPrefix);
        this.ipAddress = cidr.getAddress().getHostAddress();
        this.maskLength = Integer.valueOf(cidr.getSubnetMaskLength());
        this.cidr = cidr;
    }

    public String getKey() {
        return getKey(this.vpnPrefix, this.ipAddress);
    }

    public static String getKey(String vpnPrefix, String ip) {
        StringBuilder sb = new StringBuilder();
        if (vpnPrefix != null) {
            sb.append(vpnPrefix);
            sb.append("/");
        }
        sb.append(ip);
        return sb.toString();
    }

    public String getVpnPrefix() {
        return this.vpnPrefix;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public Integer getMaskLength() {
        return this.maskLength;
    }

    public void setMaskLength(Integer maskLength) {
        if (maskLength == null) {
            throw new IllegalArgumentException("mask length is null.");
        }
        this.maskLength = maskLength;
    }

    public String getNetworkAddress() {
        try {
            InetAddress addr = InetAddress.getByName(ipAddress);
            CidrAddress cidr = this.cidr;
            if (cidr == null) {
                cidr = new CidrAddress(addr, this.maskLength.intValue());
            }
            StringBuilder sb = new StringBuilder();
            if (this.vpnPrefix != null) {
                sb.append(this.vpnPrefix);
                sb.append("/");
            }
            sb.append(cidr.getNetworkAddress().getHostAddress());
            sb.append("/");
            sb.append(this.maskLength);
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void addNetworkPort(Port port) {
        if (this.networkPorts.contains(port)) {
            return;
        }
        this.networkPorts.add(port);
        this.lastNetworkPort = port;
    }

    public Set<Port> getNetworkPorts() {
        return this.networkPorts;
    }

    public void addIpPort(IpIfDto port) {
        if (this.dbPorts.contains(port)) {
            return;
        }
        this.dbPorts.add(port);
        this.lastDbPort = port;
    }

    public Set<IpIfDto> getDbPorts() {
        return this.dbPorts;
    }

    public String getAbsoluteName() {
        IpIfDto db = getDbPort();
        if (db != null) {
            return db.getAbsoluteName();
        }
        Port port = getNetworkPort();
        if (port != null) {
            return ATTR.TYPE_NODE + ATTR.NAME_DELIMITER_SECONDARY + port.getDevice().getDeviceName()
                    + ATTR.NAME_DELIMITER_PRIMARY
                    + ATTR.TYPE_IP_PORT + ATTR.NAME_DELIMITER_SECONDARY + this.ipAddress;
        }
        return null;
    }

    public Port getNetworkPort() {
        if (this.networkPorts.size() > 1) {
            throw new IllegalStateException();
        } else if (this.networkPorts.size() == 0) {
            return null;
        }
        return this.lastNetworkPort;
    }

    public IpIfDto getDbPort() {
        if (this.dbPorts.size() > 1) {
            throw new IllegalStateException();
        } else if (this.dbPorts.size() == 0) {
            return null;
        }
        return this.lastDbPort;
    }

    public boolean isVlanConsistent() {
        Set<Integer> vlanIds = getVlanIDs();
        return vlanIds.size() < 2;
    }

    public Integer getVlanID() {
        Set<Integer> vlanIds = getVlanIDs();
        switch (vlanIds.size()) {
            case 0:
                return null;
            case 1:
                return vlanIds.iterator().next();
        }
        return null;
    }

    private Set<Integer> getVlanIDs() {
        Set<Integer> vlanIDs = new HashSet<Integer>();
        for (Port port : this.networkPorts) {
            if (!VlanIf.class.isInstance(port)) {
                continue;
            }
            VlanIf vif = (VlanIf) port;
            vlanIDs.add(vif.getVlanId());
        }
        for (PortDto port : this.dbPorts) {
            if (!VlanIfDto.class.isInstance(port)) {
                continue;
            }
            VlanIfDto vif = (VlanIfDto) port;
            vlanIDs.add(vif.getVlanId());
        }
        log.debug("ip-address's vlan-id: " + this.ipAddress + "=" + vlanIDs);
        return vlanIDs;
    }

    public void setApplyChange(boolean mode) {
        this.applyChange = mode;
    }

    public boolean canApplyChange() {
        return this.applyChange;
    }

    public String getInventoryID() {
        if (this.lastNetworkPort != null) {
            Port networkPort = ConfigModelUtil.getPhysicalPort(this.lastNetworkPort);
            return InventoryIdCalculator.getId(networkPort);
        } else if (this.lastDbPort != null) {
            return InventoryIdCalculator.getId(this.lastDbPort);
        }
        throw new IllegalStateException("no port.");
    }

    public String getNodeName() {
        if (this.lastNetworkPort != null) {
            return this.lastNetworkPort.getDevice().getDeviceName();
        } else if (this.lastDbPort != null) {
            return this.lastDbPort.getNode().getName();
        }
        throw new IllegalStateException("no port.");
    }

    public boolean isDuplicated() {
        if (this.networkPorts.size() > 0) {
            return this.networkPorts.size() == 1;
        } else if (this.dbPorts.size() > 0) {
            return this.dbPorts.size() == 1;
        }
        return true;
    }

    public boolean isNew() {
        if (!this.applyChange) {
            return false;
        }
        Set<IpIfDto> db = getDbPorts();
        Set<Port> network = getNetworkPorts();
        return db.size() == 0 && network.size() > 0;
    }

    public boolean isDeleted() {
        if (!this.applyChange) {
            return false;
        }
        Set<IpIfDto> db = getDbPorts();
        Set<Port> network = getNetworkPorts();
        return db.size() > 0 && network.size() == 0;
    }

    public boolean isChanged() {
        if (!this.applyChange) {
            return false;
        }
        Set<Port> network = getNetworkPorts();
        Set<IpIfDto> ip = getDbPorts();
        if (ip.size() == 0 && network.size() == 0) {
            return false;
        } else if (ip.size() == 0 || network.size() == 0) {
            return true;
        }
        Set<String> dbIfNames = new HashSet<String>();
        Set<String> nwIfNames = new HashSet<String>();
        for (IpIfDto ipif : ip) {
            Collection<PortDto> db_ = ipif.getAssociatedPorts();
            if (db_ == null) {
                continue;
            }
            for (PortDto p : db_) {
                dbIfNames.add(NameUtil.getNodeIfName(p));
            }
        }
        for (Port p : network) {
            String nwIfName = p.getFullyQualifiedName();
            nwIfNames.add(nwIfName);
        }
        return !Util.isSameSet(dbIfNames, nwIfNames);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("key=").append(getKey());
        sb.append(" NW=[");
        for (Port port : this.networkPorts) {
            port = ConfigModelUtil.getPhysicalPort(port);
            sb.append(port.getFullyQualifiedName()).append(", ");
        }
        sb.append("] DB=[");
        for (IpIfDto ip : this.dbPorts) {
            sb.append(ip.getAbsoluteName()).append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return this.ipAddress.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (this == o) {
            return true;
        } else if (!(o instanceof IpAddressHolder)) {
            return false;
        }
        return getKey().equals(((IpAddressHolder) o).getKey());
    }

    public String getDeviceName() {
        if (this.lastNetworkPort != null) {
            return this.lastNetworkPort.getDevice().getDeviceName();
        } else if (this.lastDbPort != null) {
            return this.lastDbPort.getNode().getName();
        }
        return null;
    }

    public String getFullyQualifiedName() {
        if (this.lastNetworkPort != null) {
            Port p = ConfigModelUtil.getPhysicalPort(lastNetworkPort);
            return p.getFullyQualifiedName();
        } else if (this.lastDbPort != null) {
            return NameUtil.getNodeIfName(lastDbPort);
        }
        return null;
    }
}