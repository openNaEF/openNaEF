package voss.discovery.agent.alcatel.mib;

public interface TimetraServMib {

    public static final String tmnxServObjs = TimetraGlobalMib.tmnxSRObjs + ".4";
    public static final String tmnxSvcObjs = tmnxServObjs + ".2";
    public static final String svcBaseInfoTable = tmnxSvcObjs + ".2";
    public static final String svcBaseInfoEntry = svcBaseInfoTable + ".1";

    public static final String svcId = svcBaseInfoEntry + ".1";
    public static final String svcType = svcBaseInfoEntry + ".3";
    public static final String svcDescription = svcBaseInfoEntry + ".6";
    public static final String svcAdminStatus = svcBaseInfoEntry + ".8";
    public static final String svcOperStatus = svcBaseInfoEntry + ".9";
    public enum SvcType {
        unknown(0),
        epipe(1),
        p3pipe(2),
        tls(3),
        vprn(4),
        ies(5),
        mirror(6),
        apipe(7),
        fpipe(8),
        ipipe(9),
        cpipe(10),;

        private final int value;

        private SvcType(int value) {
            this.value = value;
        }

        public static SvcType get(int value) {
            for (SvcType type : SvcType.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return unknown;
        }
    }
}