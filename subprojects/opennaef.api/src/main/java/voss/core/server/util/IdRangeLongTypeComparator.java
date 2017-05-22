package voss.core.server.util;

import naef.dto.IdRange;

import java.util.Comparator;

public class IdRangeLongTypeComparator implements Comparator<IdRange<Long>> {

    @Override
    public int compare(IdRange<Long> o1, IdRange<Long> o2) {
        long diff = o1.lowerBound - o2.lowerBound;
        if (diff > 0L) {
            return 1;
        } else if (diff < 0L) {
            return -1;
        }
        diff = o1.upperBound - o2.upperBound;
        if (diff > 0L) {
            return 1;
        } else if (diff < 0L) {
            return -1;
        } else {
            return 0;
        }
    }

}