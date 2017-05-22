package voss.nms.inventory.util;

import naef.dto.PortDto;
import tef.MVO.MvoId;
import voss.core.server.util.DtoUtil;

import java.util.HashMap;
import java.util.Map;

public class PortSet {
    private final Map<MvoId, PortDto> ports = new HashMap<MvoId, PortDto>();

    public void add(PortDto port) {
        if (port == null) {
            return;
        }
        ports.put(DtoUtil.getMvoId(port), port);
    }

    public int size() {
        return this.ports.size();
    }

    public boolean contains(PortDto o) {
        if (o == null) {
            return false;
        }
        return this.ports.keySet().contains(DtoUtil.getMvoId(o));
    }
}