package voss.discovery.agent.vmware.collector;

import java.util.Map;

public class MIBResponse {
    private Map<String, Integer> prefixLengths;

    public void setPrefixLengths(Map<String, Integer> prefixLengths) {
        this.prefixLengths = prefixLengths;
    }

    public Map<String, Integer> getPrefixLengths() {
        return prefixLengths;
    }
}