package voss.core.server.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryIdComparator implements Comparator<String> {
    private final Map<String, List<String>> cache = new HashMap<String, List<String>>();

    @Override
    public int compare(String o1, String o2) {
        List<String> arr1 = getArray(o1);
        List<String> arr2 = getArray(o2);
        int diff = arr1.size() - arr2.size();
        if (diff != 0) {
            return diff;
        }
        int length = Math.min(arr1.size(), arr2.size());
        for (int i = 0; i < length; i++) {
            diff = arr1.get(i).compareTo(arr2.get(i));
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }

    private List<String> getArray(String s) {
        List<String> result = cache.get(s);
        if (result == null) {
            result = Util.splitWithEscape(s, ":/.");
            cache.put(s, result);
        }
        return result;
    }

}