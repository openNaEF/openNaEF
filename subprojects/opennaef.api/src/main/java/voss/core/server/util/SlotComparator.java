package voss.core.server.util;

import naef.dto.SlotDto;

import java.util.Comparator;

public class SlotComparator implements Comparator<SlotDto> {
    @Override
    public int compare(SlotDto o1, SlotDto o2) {
        try {
            int i1 = Integer.parseInt(o1.getName());
            int i2 = Integer.parseInt(o2.getName());
            return i1 - i2;
        } catch (NumberFormatException e) {
            return o1.getName().compareTo(o2.getName());
        }
    }

}