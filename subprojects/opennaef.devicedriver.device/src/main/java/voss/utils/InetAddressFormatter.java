package voss.utils;

import voss.util.VossMiscUtility;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

public class InetAddressFormatter extends Format {
    private static final long serialVersionUID = 1L;

    public StringBuffer format(final Object obj, final StringBuffer sb,
                               final FieldPosition pos) {
        if (obj instanceof InetAddress) {
            sb.append(((InetAddress) obj).getHostAddress());
        }
        return sb;
    }

    @Override
    public Object parseObject(final String src, final ParsePosition pos) {
        try {
            String str = src.substring(pos.getIndex());
            if (0 < str.indexOf(':')) {
                byte[] addr = VossMiscUtility.getByteFormIpAddress(str);
                return Inet6Address.getByAddress(addr);
            } else if (0 < str.indexOf('.')) {
                byte[] addr = VossMiscUtility.getByteFormIpAddress(str);
                return Inet4Address.getByAddress(addr);
            } else {
                return null;
            }
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    public static void main(final String[] argv) throws Exception {
        System.err.println(new InetAddressFormatter().parseObject(argv[0]));
    }
}