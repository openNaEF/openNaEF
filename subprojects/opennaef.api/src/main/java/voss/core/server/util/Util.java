package voss.core.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.dto.EntityDto;
import tef.skelton.dto.EntityDto.Desc;
import voss.core.server.exception.InventoryException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;

public class Util {
    public static final char escapeChar = '\\';
    public static final String escapeTarget = "\"~'";
    public static final String CR = "~~";
    public static final String LF = "''";

    private static Logger logger = null;

    private static Logger log() {
        if (logger == null) {
            logger = LoggerFactory.getLogger(Util.class);
        }
        return logger;
    }

    public static Boolean isSame(Object o1, Object o2) {
        if (o1 == null && o2 == null) {
            return Boolean.TRUE;
        } else if (o1 == o2) {
            return Boolean.TRUE;
        } else if (o1 == null) {
            return Boolean.FALSE;
        } else if (o2 == null) {
            return Boolean.FALSE;
        }
        return null;
    }

    public static boolean equals(Object o1, Object o2) {
        Boolean same = isSame(o1, o2);
        if (same != null) {
            return same.booleanValue();
        }
        return o1.equals(o2);
    }

    public static String trim(String s) {
        if (s == null) {
            return s;
        }
        return s.trim();
    }

    public static boolean equalsAll(Object arg1, Object... args) {
        if (isAllNull(arg1, args)) {
            return true;
        } else if (arg1 == null) {
            return false;
        }
        Object prev = arg1;
        for (Object o : args) {
            if (o == null) {
                return false;
            } else if (!prev.equals(o)) {
                return false;
            }
            prev = o;
        }
        return true;
    }

