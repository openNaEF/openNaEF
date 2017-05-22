package voss.nms.inventory.util;

import naef.dto.PortDto;
import voss.core.server.util.PortFilter;

public class NullPortListFilter implements PortFilter {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean match(PortDto port) {
        return true;
    }

}