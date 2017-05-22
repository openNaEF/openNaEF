package voss.core.server.util;

import junit.framework.TestCase;

public class Test1 extends TestCase {

    public void testToSubnetMask() {
        assertEquals("255.255.255.255", IpSubnetAddressUtil.toSubnetMask(Integer.valueOf(32)));
        assertEquals("255.255.255.0", IpSubnetAddressUtil.toSubnetMask(Integer.valueOf(24)));
        assertEquals("255.255.0.0", IpSubnetAddressUtil.toSubnetMask(Integer.valueOf(16)));
        assertEquals("128.0.0.0", IpSubnetAddressUtil.toSubnetMask(Integer.valueOf(1)));
        assertEquals("0.0.0.0", IpSubnetAddressUtil.toSubnetMask(Integer.valueOf(0)));
    }

}