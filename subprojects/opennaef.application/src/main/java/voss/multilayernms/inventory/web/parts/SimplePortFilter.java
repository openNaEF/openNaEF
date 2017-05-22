package voss.multilayernms.inventory.web.parts;

import naef.dto.PortDto;
import voss.core.server.util.PortFilter;

public class SimplePortFilter implements PortFilter {
    private static final long serialVersionUID = 1L;

    public boolean match(PortDto port) {
        return true;
    }
}