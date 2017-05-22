package voss.model;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IfNameComparator implements Comparator<String> {

    private static final Pattern SLOT_PORT_PATTERN
            = Pattern.compile("(.*)([0-9]+)/([0-9]+)");
    private static final Pattern NUMBERED_PORT_PATTERN
            = Pattern.compile("(.*)([0-9]+)");

    public synchronized int compare(String ifName1, String ifName2) {
        if (ifName1 == null ? ifName2 == null : ifName1.equals(ifName2)) {
            return 0;
        } else if (ifName1 == null) {
            return -1;
        } else if (ifName2 == null) {
            return 1;
        }

        Matcher slotPortPatternMatcher1;
        Matcher slotPortPatternMatcher2;
        synchronized (SLOT_PORT_PATTERN) {
            slotPortPatternMatcher1 = SLOT_PORT_PATTERN.matcher(ifName1);
            slotPortPatternMatcher2 = SLOT_PORT_PATTERN.matcher(ifName2);
        }

        slotPortPatternMatcher1.find();
        slotPortPatternMatcher2.find();

        boolean slotPortPatternMatches1 = slotPortPatternMatcher1.matches();
        boolean slotPortPatternMatches2 = slotPortPatternMatcher2.matches();

        if (slotPortPatternMatches1 && slotPortPatternMatches2) {
            return compareAsSlotPort
                    (slotPortPatternMatcher1, slotPortPatternMatcher2);
        }

        if (slotPortPatternMatches1 && (!slotPortPatternMatches2)) {
            return 1;
        }

        if ((!slotPortPatternMatches1) && slotPortPatternMatches2) {
            return -1;
        }

        Matcher numberedPortPatternMatcher1;
        Matcher numberedPortPatternMatcher2;
        synchronized (NUMBERED_PORT_PATTERN) {
            numberedPortPatternMatcher1 = NUMBERED_PORT_PATTERN.matcher(ifName1);
            numberedPortPatternMatcher2 = NUMBERED_PORT_PATTERN.matcher(ifName2);
        }

        numberedPortPatternMatcher1.find();
        numberedPortPatternMatcher2.find();

        boolean numberedPortPatternMatches1 = numberedPortPatternMatcher1.matches();
        boolean numberedPortPatternMatches2 = numberedPortPatternMatcher2.matches();

        if (numberedPortPatternMatches1 && numberedPortPatternMatches2) {
            return compareAsNumberedPort
                    (numberedPortPatternMatcher1, numberedPortPatternMatcher2);
        }

        if (numberedPortPatternMatches1 && (!numberedPortPatternMatches2)) {
            return 1;
        }

        if ((!numberedPortPatternMatches1) && numberedPortPatternMatches2) {
            return -1;
        }

        return ifName1.compareTo(ifName2);
    }

    private int compareAsSlotPort
            (Matcher slotPortPatternMatcher1, Matcher slotPortPatternMatcher2) {
        String prefix1 = slotPortPatternMatcher1.group(1);
        String prefix2 = slotPortPatternMatcher2.group(1);
        if (!prefix1.equals(prefix2)) {
            return prefix1.compareTo(prefix2);
        }

        int slot1 = Integer.parseInt(slotPortPatternMatcher1.group(2));
        int slot2 = Integer.parseInt(slotPortPatternMatcher2.group(2));
        int port1 = Integer.parseInt(slotPortPatternMatcher1.group(3));
        int port2 = Integer.parseInt(slotPortPatternMatcher2.group(3));

        if (slot1 == slot2) {
            return port1 - port2;
        }

        return slot1 - slot2;
    }

    private int compareAsNumberedPort
            (Matcher numberedPortPatternMatcher1, Matcher numberedPortPatternMatcher2) {
        String prefix1 = numberedPortPatternMatcher1.group(1);
        String prefix2 = numberedPortPatternMatcher2.group(1);
        if (!prefix1.equals(prefix2)) {
            return prefix1.compareTo(prefix2);
        }

        int port1 = Integer.parseInt(numberedPortPatternMatcher1.group(2));
        int port2 = Integer.parseInt(numberedPortPatternMatcher2.group(2));

        return port1 - port2;
    }
}