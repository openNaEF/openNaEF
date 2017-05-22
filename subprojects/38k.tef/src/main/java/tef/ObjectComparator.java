package tef;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ObjectComparator implements Comparator<Object> {

    private static Map<Class<?>, Comparator<?>> extraComparators__
            = new HashMap<Class<?>, Comparator<?>>();

    public static <T> void addExtraComparator(Class<T> klass, Comparator<T> comparator) {
        extraComparators__.put(klass, comparator);
    }

    public ObjectComparator() {
    }

    public int compare(Object o1, Object o2) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return 1;
        }

        Class<?> type1 = o1.getClass();
        Class<?> type2 = o2.getClass();

        if (type1.isArray() && type2.isArray()) {
            Object[] array1 = (Object[]) o1;
            Object[] array2 = (Object[]) o2;
            if (array1.length != array2.length) {
                return array1.length - array2.length;
            } else {
                for (int i = 0; i < array1.length; i++) {
                    int compared = compare(array1[i], array2[i]);
                    if (compared == 0) {
                        continue;
                    }
                    return compared;
                }
            }
            return 0;
        }

        if (!type1.getName().equals(type2.getName())) {
            return type1.getName().compareTo(type2.getName());
        }

        Class<?> type = type1;
        if (Comparable.class.isAssignableFrom(type)) {
            return ((Comparable) o1).compareTo((Comparable) o2);
        }

        if (MVO.class.isAssignableFrom(type)) {
            MVO mvo1 = (MVO) o1;
            MVO mvo2 = (MVO) o2;
            int transactionSerial1 = mvo1.getInitialVersion().serial;
            int transactionSerial2 = mvo2.getInitialVersion().serial;
            return transactionSerial1 != transactionSerial2
                    ? transactionSerial1 - transactionSerial2
                    : (mvo1.getTransactionLocalSerial()
                    - mvo2.getTransactionLocalSerial());
        }

        if (type == Class.class) {
            return ((Class<?>) o1).getName().compareTo(((Class<?>) o2).getName());
        }

        if (extraComparators__.get(type) != null) {
            Comparator<Object> comparator = (Comparator<Object>) extraComparators__.get(type);
            return comparator.compare(o1, o2);
        }

        throw new RuntimeException(type.getName());
    }
}
