package voss.core.server.config;

import tef.skelton.dto.EntityDto;

import java.util.Map;

public interface AttributePolicy {
    boolean isCollectionAttribute(String attr);

    boolean isMapAttribute(String attr);

    boolean isExcludeAttr(String attr);

    boolean isPersistentAttribute(String attr);

    boolean isToBeInitializeState(EntityDto o, Map<String, String> map);

    boolean isToBeInitializeState(String status);

    String getPortModeAttributeName();

    boolean isPortModeSwitch(String mode);

    boolean isPortModeRouter(String mode);

    String getSwitchPortModeAttributeName();

    boolean isSwitchPortTrunk(String mode);

    boolean isSwitchPortAccess(String mode);

    boolean isSwitchPortDot1qTunnel(String mode);

    String getIfNameAttributeName();
}