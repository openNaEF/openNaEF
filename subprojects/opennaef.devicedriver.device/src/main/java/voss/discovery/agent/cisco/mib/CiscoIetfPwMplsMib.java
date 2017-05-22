package voss.discovery.agent.cisco.mib;

public class CiscoIetfPwMplsMib {
    public static final String CISCO_IETF_PW_MPLS_MIB_BASE = ".1.3.6.1.4.1.9.10.107";
    public static final String cpwVcMplsObjects = CISCO_IETF_PW_MPLS_MIB_BASE + ".1";

    public static final String cpwVcMplsEntry = cpwVcMplsObjects + ".1.1";
    public static final String cpwVcMplsOutboundEntry = cpwVcMplsObjects + ".3.1";
    public static final String cpwVcMplsInboundEntry = cpwVcMplsObjects + ".5.1";
    public static final String cpwVcMplsNonTeMappingEntry = cpwVcMplsObjects + ".6.1";
    public static final String cpwVcMplsTeMappingEntry = cpwVcMplsObjects + ".7.1";

    public static final String cpwVcMplsType = cpwVcMplsEntry + ".1";

    public static final String cpwVcMplsOutboundLsrXcIndex = cpwVcMplsOutboundEntry + ".2";
    public static final String cpwVcMplsOutboundTunnelIndex = cpwVcMplsOutboundEntry + ".3";
    public static final String cpwVcMplsOutboundTunnelInstance = cpwVcMplsOutboundEntry + ".4";
    public static final String cpwVcMplsOutboundTunnelLclLSR = cpwVcMplsOutboundEntry + ".5";
    public static final String cpwVcMplsOutboundTunnelPeerLSR = cpwVcMplsOutboundEntry + ".6";

    public static final String cpwVcMplsInboundLsrXcIndex = cpwVcMplsInboundEntry + ".2";
    public static final String cpwVcMplsInboundTunnelIndex = cpwVcMplsInboundEntry + ".3";
    public static final String cpwVcMplsInboundTunnelInstance = cpwVcMplsInboundEntry + ".4";
    public static final String cpwVcMplsInboundTunnelLclLSR = cpwVcMplsInboundEntry + ".5";
    public static final String cpwVcMplsInboundTunnelPeerLSR = cpwVcMplsInboundEntry + ".6";
    public static final String cpwVcMplsInboundIfIndex = cpwVcMplsInboundEntry + ".7";

    public static final String cpwVcMplsNonTeMappingTunnelDirection = cpwVcMplsNonTeMappingEntry + ".1";
    public static final String cpwVcMplsNonTeMappingXcTunnelIndex = cpwVcMplsNonTeMappingEntry + ".2";
    public static final String cpwVcMplsNonTeMappingIfIndex = cpwVcMplsNonTeMappingEntry + ".3";
    public static final String cpwVcMplsNonTeMappingVcIndex = cpwVcMplsNonTeMappingEntry + ".4";

    public static final String cpwVcMplsTeMappingTunnelDirection = cpwVcMplsTeMappingEntry + ".1";
    public static final String cpwVcMplsTeMappingTunnelIndex = cpwVcMplsTeMappingEntry + ".2";
    public static final String cpwVcMplsTeMappingTunnelInstance = cpwVcMplsTeMappingEntry + ".3";
    public static final String cpwVcMplsTeMappingTunnelPeerLsrID = cpwVcMplsTeMappingEntry + ".4";
    public static final String cpwVcMplsTeMappingTunnelLocalLsrID = cpwVcMplsTeMappingEntry + ".5";
}