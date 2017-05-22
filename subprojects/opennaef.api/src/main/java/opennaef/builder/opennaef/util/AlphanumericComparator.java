package opennaef.builder.opennaef.util;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlphanumericComparator implements Comparator<String> {
    public static final AlphanumericComparator INSTANCE = new AlphanumericComparator();

    private Pattern alphaNumChunkPattern = Pattern.compile("(\\d+\\.\\d+)|(\\d+)|(\\D+)");

    private AlphanumericComparator() {
    }

    public int compare(String raw1, String raw2) {
        Matcher chunk1 = alphaNumChunkPattern.matcher(raw1);
        Matcher chunk2 = alphaNumChunkPattern.matcher(raw2);

        while (chunk1.find() && chunk2.find()) {
            String s1 = chunk1.group();
            String s2 = chunk2.group();
            try {
                Double d1 = Double.valueOf(s1);
                Double d2 = Double.valueOf(s2);
                int compareValue = d1.compareTo(d2);
                if (compareValue != 0) {
                    return compareValue;
                }
            } catch (NumberFormatException e) {
                int compareValue = s1.compareTo(s2);
                if (compareValue != 0) {
                    return compareValue;
                }
            }

            if (chunk1.hitEnd() && !chunk2.hitEnd()) {
                return -1;
            } else if (!chunk1.hitEnd() && chunk2.hitEnd()) {
                return 1;
            }
        }
        return 0;
    }
}