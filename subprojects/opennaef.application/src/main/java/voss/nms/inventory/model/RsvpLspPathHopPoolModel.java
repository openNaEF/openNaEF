package voss.nms.inventory.model;

import naef.dto.mpls.RsvpLspHopSeriesDto;
import naef.dto.mpls.RsvpLspHopSeriesIdPoolDto;
import voss.core.server.util.DtoUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RsvpLspPathHopPoolModel implements Serializable {
    private static final long serialVersionUID = 1L;
    private final RsvpLspHopSeriesIdPoolDto pool;
    private final List<String> usedIDs = new ArrayList<String>();

    public RsvpLspPathHopPoolModel(RsvpLspHopSeriesIdPoolDto pool) {
        this.pool = pool;
    }

    public void renew() {
        this.pool.renew();
        this.usedIDs.clear();
        for (RsvpLspHopSeriesDto dto : pool.getUsers()) {
            usedIDs.add(dto.getAbsoluteName());
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
        return pool.getName();
    }

    public String getUsage() {
        return DtoUtil.getString(pool, MPLSNMS_ATTR.PURPOSE);
    }

    public String getStatus() {
        return DtoUtil.getString(pool, MPLSNMS_ATTR.ADMIN_STATUS);
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