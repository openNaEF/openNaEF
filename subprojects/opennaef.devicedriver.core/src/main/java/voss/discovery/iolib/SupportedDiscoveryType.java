package voss.discovery.iolib;

public enum SupportedDiscoveryType {
    Unknown,
    NOT_SUPPORTED,

    CiscoIosRouter,
    Cisco12000GsrRouter,
    CiscoIosSwitch,
    CiscoCatalystDiscovery,
    CiscoCatalyst2900XLDiscovery,
    CiscoCatalyst3750Discovery,
    CiscoCatalyst4500Discovery,

    CiscoNexusDiscovery,

    Alaxala1230SDiscovery,
    Alaxala2430SDiscovery,
    Alaxala3630SDiscovery,
    Alaxala5400SDiscovery,
    Alaxala6300SDiscovery,
    Alaxala6700SDiscovery,
    Alaxala7800SDiscovery,

    ApresiaApwareDiscovery,
    ApresiaAmiosDiscovery,
    ApresiaAeosDiscovery,

    BladeNetworkSwitchDiscovery,

    JuniperJunosRouter,
    NetscreenSSGChassisRouter,
    NetscreenSSGBoxRouter,

    FlashWave5740Discovery,
    FlashWave5540Discovery,
    FlashWave5530Discovery,

    FortigateDiscovery,

    ExtremeGenericDiscovery,
    ExtremeBD10800Discovery,

    FoundryGenericDiscovery,
    FoundryMG8Discovery,

    NecIx3000Discovery,

    SiiExatrax,
    SiiExatrax_SNMP,
    NecSuperHub,

    Alcatel7710SRDiscovery,

    F5BigIpDiscovery,

    TestSshDiscovery,

    Mib2Generic,

    AdvaWDMDiscovery,;

}