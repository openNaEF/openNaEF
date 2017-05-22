package voss.discovery.agent.common;

public enum OSType {
    IOS("IOS"),
    IOS_XR("IOS XR"),
    CATOS("Catalyst OS"),

    NX_OS("NX-OS"),

    ALCATEL_OS("TiMOS"),

    JUNOS("JUNOS"),
    SCREENOS("ScreenOS"),

    EXTREMEWARE("ExtremeWare"),
    EXTREMEXOS("ExtemreXOS"),

    F5_TMOS("F5 TMOS"),

    FORTIGATE("FortiOS"),

    FOUNDRY("Foundry"),

    FRASHWAVE("FlashWaveOS"),

    ALAXALA_CLI("AX-CLI"),
    ALAXALA_AX("AX-AX"),

    APWARE("ApWare"),
    APBWARE("ApbWare"),
    AMIOS("AMI OS"),
    AEOS("AEOS"),

    PICO("Portable Internetwork Core Operating System"),

    EXAOS("EXA OS"),
    SHOS("SH"),

    VMware_ESX("VMware_ESX"),
    vSwitchOS("vSwitchOS"),

    BLADENETWORKOS("BladeNetworkOS"),

    OS("OS"),

    NULL(""),

    UNKNOWN("Unknown"),;
    public final String caption;

    private OSType(String name) {
        this.caption = name;
    }

    public static OSType getByCaption(String s) {
        for (OSType instance : values()) {
            if (instance.caption.equals(s)) {
                return instance;
            }
        }
        return UNKNOWN;
    }
}