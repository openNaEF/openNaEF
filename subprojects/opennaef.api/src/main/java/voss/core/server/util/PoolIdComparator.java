package voss.core.server.util;

import naef.dto.IdPoolDto;

import java.io.Serializable;
import java.util.Comparator;

public class PoolIdComparator implements Comparator<IdPoolDto<?, ?, ?>>, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(IdPoolDto<?, ?, ?> o1, IdPoolDto<?, ?, ?> o2) {
        return o1.getName().compareTo(o2.getName());
    }

}