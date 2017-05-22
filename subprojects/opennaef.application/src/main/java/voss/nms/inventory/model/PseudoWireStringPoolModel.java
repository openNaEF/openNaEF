package voss.nms.inventory.model;

import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.PseudowireStringIdPoolDto;
import voss.core.server.util.DtoUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PseudoWireStringPoolModel implements Serializable {
    private static final long serialVersionUID = 1L;
    private final PseudowireStringIdPoolDto pool;
    private final List<String> usedIDs = new ArrayList<String>();

    public PseudoWireStringPoolModel(PseudowireStringIdPoolDto pool) {
        this.pool = pool;
    }

    public void renew() {
        this.pool.renew();
        this.usedIDs.clear();
        for (PseudowireDto pw : pool.getUsers()) {
            usedIDs.add(pw.getStringId());
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

    public String getLowestFreeID() {
        return null;
    }
}