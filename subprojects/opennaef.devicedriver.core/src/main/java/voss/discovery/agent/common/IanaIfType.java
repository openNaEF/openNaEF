package voss.discovery.agent.common;

public enum IanaIfType {
    undefined(-1, "UNDEFINED"),
    other(1, "Other"),
    regular1822(2),
    hdh1822(3),
    ddnX25(4, "X.25"),
    rfc877x25(5, "X.25(RFC877)"),
    ethernetCsmacd(6, "Ethernet(DIX)"),
    iso88023Csmacd(7, "Ethernet802.3"),
    iso88024TokenBus(8, "TokenBus"),
    iso88025TokenRing(9, "TokenRing"),
    iso88026Man(10),
    starLan(11, "StarLAN"),
    proteon10Mbit(12),
    proteon80Mbit(13),
    hyperchannel(14),
    fddi(15, "FDDI"),
    lapb(16, "LAP-B"),
    sdlc(17, "SDLC"),
    ds1(18, "DS1"),
    e1(19, "E1"),
    basicISDN(20, "ISDN(BRI)"),
    primaryISDN(21, "ISDN(PRI)"),
    propPointToPointSerial(22, "Serial"),
    ppp(23, "PPP"),
    softwareLoopback(24),
    eon(25),
    ethernet3Mbit(26, "XNS"),
    nsip(27),
    slip(28, "SLIP"),
    ultra(29),
    ds3(30, "DS3"),
    sip(31, "SMDS"),
    frameRelay(32, "FrameRelay"),
    rs232(33, "RS232C"),
    para(34),
    arcnet(35, "ARCNET"),
    arcnetPlus(36, "ARCNET+"),
    atm(37, "ATM"),
    miox25(38),
    sonet(39, "SONET"),
    x25ple(40),
    iso88022llc(41, "802.2LLC"),
    localTalk(42, "LocalTalk"),
    smdsDxi(43),
    frameRelayService(44),
    v35(45, "V.35"),
    hssi(46, "HSSI"),
    hippi(47, "HIPPI"),
    modem(48, "Modem"),
    aal5(49, "ATM AAL5"),
    sonetPath(50),
    sonetVT(51),
    smdsIcip(52),
    propVirtual(53, "Virtual"),
    propMultiplexor(54, "Multiplexor"),
    ieee80212(55, "100BASE-VG"),
    fibreChannel(56, "Fibre Channel"),
    hippiInterface(57, "HIPPI Interface"),
    frameRelayInterconnect(58),
    aflane8023(59),
    aflane8025(60),
    cctEmul(61),
    fastEther(62, "100BASE-TX"),
    isdn(63, "ISDN"),
    v11(64),
    v36(65),
    g703at64k(66),
    g703at2mb(67),
    qllc(68),
    fastEtherFX(69, "100BASE-FX"),
    channel(70),
    ieee80211(71, "802.11 Wireless LAN"),
    ibm370parChan(72),
    escon(73, "ESCON"),
    dlsw(74, "DLSW"),
    isdns(75),
    isdnu(76),
    lapd(77, "LAP-D"),
    ipSwitch(78),
    rsrb(79),
    atmLogical(80, "ATM Logical"),
    ds0(81, "DS0"),
    ds0Bundle(82),
    bsc(83),
    async(84),
    cnr(85),
    iso88025Dtr(86),
    eplrs(87),
    arap(88),
    propCnls(89),
    hostPad(90),
    termPad(91),
    frameRelayMPI(92),
    x213(93),
    adsl(94, "ADSL"),
    radsl(95, "RADSL"),
    sdsl(96, "SDSL"),
    vdsl(97, "VDSL"),
    iso88025CRFPInt(98),
    myrinet(99),
    voiceEM(100, "Voice EM"),
    voiceFXO(101, "FXO"),
    voiceFXS(102, "FXS"),
    voiceEncap(103),
    voiceOverIp(104, "VoIP"),
    atmDxi(105, "ATM DXI"),
    atmFuni(106, "ATM FUNI"),
    atmIma(107, "ATM IMA"),
    pppMultilinkBundle(108, "Multilink PPP"),
    ipOverCdlc(109),
    ipOverClaw(110),
    stackToStack(111),
    virtualIpAddress(112),
    mpc(113),
    ipOverAtm(114),
    iso88025Fiber(115),
    tdlc(116),
    gigabitEthernet(117, "1000BASE-X"),
    hdlc(118, "HDLC"),
    lapf(119, "LAP-F"),
    v37(120),
    x25mlp(121),
    x25huntGroup(122),
    trasnpHdlc(123),
    interleave(124),
    fast(125),
    ip(126, "IP"),
    docsCableMaclayer(127),
    docsCableDownstream(128),
    docsCableUpstream(129),
    a12MppSwitch(130),
    tunnel(131, "Tunnel"),
    coffee(132),
    ces(133, "ATM CES"),
    atmSubInterface(134, "ATM Sub"),
    l2vlan(135, "VLAN"),
    l3ipvlan(136),
    l3ipxvlan(137),
    digitalPowerline(138),
    mediaMailOverIp(139),
    dtm(140),
    dcn(141),
    ipForward(142),
    msdsl(143),
    ieee1394(144, "IEEE1394"),
    if_gsn(145, "IF-GSN"),
    dvbRccMacLayer(146),
    dvbRccDownstream(147),
    dvbRccUpstream(148),
    atmVirtual(149, "ATM Virtual"),
    mplsTunnel(150, "MPLS Tunnel"),
    srp(151),
    voiceOverAtm(152),
    voiceOverFrameRelay(153),
    idsl(154),
    compositeLink(155),
    ss7SigLink(156),
    propWirelessP2P(157),
    frForward(158),
    rfc1483(159, "MPoA"),
    usb(160, "USB"),
    ieee8023adLag(161, "IEEE802.3 LAG"),
    bgppolicyaccounting(162),
    frf16MfrBundle(163),
    h323Gatekeeper(164),
    h323Proxy(165),
    mpls(166, "MPLS"),
    mfSigLink(167),
    hdsl2(168, "HDSL2"),
    shdsl(169, "SHDSL"),
    ds1FDL(170),
    pos(171, "POS"),
    dvbAsiln(172),
    dvbAsiOut(173),
    plc(174),
    nfas(175),
    tr008(176),
    gr303RDT(177),
    gr303IDT(178),
    isup(179),
    propDocsWirelessMaclayer(180),
    propDocsWirelessDownstream(181),
    propDocsWirelessUpstream(182),
    hiperlan2(183),
    propBWAp2Mp(184),
    sonetOverheadChannel(185),
    digitalWrapperOverheadChannel(186),
    aal2(187, "ATM AAL2"),
    radioMAC(188),
    atmRadio(189),
    imt(190),
    mvl(191),
    reachDSL(192),
    frDlciEndPt(193),
    atmVciEndPt(194),
    opticalChannel(195),
    opticalTransport(196)
    ;

    private int id;
    private String name;

    private IanaIfType(int id) {
        this.id = id;
    }

    private IanaIfType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    public boolean is(String... names) {
        for (String name : names) {
            if (this.name().toLowerCase().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFamilyOf(String... names) {
        for (String name : names) {
            if (this.name().toLowerCase().startsWith(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        if (this.name != null) {
            return this.name;
        }
        return super.toString();
    }

    public static IanaIfType valueOf(int id) {
        for (IanaIfType value : IanaIfType.values()) {
            if (value.id == id) {
                return value;
            }
        }
        return undefined;
    }
}