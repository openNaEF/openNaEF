package voss.nms.inventory.model;

import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIdPoolDto;
import voss.core.server.util.DtoUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class VlanPoolModel implements Serializable {
    private static final long serialVersionUID = 1L;
    private final VlanIdPoolDto pool;
    private final List<Integer> usedIDs = new ArrayList<Integer>();

    public VlanPoolModel(VlanIdPoolDto pool) {
        this.pool = pool;
    }

    public void renew() {
        this.pool.renew();
        for (VlanDto vlan : pool.getUsers()) {
            this.usedIDs.add(vlan.getVlanId());
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
        return this.usedIDs.size();
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
}