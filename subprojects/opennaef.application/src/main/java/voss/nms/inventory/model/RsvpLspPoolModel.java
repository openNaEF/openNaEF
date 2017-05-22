package voss.nms.inventory.model;

import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspIdPoolDto;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RsvpLspPoolModel implements Serializable {
    private static final long serialVersionUID = 1L;
    private final RsvpLspIdPoolDto pool;
    private final List<String> usedIDs = new ArrayList<String>();

    public RsvpLspPoolModel(RsvpLspIdPoolDto pool) {
        this.pool = pool;
    }

    public void renew() {
        this.pool.renew();
        this.usedIDs.clear();
        for (RsvpLspDto pw : pool.getUsers()) {
            usedIDs.add(pw.getName());
        }
    }

    public String getRange() {
        return pool.getConcatenatedIdRangesStr();
    }

    public String getUsed() {
        return String.valueOf(getUsedIds());
    }

    public int getUsedIds() {
        return usedIDs.size();
    }

    public String getPoolName() {
        if (pool.getName() == null || pool.getName().length() == 0) {
            return "-";
        }
        return pool.getName();
    }

    public String getUsage() {
        return DtoUtil.getString(pool, MPLSNMS_ATTR.PURPOSE);
    }

    public String getStatus() {
        return DtoUtil.getString(pool, MPLSNMS_ATTR.ADMIN_STATUS);
    }

    public boolean isEnabled() {
        return ATTR.ENABLED.equals(getStatus());
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
        return DtoUtil.getString(pool, MPLSNMS_ATTR.NOTE);
    }
}