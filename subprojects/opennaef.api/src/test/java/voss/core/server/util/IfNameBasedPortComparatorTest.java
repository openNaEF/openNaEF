package voss.core.server.util;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IfNameBasedPortComparatorTest extends TestCase {

    public void testCompareIfName() {
        IfNameComparator comparator = new IfNameComparator();
        int result;

        result = comparator.compare("ATM1/0", "ATM1/1");
        assertTrue(0 > result);

        result = comparator.compare("ATM1/1", "ATM1/0");
        assertTrue(result > 0);

        result = comparator.compare("ATM1", "ATM1/0");
        assertTrue(0 > result);

        result = comparator.compare("ATM1/0", "ATM1/0.1");
        assertTrue(0 > result);

        result = comparator.compare("ATM1/0:1", "ATM1/0.1");
        assertTrue(0 == result);

        result = comparator.compare("ATM1/0:10", "ATM1/0.1");
        assertTrue(result > 0);

        result = comparator.compare("Gi1/0", "ATM1/0");
        assertTrue(result > 0);

        result = comparator.compare("Gi1/0", "ATM1/1");
        assertTrue(result > 0);

        result = comparator.compare("Gi1/1", "ATM1/0");
        assertTrue(result > 0);

        result = comparator.compare("a1a2", "a12");
        assertTrue(0 > result);

        result = comparator.compare("aa2", "ab1");
        assertTrue(0 > result);

        result = comparator.compare("fastethernet 1/1", "fastethernet 1/0");
        assertTrue(0 < result);

        List<String> list = new ArrayList<String>();
        list.add("fastethernet 1/21");
        list.add("fastethernet 1/10");
        list.add("fastethernet 1/1");
        list.add("fastethernet 1/0");
        list.add("fastethernet 1/4");
        list.add("fastethernet 1/20");
        Collections.sort(list, new IfNameComparator());
        for (String s : list) {
            System.out.println(s);
        }
        assertEquals(list.get(0), "fastethernet 1/0");
        assertEquals(list.get(list.size() - 1), "fastethernet 1/21");
    }
}