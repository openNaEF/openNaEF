package tef;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public final class TefUtils {

    private static Object[] arrayDeepCopy(Object[] original) {
        Object[] copy
                = (Object[]) Array
                .newInstance(original.getClass().getComponentType(), original.length);
        for (int i = 0; i < copy.length; i++) {
            Object element = original[i];
            copy[i] = element == null
                    ? null
                    : (element.getClass().isArray()
                    ? arrayDeepCopy((Object[]) element)
                    : element);
        }
        return copy;
    }

    static Object arrayShallowCopy(Object original) {
        if (original.getClass().getComponentType().isArray()) {
            throw new RuntimeException("nested-array is not supported.");
        }

        Class componentType = original.getClass().getComponentType();
        int length = Array.getLength(original);

        Object copy = Array.newInstance(componentType, length);
        System.arraycopy(original, 0, copy, 0, length);
        return copy;
    }

    static <T> T mutableGuard(T value) {

        if (value == null) {
            return null;
        } else if (value.getClass().isArray()) {
            return (T) arrayShallowCopy(value);
        } else {
            return value;
        }
    }

    static boolean equals(Object o1, Object o2) {
        if (o1 == null && o2 == null) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }
        if (o1.getClass().isArray() && o2.getClass().isArray()) {
            return arrayEquals((Object[]) o1, (Object[]) o2);
        }
        return o1.equals(o2);
    }

    static boolean arrayEquals(Object[] array1, Object[] array2) {
        if (array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (!equals(array1[i], array2[i])) {
                return false;
            }
        }
        return true;
    }

    static boolean isSameMemberArray(Object[] array1, Object[] array2) {
        if (array1 == null && array2 == null) {
            return true;
        }
        if (array1 == null && array2 != null) {
            return false;
        }
        if (array1 != null && array2 == null) {
            return false;
        }
        if (array1.length != array2.length) {
            return false;
        }

        List<Object> list1 = Arrays.asList(array1);
        List<Object> list2 = new ArrayList<Object>(Arrays.asList(array2));
        for (Object o : list1) {
            if (!list2.contains(o)) {
                return false;
            }
            list2.remove(o);
        }
        return true;
    }

    static final <T> T[] getZeroLengthArray(Class<T> componentType) {
        return (T[]) Array.newInstance(componentType, 0);
    }

    public static List<String> getStackTraceLines(TransactionId.W transactionId) {
        return TefService.instance().getTransactionExecLogger()
                .getStacktraceLines(transactionId);
    }

    public static String formatDateYyyymmddhhmmss(Date date) {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(date);
    }

    public static <T> List<T> select(List<?> list, Class<T> type) {
        List<T> result = new ArrayList<T>();
        for (Object obj : list) {
            if (type.isInstance(obj)) {
                result.add(type.cast(obj));
            }
        }
        return result;
    }

    private static Set<String> toUnmodifiableSet(String... values) {
        return Collections.unmodifiableSet
                (new HashSet<String>(Arrays.<String>asList(values)));
    }

    private static final Set<String> trueStrings__;
    private static final Set<String> falseStrings__;

    static {
        trueStrings__ = toUnmodifiableSet("true", "yes", "on");
        falseStrings__ = toUnmodifiableSet("false", "no", "off");
    }

    static Boolean parseAsBoolean(String value) {
        if (value == null) {
            return null;
        }
        if (trueStrings__.contains(value.toLowerCase())) {
            return Boolean.TRUE;
        }
        if (falseStrings__.contains(value.toLowerCase())) {
            return Boolean.FALSE;
        }
        return null;
    }

    static Integer parseAsInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    static String escapeDotAsRegexp(String str) {
        return str.replaceAll("\\.", "\\\\.");
    }

    public static <T extends Enum<T>> T resolveEnum(Class<T> enumType, String name) {
        return Enum.valueOf(enumType, name.toUpperCase().replace('-', '_'));
    }

    public static long getCurrentThreadCpuTime() {
        return ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
    }

    public static long getCurrentThreadUserTime() {
        return ManagementFactory.getThreadMXBean().getCurrentThreadUserTime();
    }

    public static String hexInt(int value) {
        return Integer.toString(value, 16);
    }

    public static String hexLong(long value) {
        return Long.toString(value, 16);
    }
}
