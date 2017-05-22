package voss.util;

import voss.model.CidrAddress;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VossMiscUtility {

    public synchronized static String showDetail(String val) {
        StringBuffer sb = new StringBuffer();
        byte[] b = val.getBytes();
        for (int i = 0; i < b.length; i++) {
            sb.append(Integer.toHexString((int) b[i]) + ":");
        }
        sb.append("=>[");
        for (int i = 0; i < b.length; i++) {
            sb.append((char) b[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static InetAddress getNetworkAddress(InetAddress nodeAddress, int maskLength) {
        assert nodeAddress != null;
        assert 0 < maskLength;
        assert (nodeAddress instanceof Inet4Address && maskLength <= 32)
                || (nodeAddress instanceof Inet6Address && maskLength <= 128);

        byte[] addr = nodeAddress.getAddress();
        try {
            InetAddress networkAddress = InetAddress.getByAddress(getNetworkAddress(addr, maskLength));
            return networkAddress;
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static byte[] getNetworkAddress(byte[] addr, int maskLength) {
        assert addr != null;
        assert 0 <= maskLength;
        assert addr.length * 8 >= maskLength;
        assert addr.length == 4 || addr.length == 16;

        int offset = 0;
        byte[] result = new byte[addr.length];
        for (int x = 0; x < (addr.length / 2); x++) {
            long upper = (long) addr[x << 1] << 8;
            long lower = (long) addr[(x << 1) + 1];
            long concatinated = (upper & 0xff00) | (lower & 0xff);
            if (maskLength < (offset + 16)) {
                int len = ((offset + 16) - maskLength > 16 ? 16 : (offset + 16) - maskLength);
                concatinated = (long) (concatinated >> len);
                concatinated = (long) (concatinated << len);
            }
            result[(x << 1) + 1] = (byte) (concatinated & 0xff);
            concatinated = (long) (concatinated >> 8);
            result[x << 1] = (byte) (concatinated & 0xff);
            offset = offset + 16;
        }
        return result;
    }

    public static boolean isInSubnet(InetAddress networkAddress, int maskLength, InetAddress nodeAddress) {
        return isInSubnet(networkAddress.getAddress(), maskLength, nodeAddress.getAddress());
    }

    public static boolean isInSubnet(byte[] networkAddress, int maskLength, byte[] nodeAddress) {
        assert networkAddress != null;
        assert nodeAddress != null;
        assert maskLength <= networkAddress.length * 8;

        if (networkAddress.length != nodeAddress.length) {
            return false;
        }

        byte[] networkAddr = getNetworkAddress(networkAddress, maskLength);
        byte[] nodeAddr = getNetworkAddress(nodeAddress, maskLength);
        for (int i = 0; i < networkAddr.length; i++) {
            if (networkAddr[i] != nodeAddr[i]) {
                return false;
            }
        }
        return true;
    }

    public synchronized static byte[] getByteFormIpAddress(String ipAddress) {
        Pattern ipv4Pattern = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+");
        Pattern ipv6Pattern = Pattern.compile("[0-9a-fA-F:-]+");
        if (ipAddress == null) {
            throw new IllegalArgumentException();
        }

        Matcher v4matcher = ipv4Pattern.matcher(ipAddress);
        if (v4matcher.matches()) {
            byte[] result = new byte[4];
            String[] v4 = ipAddress.split("\\.");
            if (v4.length != 4) {
                throw new IllegalArgumentException("invalid IPv4 address: " + ipAddress);
            }
            for (int i = 0; i < 4; i++) {
                int a = Integer.parseInt(v4[i]);
                result[i] = (byte) a;
            }
            return result;
        }

        Matcher v6matcher = ipv6Pattern.matcher(ipAddress);
        if (v6matcher.matches()) {
            byte[] result = new byte[16];
            String[] v6;
            if (ipAddress.indexOf(":") != -1) {
                v6 = ipAddress.split(":");
            } else if (ipAddress.indexOf("-") != -1) {
                v6 = ipAddress.split("-");
            } else {
                throw new IllegalArgumentException();
            }
            if (v6.length > 8) {
                throw new IllegalArgumentException("invalid IPv6 address: " + ipAddress);
            }

            int trueLength = 0;
            for (String s : v6) {
                if (getNullIfLengthZero(s) != null) {
                    trueLength++;
                }
            }
            int lengthOfZeroCompletion = (8 - trueLength) * 2;
            int position = 0;
            for (int i = 0; i < v6.length; i++) {
                String element = getNullIfLengthZero(v6[i]);

                if (element != null) {
                    if (element.length() > 4) {
                        throw new IllegalArgumentException("invalid IPv6 address: [" + v6[i] + "] in (" + ipAddress + ")");
                    }
                    element = "0000".substring(0, 4 - element.length()) + element;
                    String upper = "0x" + element.substring(0, 2).toUpperCase();
                    String lower = "0x" + element.substring(2, 4).toUpperCase();
                    byte upperByte = (byte) Short.decode(upper).shortValue();
                    byte lowerByte = (byte) Short.decode(lower).shortValue();
                    result[position++] = upperByte;
                    result[position++] = lowerByte;
                } else {
                    for (int j = 0; j < lengthOfZeroCompletion; j++) {
                        result[position++] = (byte) 0;
                    }
                }
            }
            if (position != 16) {
                throw new IllegalStateException();
            }
            return result;
        }
        throw new IllegalArgumentException("invalied ip address: " + ipAddress);
    }

    private static String getNullIfLengthZero(String s) {
        if (s == null) {
            return s;
        }
        if (s.length() > 0) {
            return s;
        }
        return null;
    }

    public synchronized static <T> Set<T> arrayToSet(T[] array) {
        Set<T> result = new HashSet<T>();
        for (T element : array) {
            result.add(element);
        }
        return result;
    }

    public static Comparator<CidrAddress> getComparator() {
        Comparator<CidrAddress> comparator = new Comparator<CidrAddress>() {
            public int compare(CidrAddress o1, CidrAddress o2) {
                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o2 == null) {
                    return 1;
                } else if (o1 == null) {
                    return -1;
                }
                if (o1 == o2) {
                    return 0;
                }
                if (o1.addr.getAddress().length > o2.addr.getAddress().length) {
                    return -1;
                } else if (o2.addr.getAddress().length > o1.addr.getAddress().length) {
                    return 1;
                }
                for (int i = 0; i < o1.addr.getAddress().length; i++) {
                    int r = (o1.addr.getAddress()[i] & 0xff) - (o2.addr.getAddress()[i] & 0xff);
                    if (r != 0) {
                        return r;
                    }
                }
                return 0;
            }
        };
        return comparator;
    }

    public static String getConcatinatedRange(List<? extends Number> numbers) {
        StringBuilder sb = new StringBuilder();
        if (numbers.size() == 0) {
            return sb.toString();
        }
        Number prev = null;
        Number current = null;
        for (int i = 0; i < numbers.size(); i++) {
            current = numbers.get(i);
            if (prev != null) {
                if (numbers.get(i - 1).intValue() == (numbers.get(i).intValue() - 1)) {
                    continue;
                } else {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(prev);
                    if (prev != numbers.get(i - 1)) {
                        sb.append("-").append(numbers.get(i - 1));
                    }
                    prev = current;
                }
            } else {
                prev = current;
            }
        }
        if (sb.length() > 0) {
            sb.append(",");
        }
        sb.append(prev);
        if (prev != current) {
            sb.append("-").append(current);
        }
        return sb.toString();
    }

}