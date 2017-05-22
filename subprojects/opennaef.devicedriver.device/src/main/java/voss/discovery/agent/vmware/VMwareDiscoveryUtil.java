package voss.discovery.agent.vmware;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VMwareDiscoveryUtil {
    public static final String uplinkIfNameRegex = "key-vim\\.host\\.PhysicalNic-(.*)";
    public static final Pattern uplinkIfNamePattern = Pattern.compile(uplinkIfNameRegex);

    public static String getVmnicName(String key) {
        Matcher m = uplinkIfNamePattern.matcher(key);
        if (!m.find()) {
            return key;
        }
        return m.group(1);
    }
}