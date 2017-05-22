package voss.nms.inventory.diff.network;

import naef.mvo.ip.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.util.ConfigModelUtil;
import voss.model.AbstractVlanModel;
import voss.model.Port;

import java.util.HashSet;
import java.util.Set;

public class IpSubnet extends AbstractVlanModel {
    private static final long serialVersionUID = 1L;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final String vpnPrefix;
    private final String startAddress;
    private final Integer maskLength;
    private final Set<IpAddressHolder> members = new HashSet<IpAddressHolder>();
    private boolean applyChange = true;

    public IpSubnet(String networkAddr) {
        this(null, networkAddr);
    }

    public IpSubnet(String vpnPrefix, String networkAddr) {
        if (networkAddr == null) {
            throw new IllegalArgumentException();
        }
        String[] addrPart = networkAddr.split("/");
        String ipPart;
        String maskPart;
        if (addrPart.length == 2) {
            ipPart = addrPart[0];
            maskPart = addrPart[1];
        } else if (addrPart.length == 3) {
            ipPart = addrPart[1];
            maskPart = addrPart[2];
        } else {
            throw new IllegalArgumentException("Unexpected network-address: " + networkAddr);
        }
        this.vpnPrefix = vpnPrefix;
        try {
            IpAddress.gain(ipPart);
            this.startAddress = ipPart;
        } catch (Exception e) {
            throw new IllegalArgumentException("Illegal network address: " + networkAddr);
        }
        try {
            Integer mask = Integer.valueOf(maskPart);
            this.maskLength = mask;
        } catch (Exception e) {
            throw new IllegalArgumentException("Illegal network address: " + networkAddr);
        }
    }

    public boolean isAlive() {
        for (IpAddressHolder member : this.members) {
            if (member.isDeleted()) {
                return false;
            }
        }
        return true;
    }

    public String getVpnPrefix() {
        return this.vpnPrefix;
    }

    public String getStartAddress() {
        return this.startAddress;
    }

    public Integer getMaskLength() {
        return this.maskLength;
    }

    public String getNetworkAddress() {
        return this.startAddress + "/" + this.maskLength.intValue();
    }

    public String getIpSubnetAddressName() {
        return AbsoluteNameFactory.toIpSubnetAddressName(this.vpnPrefix, this.startAddress, this.maskLength);
    }

    public void addMember(IpAddressHolder holder) {
        if (this.members.contains(holder)) {
            return;
        }
        this.members.add(holder);
    }

    public Set<IpAddressHolder> getMembers() {
        return this.members;
    }

    public boolean isP2pLink() {
        return this.members.size() == 2;
    }

    public void setApplyChange(boolean mode) {
        this.applyChange = mode;
    }

    public boolean canApplyChange() {
        return this.applyChange;
    }

    @Override
    public int hashCode() {
        return getIpSubnetAddressName().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (!(o instanceof IpSubnet)) {
            return false;
        }
        IpSubnet other = (IpSubnet) o;
        return this.getIpSubnetAddressName().equals(other.getIpSubnetAddressName());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.vpnPrefix != null) {
            sb.append(this.vpnPrefix).append("/");
        }
        sb.append(this.startAddress).append("/").append(this.maskLength);
        return sb.toString();
    }

    public String getUpstreamIP(String upstreamDeviceName, String hopIP) {
        if (this.members.size() != 2) {
            return null;
        }
        for (IpAddressHolder member : this.members) {
            String deviceName = member.getDeviceName();
            if (deviceName.equals(upstreamDeviceName)) {
                return member.getIpAddress();
            }
        }
        return null;
    }

    public String getDownstreamDeviceName(String upstreamDeviceName) {
        if (this.members.size() != 2) {
            return null;
        }
        for (IpAddressHolder member : this.members) {
            String deviceName = member.getDeviceName();
            if (!deviceName.equals(upstreamDeviceName)) {
                return member.getDeviceName();
            }
        }
        return null;
    }

    public boolean isTypeMatched() {
        Set<Class<?>> portTypes = new HashSet<Class<?>>();
        for (IpAddressHolder ip : this.members) {
            if (!ip.isDuplicated()) {
                continue;
            }
            if (ip.getNetworkPort() == null) {
                continue;
            }
            Port phy = ConfigModelUtil.getPhysicalPort(ip.getNetworkPort());
            portTypes.add(phy.getClass());
        }
        return portTypes.size() == 1;
    }

    public boolean isVlanConsistent() {
        Set<Integer> vlanIDs = getVlanIDs();
        return vlanIDs.size() <= 1;
    }

    public Integer getVlanID() {
        Set<Integer> vlanIDs = getVlanIDs();
        if (vlanIDs.size() == 0) {
            return null;
        } else if (vlanIDs.size() == 1) {
            return vlanIDs.iterator().next();
        } else {
            return null;
        }
    }

    private Set<Integer> getVlanIDs() {
        Set<Integer> vlanIDs = new HashSet<Integer>();
        for (IpAddressHolder ip : this.members) {
            if (!ip.isVlanConsistent()) {
                return null;
            }
            Integer vlanID = ip.getVlanID();
            vlanIDs.add(vlanID);
        }
        log.debug("subnet's vlan-id: " + vlanIDs);
        return vlanIDs;
    }
}