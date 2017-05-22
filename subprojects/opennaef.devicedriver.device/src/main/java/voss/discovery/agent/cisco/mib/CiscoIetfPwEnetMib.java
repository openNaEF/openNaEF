package voss.discovery.agent.cisco.mib;

public interface CiscoIetfPwEnetMib {
    public static final String CISCO_IETF_PW_ENET_MIB_BASE = ".1.3.6.1.4.1.9.10.108";

    public static final String cpwVcEnetObjects = CISCO_IETF_PW_ENET_MIB_BASE + ".1";
    public static final String cpwVcEnetTable = cpwVcEnetObjects + ".1";

    public static final String cpwVcEnetEntry = cpwVcEnetTable + ".1";

    public static final String cpwVcEnetVlanMode = cpwVcEnetEntry + ".2";
    public static final String cpwVcEnetPortVlan = cpwVcEnetEntry + ".3";
    public static final String cpwVcEnetVcIfIndex = cpwVcEnetEntry + ".4";
    public static final String cpwVcEnetPortIfIndex = cpwVcEnetEntry + ".5";

}