package voss.model;

import voss.util.VossMiscUtility;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

public class CidrAddress implements Serializable {
    private static final long serialVersionUID = 1L;
    public final InetAddress addr;
    public final int masklen;

    public CidrAddress() {
        this.addr = null;
        this.masklen = 0;
    }

    public CidrAddress(InetAddress addr, int masklen) {
        if (addr == null) {
            throw new IllegalArgumentException("addr is null");
        }
        this.addr = addr;
        this.masklen = masklen;
    }

    public InetAddress getAddress() {
        return this.addr;
    }

    public int getSubnetMaskLength() {
        return this.masklen;
    }

    public boolean isV4Address() {
        return this.addr instanceof Inet4Address;
    }

    public boolean isV6Address() {
        return this.addr instanceof Inet6Address;
    }

    public InetAddress getNetworkAddress() {
        if (this.isV4Address() || this.isV6Address()) {
            return VossMiscUtility.getNetworkAddress(addr, masklen);
        }
        throw new IllegalStateException();
    }

    @Override
    public String toString() {
        if (this.addr == null) {
            return "0.0.0.0/0";
        }
        return this.addr.getHostAddress() + "/" + this.masklen;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof CidrAddress)) {
            return false;
        }
        return this.toString().equals(((CidrAddress) o).toString());
    }

    @Override
    public int hashCode() {
        int key = this.toString().hashCode();
        return key * key + key + 41;
    }

}