    public static String escapeForCommand(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            int ch = s.codePointAt(i);
            if (ch == '"') {
                sb.append('\\');
            }
            sb.append((char) ch);
        }
        return sb.toString();
    }

    public static String stripCrlf(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch < 0x20) {
                continue;
            } else if (ch > 0x7E) {
                continue;
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    public static String escapeCrlf(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (escapeTarget.indexOf(ch) != -1) {
                sb.append(escapeChar);
            } else if (ch == 0x13) {
                sb.append(CR);
                continue;
            } else if (ch == 0x10) {
                sb.append(LF);
                continue;
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    public static String unescapeCrlf(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int length = s.length();
        for (int i = 0; i < length; i++) {
            char ch = s.charAt(i);
            if (ch == escapeChar && (i + 1) < length) {
                char ch2 = s.charAt(i + 1);
                if (escapeTarget.indexOf(ch2) != -1) {
                    sb.append(ch2);
                } else {
                    sb.append(ch).append(ch2);
                }
                i++;
            } else if (escapeTarget.indexOf(ch) != -1 && (i + 1) < length) {
                char ch2 = s.charAt(i + 1);
                if (isSame(ch, ch2, CR)) {
                    sb.append("\r");
                } else if (isSame(ch, ch2, LF)) {
                    sb.append("\n");
                } else {
                    sb.append(ch).append(ch2);
                }
                i++;
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static boolean isSame(char ch1, char ch2, String s) {
        if (s.getBytes().length != 2) {
            return false;
        }
        byte[] bytes = s.getBytes();
        return bytes[0] == ch1 && bytes[1] == ch2;
    }

    public static List<String> splitWithEscape(String s, String splitChars) {
        if (splitChars == null) {
            splitChars = "";
        }
        List<String> result = new ArrayList<String>();
        if (s == null) {
            return result;
        }
        boolean escaped = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = (char) s.codePointAt(i);
            if (escaped) {
                sb.append(ch);
                escaped = false;
            } else {
                if (ch == escapeChar) {
                    escaped = true;
                    continue;
                } else {
                    if (splitChars.indexOf(ch) > -1) {
                        result.add(sb.toString());
                        sb = new StringBuilder();
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        result.add(sb.toString());
        return result;
    }

    public static String encodeUTF8(String s) {
        if (s == null) {
            return s;
        }
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log().error("illegal encoding.", e);
        }
        return s;
    }

    public static String decodeUTF8(String s) {
        if (s == null) {
            return s;
        }
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log().error("illegal encoding.", e);
        }
        return s;
    }

    public static boolean hasDiff(String before, String after) {
        before = stringToNull(before);
        after = stringToNull(after);
        if (before == null) {
            return after != null;
        }
        return !before.equals(after);
    }

    public static <T> List<T> getAddedList(Collection<T> before, Collection<T> after) {
        if (before == null) {
            return new ArrayList<T>(after);
        } else if (after == null) {
            return new ArrayList<T>();
        }
        Set<Object> keys = new HashSet<Object>();
        for (T o : before) {
            if (o == null) {
                continue;
            }
            Object key = getListDiffKey(o);
            keys.add(key);
        }
        List<T> result = new ArrayList<T>();
        for (T e : after) {
            Object key = getListDiffKey(e);
            if (keys.contains(key)) {
                continue;
            }
            result.add(e);
        }
        return result;
    }

    public static <T> List<T> getRemovedList(Collection<T> before, Collection<T> after) {
        if (after == null) {
            return new ArrayList<T>(before);
        } else if (before == null) {
            return new ArrayList<T>();
        }
        Set<Object> keys = new HashSet<Object>();
        for (T o : after) {
            if (o == null) {
                continue;
            }
            Object key = getListDiffKey(o);
            keys.add(key);
        }
        List<T> result = new ArrayList<T>();
        for (T e : before) {
            Object key = getListDiffKey(e);
            if (keys.contains(key)) {
                continue;
            }
            result.add(e);
        }
        return result;
    }

    public static List<?> getCommonList(Collection<?> before, Collection<?> after) {
        if (after == null) {
            return new ArrayList<Object>(before);
        } else if (before == null) {
            return new ArrayList<Object>();
        }
        Set<Object> keys = new HashSet<Object>();
        for (Object o : after) {
            if (o == null) {
                continue;
            }
            Object key = getListDiffKey(o);
            keys.add(key);
        }
        List<Object> result = new ArrayList<Object>();
        for (Object e : before) {
            Object key = getListDiffKey(e);
            if (keys.contains(key)) {
                result.add(e);
            }
        }
        return result;
    }

    private static Object getListDiffKey(Object o) {
        if (o == null) {
            return null;
        } else if (EntityDto.class.isInstance(o)) {
            return DtoUtil.getMvoId(((EntityDto) o));
        } else if (Desc.class.isInstance(o)) {
            return DtoUtil.getMvoId(((Desc<?>) o));
        }
        return o;
    }

    public static boolean isNull(Object... args) {
        for (Object o : args) {
            if (o == null) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAllNull(Object... args) {
        for (Object o : args) {
            if (o != null) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAllNotNull(Object... args) {
        for (Object o : args) {
            if (o == null) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAllOrNothing(Object... args) {
        if (isAllNull(args)) {
            return true;
        }
        return !isNull(args);
    }

    public static boolean contains(String s, String... contained) {
        if (s == null) {
            return false;
        }
        for (String c : contained) {
            if (s.contains(c)) {
                return true;
            }
        }
        return false;
    }

    public static String getLocalhostAddress() {
        try {
            List<String> list = new ArrayList<String>();
            Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
            while (ifs.hasMoreElements()) {
                NetworkInterface ni = ifs.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    String addr = address.getHostAddress();
                    if (addr.equals("127.0.0.1")) {
                        continue;
                    } else if (addr.indexOf(':') != -1) {
                        continue;
                    }
                    list.add(addr);
                }
            }
            Collections.sort(list);
            String first = list.get(0);
            if (first != null) {
                return first;
            }
        } catch (SocketException e) {
            log().error("failed to get local address.", e);
        }
        return "127.0.0.1";
    }

    public static String formatRange(String range) {
        range = range.trim();
        if (range.indexOf(',') > -1) {
            return formatRanges(range);
        }
        if (range.matches("^[0-9]+$")) {
            return range + "-" + range;
        }
        return range;
    }

    private static String formatRanges(String ranges) {
        StringBuilder sb = new StringBuilder();
        for (String range : ranges.split(",")) {
            range = range.trim();
            range = formatRange(range);
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(range);
        }
        return sb.toString();
    }

    public static void checkRange(String range, boolean allowZero) throws InventoryException {
        if (range == null) {
            throw new InventoryException("The ID range is unspecified.");
        }
        range = range.trim();
        if (range.indexOf('-') == -1) {
            throw new InventoryException("\"-\" is not included in ID range.");
        }
        if (range.indexOf(',') > -1) {
            for (String range_ : range.split(",")) {
                range_ = range_.trim();
                checkRangeInner(range_, allowZero);
            }
        } else {
            checkRangeInner(range, allowZero);
        }
    }

    private static void checkRangeInner(String range, boolean allowZero) throws InventoryException {
        String[] s = range.split("-");
        if (s.length > 2) {
            throw new InventoryException("Two or more integers are specified in the ID rang.");
        } else if (s.length < 2) {
            throw new InventoryException("The ID range must consist of two integers.");
        }
        try {
            long long1 = Long.parseLong(s[0]);
            long long2 = Long.parseLong(s[1]);
            if (long1 > long2) {
                throw new InventoryException("ID range must be [lower limit] <= [upper limit]. Can not be specified like 100-10.");
            }
            if (long1 < 0) {
                throw new InventoryException("ID range must be a positive integer.");
            }
            if (long1 == 0 && !allowZero) {
                throw new InventoryException("ID range can not contain 0.");
            }
        } catch (NumberFormatException e) {
            throw new InventoryException("Please specify ID range with half-width positive integers and \"-\".");
        }
    }

    public static <K, T> Set<T> getOrCreateSet(Map<K, Set<T>> map, K key) {
        Set<T> set = map.get(key);
        if (set == null) {
            set = new HashSet<T>();
            map.put(key, set);
        }
        return set;
    }

    public static <K, T> List<T> getOrCreateList(Map<K, List<T>> map, K key) {
        List<T> set = map.get(key);
        if (set == null) {
            set = new ArrayList<T>();
            map.put(key, set);
        }
        return set;
    }

    public static <K, T1, T2> Map<T1, T2> getOrCreateMap(Map<K, Map<T1, T2>> map, K key) {
        Map<T1, T2> set = map.get(key);
        if (set == null) {
            set = new HashMap<T1, T2>();
            map.put(key, set);
        }
        return set;
    }

    public static String getNumberChars(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ('0' <= c && c <= '9') {
                if (i == 1 && s.charAt(0) == '-') {
                    sb.append('-');
                }
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            return null;
        } else {
            return sb.toString();
        }
    }

    public static int parseInt(String s, int defaultValue) {
        String ch = getNumberChars(s);
        if (ch == null) {
            return defaultValue;
        }
        return Integer.parseInt(ch);
    }

    public static long parseLong(String s, long defaultValue) {
        String ch = getNumberChars(s);
        if (ch == null) {
            return defaultValue;
        }
        return Long.parseLong(ch);
    }

    public static BigInteger parseBigInteger(String s, BigInteger defaultValue) {
        String ch = getNumberChars(s);
        if (ch == null) {
            return defaultValue;
        }
        return new BigInteger(ch);
    }

    public static int compareLong(long l1, long l2) {
        if (l1 < l2) {
            return -1;
        } else if (l1 > l2) {
            return 1;
        }
        return 0;
    }

    public static Integer compare(Object o1, Object o2) {
        if (o1 == null && o2 == null) {
            return Integer.valueOf(0);
        } else if (o1 == null) {
            return Integer.valueOf(-1);
        } else if (o2 == null) {
            return Integer.valueOf(1);
        }
        return null;
    }

    public static String removeSlashes(String ifName) {
        if (ifName == null) {
            return ifName;
        }
        ifName = ifName.replace("//", "/");
        if (ifName.endsWith("/")) {
            ifName = ifName.substring(0, ifName.length() - 1);
        }
        if (ifName.startsWith("/")) {
            ifName = ifName.substring(1, ifName.length());
        }
        return ifName;
    }

    public static boolean isAllTrue(boolean... conditions) {
        if (conditions == null) {
            return false;
        }
        for (boolean condition : conditions) {
            if (!condition) {
                return false;
            }
        }
        return true;
    }

    public static String getSuffix(String ifName) {
        if (ifName == null || ifName.length() == 0) {
            return null;
        }
        int lastIndex = ifName.lastIndexOf('.');
        if (lastIndex < 0) {
            return null;
        } else if (lastIndex >= (ifName.length() - 1)) {
            return null;
        }
        return ifName.substring(lastIndex + 1);
    }

    public static String s2n(String s) {
        if (s == null) {
            return null;
        } else if (s.length() == 0) {
            return null;
        }
        return s;
    }

    public static <T extends Enum<T>> boolean isOneOf(Enum<T> testee, Enum<T>... tests) {
        if (testee == null) {
            return false;
        }
        for (Enum<T> test : tests) {
            if (testee.equals(test)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOneOf(Object testee, Object... tests) {
        if (testee == null) {
            return false;
        }
        for (Object test : tests) {
            if (testee.equals(test)) {
                return true;
            }
        }
        return false;
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

    public static String rangeToString(List<String> list) {
        if (list == null || list.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String allowedVlan : list) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(allowedVlan);
        }
        return sb.toString();
    }

    public static String nullToString(String s) {
        if (s == null) {
            return "";
        }
        return s;
    }

    public static String crLfToBr(String s) {
        StringBuilder sb = new StringBuilder();
        boolean crlf = false;
        for (int i = 0; i < s.length(); i++) {
            int cp = s.codePointAt(i);
            if (cp == '\r' || cp == '\n') {
                if (crlf) {
                    continue;
                } else {
                    sb.append("<br/>");
                    crlf = true;
                }
            } else {
                crlf = false;
            }
            sb.append((char) (cp & 0xffff));
        }
        return sb.toString();
    }

    public static List<String> stringToLines(String s) {
        List<String> lines = new ArrayList<String>();
        if (s == null) {
            return lines;
        }
        BufferedReader br = new BufferedReader(new StringReader(s));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (IOException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
            }
        }
    }

    public static String stringToNull(String s) {
        if (s == null) {
            return null;
        } else if (s.equals("")) {
            return null;
        }
        return s;
    }

    public static boolean isNull2(Object value) {
        if (value == null) {
            return true;
        } else if (value instanceof String && ((String) value).equals("")) {
            return true;
        }
        return false;
    }

    public static String addString(String s, String prefix, String suffix) {
        if (s == null) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        if (prefix != null) {
            sb.append(prefix);
        }
        sb.append(s);
        if (suffix != null) {
            sb.append(suffix);
        }
        return sb.toString();
    }

    public static boolean isPartialMatch(String s, String query) {
        s = stringToNull(s);
        if (s == null && query == null) {
            return true;
        } else if (s == null || query == null) {
            return false;
        }
        if (query.indexOf('*') != -1) {
            if (query.indexOf('*') != query.lastIndexOf('*')) {
                throw new IllegalStateException("There is only one * that can be specified in the search condition.");
            }
            if (query.startsWith("*")) {
                String q = query.substring(1);
                return s.toLowerCase().endsWith(q.toLowerCase());
            } else if (query.endsWith("*")) {
                String q = query.substring(0, query.length() - 1);
                return s.toLowerCase().startsWith(q.toLowerCase());
            } else {
                String[] q = query.split("\\*");
                if (q.length != 2) {
                    throw new IllegalStateException("Illegal search condition: " + query);
                }
                return s.toLowerCase().startsWith(q[0].toLowerCase())
                        && s.toLowerCase().endsWith(q[1].toLowerCase());
            }
        }
        return s.toLowerCase().contains(query.toLowerCase());
    }

    public static String trimFront(String s) {
        if (s == null) {
            return null;
        } else if (!s.startsWith(" ")) {
            return s;
        }
        int nonSpace = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != ' ') {
                nonSpace = i;
                break;
            }
        }
        return s.substring(nonSpace);
    }

    public static String trimTail(String s) {
        if (s == null) {
            return null;
        } else if (!s.endsWith(" ")) {
            return s;
        }
        int nonSpace = 0;
        for (int i = (s.length() - 1); i >= 0; i--) {
            if (s.charAt(i) != ' ') {
                nonSpace = i;
                break;
            }
        }
        return s.substring(0, nonSpace + 1);
    }

    public static boolean isSameList(List<String> list1, List<String> list2) {
        if (list1 == null && list2 == null) {
            return true;
        } else if (list1 == null) {
            return false;
        } else if (list2 == null) {
            return false;
        }
        if (list1.size() != list2.size()) {
            return false;
        }
        for (String item : list1) {
            if (!list2.contains(item)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isSameSet(Set<String> set1, Set<String> set2) {
        if (set1 == null && set2 == null) {
            return true;
        } else if (set1 == null) {
            return false;
        } else if (set2 == null) {
            return false;
        }
        if (set1.size() != set2.size()) {
            return false;
        }
        for (String item : set1) {
            if (!set2.contains(item)) {
                return false;
            }
        }
        return true;
    }

    public static void appendIfNotNull(StringBuilder sb, String prefix, String value, String suffix) {
        if (value == null) {
            return;
        }
        if (prefix != null) {
            sb.append(prefix);
        }
        sb.append(value);
        if (suffix != null) {
            sb.append(suffix);
        }
    }

    public static List<String> sortNames(String... names) {
        List<String> result = new ArrayList<String>();
        for (String name : names) {
            if (name == null) {
                continue;
            }
            result.add(name);
        }
        Collections.sort(result);
        return result;
    }

    public static boolean isInstanceOf(Object dto, Class<?>... classes) {
        for (Class<?> cls : classes) {
            if (cls.isInstance(dto)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAllInstanceOf(Class<?> cls, Object... args) {
        for (Object arg : args) {
            if (!cls.isInstance(arg)) {
                return false;
            }
        }
        return true;
    }

    public static <T extends EntityDto> T getInstance(Class<T> class_, Collection<EntityDto> set) {
        if (set == null) {
            return null;
        } else if (class_ == null) {
            throw new IllegalArgumentException("class_ is null.");
        }
        for (EntityDto dto : set) {
            if (class_.isInstance(dto)) {
                return class_.cast(dto);
            }
        }
        return null;
    }

    public static String getDigest(String scheme, String value) {
        if (scheme == null) {
            throw new IllegalArgumentException("illegal scheme: null");
        } else if (value == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(scheme);
            digest.update(value.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }
}