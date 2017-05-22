package voss.core.server.naming;

import voss.core.server.config.CoreConfiguration;

public class NamingUtil {

    public static String getIfNameAttributeName() {
        return CoreConfiguration.getInstance().getAttributePolicy().getIfNameAttributeName();
    }
}