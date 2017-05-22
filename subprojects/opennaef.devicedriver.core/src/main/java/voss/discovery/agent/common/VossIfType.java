package voss.discovery.agent.common;


public enum VossIfType {
    UNDEFINED(-1, "UNDEFINED"),
    OTHER(0, "Other"),
    NOCONNECTOR(1, "No Connector"),

    E10AUI(101, "AUI"),
    E10BASET(111, "10BASE-T"),
    E10BASEF(112, "10BASE-F"),
    E10BASE2(113, "10BASE-2"),
    E10BASE5(114, "10BASE-5"),
    E10FOIRL(115, "Foirl"),
    E10BASEFP(116, "10BASE-FP"),
    E10BASEFB(117, "10BASE-FB"),
    E10BASEFL(118, "10BASE-FL"),

    E100BASEMII(201, "MII"),
    E10100BASETX(211, "10/100BASE-TX"),
    E100BASETX(212, "100BASE-TX"),
    E100BASEFX(213, "100BASE-FX"),
    E100BASET4(214, "100BASE-T4"),
    E100BASEBX10D(215, "100BASE-BX10D"),
    E100BASEBX10U(216, "100BASE-BX10U"),
    E100BASESX(217, "100BASE-SX"),
    E100BASET2(218, "100BASE-T2"),

    E1000GBIC(301, "GBIC"),
    E1000SFP(302, "SFP"),
    E1000BASESX(311, "1000BASE-SX"),
    E1000BASELX(312, "1000BASE-LX"),
    E1000BASELH(313, "1000BASE-LH"),
    E1000BASECX(314, "1000BASE-CX"),
    E1000BASET(315, "1000BASE-T"),
    E1000BASEZX(316, "1000BAE-ZX"),
    E101001000BASET(317, "10/100/1000BASE-T"),
    E1000BASECWDM(318, "1000BASE-CWDM"),
    E1000BASEBT(319, "1000BASE-BT"),
    E1000BASEBX10D(320, "1000BASE-BX10D"),
    E1000BASEBX10U(321, "1000BASE-BX10U"),
    E1000BASEX(322, "1000BASE-X"),
    E1000BASEFX(323, "1000BASE-FX"),

    E10GXFP(401, "XFP"),
    E10GBASELR(411, "10GBASE-LR"),
    E10GBASESR(412, "10GBASE-SR"),
    E10GBASEXR(413, "10GBASE-XR"),
    E10GBASESX4(414, "10GBASE-SX4"),
    E10GBASELX4(415, "10GBASE-LX4"),
    E10GBASEEX4(416, "10GBASE-EX4"),
    E10GBASECX4(417, "10GBASE-CX4"),
    E10GBASESW(418, "10GBASE-SW"),
    E10GBASELW(419, "10GBASE-LW"),
    E10GBASEEW(420, "10GBASE-EW"),
    E10GBASEWDM(421, "10GBASE-WDM"),
    E10GBASEDWDM(422, "10GBASE-DWDM"),
    E10GBASEEX(423, "10GBASE-EX"),
    E10GBASEZX(424, "10GBASE-ZX"),
    E10GBASEZR(425, "10GBASE-ZR"),
    E10GBASEFX(426, "10GBASE-FX"),

    ATM(1001, "ATM"),
    ATM_OC3(1002, "ATM OC3"),
    ATM_OC12(1003, "ATM OC12"),
    ATM_OC48(1004, "ATM OC48"),

    POS(1101, "POS"),
    POS_OC12(1102, "POS OC12"),
    POS_OC48(1102, "POS OC48"),
    POS_OC192(1102, "POS OC192"),

    SERIAL(1201, "Serial"),
    E1(1202, "E1"),
    T1(1203, "T1"),
    T2(1202, "T2"),
    T3(1205, "T3"),

    TUNNEL(1301, "Tunnel"),

    MPLS(1401, "MPLS"),

    E10BROAD36(2001, "10BROAD-36"),;

    private final int id;
    private final String value;

    private VossIfType(int id) {
        this.id = id;
        this.value = null;
    }

    private VossIfType(int id, String value) {
        this.id = id;
        this.value = value;
    }

    public int getId() {
        return this.id;
    }

    public String getValue() {
        if (value == null) {
            return this.toString();
        }
        return this.value;
    }

    public static VossIfType valueOf(int id) {
        for (VossIfType value : VossIfType.values()) {
            if (value.id == id) {
                return value;
            }
        }
        return UNDEFINED;
    }
}