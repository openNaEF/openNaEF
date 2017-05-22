package voss.nms.inventory.model;

import naef.dto.IdRange;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.PseudowireLongIdPoolDto;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.IdRangeLongTypeComparator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PseudoWirePoolModel implements Serializable {
    private static final long serialVersionUID = 1L;
    private final PseudowireLongIdPoolDto pool;
    private final List<Long> usedIDs = new ArrayList<Long>();

    public PseudoWirePoolModel(PseudowireLongIdPoolDto pool) {
        this.pool = pool;
    }

    public void renew() {
        this.pool.renew();
        this.usedIDs.clear();
        for (PseudowireDto pw : pool.getUsers()) {
            usedIDs.add(pw.getLongId());
        }
    }

    public String getRange() {
        return pool.getConcatenatedIdRangesStr();
    }

    public String getRemains() {
        long remains = pool.getTotalNumberOfIds() - (long) getUsedIds();
        return String.valueOf(remains);
    }

    public long getRemainsByNumber() {
        long remains = pool.getTotalNumberOfIds() - (long) getUsedIds();
        return remains;
    }

    public String getUsed() {
        return String.valueOf(getUsedIds());
    }

    public int getUsedIds() {
        return usedIDs.size();
    }

    public String getPoolName() {
        return pool.getName();
    }

    public String getUsage() {
        return DtoUtil.getString(pool, "用途");
    }

    public String getStatus() {
        return DtoUtil.getString(pool, "運用状態");
    }

    public String getLastEditor() {
        return DtoUtil.getStringOrNull(pool, "_LastEditor");
    }

    public String getLastEditTime() {
        return DtoUtil.getDateTime(pool, "_LastEditTime");
    }

    public String getVersion() {
        return DtoUtil.getMvoVersionString(pool);
    }

    public String getNote() {
        return DtoUtil.getString(pool, "備考");
    }

    public Long getLowestFreeID() {
        List<IdRange<Long>> ranges = new ArrayList<IdRange<Long>>();
        for (IdRange<Long> range : this.pool.getIdRanges()) {
            ranges.add(range);
        }
        Collections.sort(ranges, new IdRangeLongTypeComparator());
        for (IdRange<Long> range : ranges) {
            for (long curr = range.lowerBound; curr <= range.upperBound; curr++) {
                if (!usedIDs.contains(Long.valueOf(curr))) {
                    return Long.valueOf(curr);
                }
            }
        }
        return null;
    }
}