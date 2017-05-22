package voss.discovery.agent.alcatel.mib;

public interface TimetraChassisMib {

    public static final String tmnxHwObjs = TimetraGlobalMib.tmnxSRObjs + ".2";

    public static final String tmnxChassisObjs = tmnxHwObjs + ".1";
    public static final String tmnxChassisTable = tmnxChassisObjs + ".3";
    public static final String tmnxChassisEntry = tmnxChassisTable + ".1";
    public static final String tmnxChassisType = tmnxChassisEntry + ".4";
    public static final String tmnxChassisTypeTable = tmnxChassisObjs + ".6";
    public static final String tmnxChassisTypeEntry = tmnxChassisTypeTable + ".1";
    public static final String tmnxChassisTypeName = tmnxChassisTypeEntry + ".2";

    public static final String tmnxHwTable = tmnxChassisObjs + ".8";
    public static final String tmnxHwEntry = tmnxHwTable + ".1";
    public static final String tmnxHwMfgBoardNumber = tmnxHwEntry + ".4";
    public static final String tmnxHwSerialNumber = tmnxHwEntry + ".5";
    public static final String tmnxHwClass = tmnxHwEntry + ".7";

    public static final String tmnxCardObjs = tmnxHwObjs + ".3";
    public static final String tmnxCardTable = tmnxCardObjs + ".2";
    public static final String tmnxCardEntry = tmnxCardTable + ".1";
    public static final String tmnxCardEquippedType = tmnxCardEntry + ".5";
    public static final String tmnxCardHwIndex = tmnxCardEntry + ".6";
    public static final String tmnxCpmCardTable = tmnxCardObjs + ".4";
    public static final String tmnxCpmCardEntry = tmnxCpmCardTable + ".1";
    public static final String tmnxCpmCardEquippedType = tmnxCpmCardEntry + ".6";
    public static final String tmnxCpmCardHwIndex = tmnxCpmCardEntry + ".7";

    public static final String tmnxMDATable = tmnxCardObjs + ".8";
    public static final String tmnxMDAEntry = tmnxMDATable + ".1";
    public static final String tmnxMDAEquippedType = tmnxMDAEntry + ".5";
    public static final String tmnxMDAHwIndex = tmnxMDAEntry + ".6";
    public static final String tmnxCardTypeTable = tmnxCardObjs + ".9";
    public static final String tmnxCardTypeEntry = tmnxCardTypeTable + ".1";
    public static final String tmnxCardTypeName = tmnxCardTypeEntry + ".2";
    public static final String tmnxCardTypeDescription = tmnxCardTypeEntry + ".3";

    public static final String tmnxMdaTypeTable = tmnxCardObjs + ".10";
    public static final String tmnxMdaTypeEntry = tmnxMdaTypeTable + ".1";
    public static final String tmnxMdaTypeName = tmnxMdaTypeEntry + ".2";
    public static final String tmnxMdaTypeDescription = tmnxMdaTypeEntry + ".3";

    public enum TmnxMDAChanType {
        unknown(0),
        sonetSts768(1),
        sonetSts192(2),
        sonetSts48(3),
        sonetSts12(4),
        sonetSts3(5),
        sonetSts1(6),
        sdhTug3(7),
        sonetVtg(8),
        sonetVt15(9),
        sonetVt2(10),
        sonetVt3(11),
        sonetVt6(12),
        pdhTu3(13),
        pdhDs3(14),
        pdhE3(15),
        pdhDs1(16),
        pdhE1(17),
        pdhDs0Grp(18);
        private final int value;

        private TmnxMDAChanType(int value) {
            this.value = value;
        }

        public static TmnxMDAChanType get(int value) {
            for (TmnxMDAChanType type : TmnxMDAChanType.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalStateException();
        }
    }
}