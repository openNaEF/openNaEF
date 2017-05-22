package voss.core.server.util;

import naef.dto.PortDto;

import java.io.Serializable;

public interface PortFilter extends Serializable {
    boolean match(PortDto port);
}