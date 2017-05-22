package voss.discovery.agent.alcatel.mib;

public interface TimetraMplsMib {

    public static final String tmnxMplsObjs = TimetraGlobalMib.tmnxSRObjs + ".6";

    public static final String vRtrMplsLspTable = tmnxMplsObjs + ".1";
    public static final String vRtrMplsLspEntry = vRtrMplsLspTable + ".1";
    public static final String vRtrMplsLspName = vRtrMplsLspEntry + ".4";
    public static final String vRtrMplsLspOperState = vRtrMplsLspEntry + ".6";
    public static final String vRtrMplsLspFromAddr = vRtrMplsLspEntry + ".7";
    public static final String vRtrMplsLspToAddr = vRtrMplsLspEntry + ".8";

    public static final String vRtrMplsLspStatTable = tmnxMplsObjs + ".2";
    public static final String vRtrMplsLspStatEntry = vRtrMplsLspStatTable + ".1";
    public static final String vRtrMplsLspConfiguredPaths = vRtrMplsLspStatEntry + ".11";
    public static final String vRtrMplsLspStandbyPaths = vRtrMplsLspStatEntry + ".12";
    public static final String vRtrMplsLspOperationalPaths = vRtrMplsLspStatEntry + ".13";

    public static final String vRtrMplsLspPathTable = tmnxMplsObjs + ".4";
    public static final String vRtrMplsLspPathEntry = vRtrMplsLspPathTable + ".1";
    public static final String vRtrMplsLspPathType = vRtrMplsLspPathEntry + ".3";
    public static final String vRtrMplsLspPathProperties = vRtrMplsLspPathEntry + ".5";
    public static final String vRtrMplsLspPathBandwidth = vRtrMplsLspPathEntry + ".6";
    public static final String vRtrMplsLspPathState = vRtrMplsLspPathEntry + ".8";
    public static final String vRtrMplsLspPathAdminState = vRtrMplsLspPathEntry + ".17";
    public static final String vRtrMplsLspPathOperState = vRtrMplsLspPathEntry + ".18";
    public static final String vRtrMplsLspPathTunnelARHopListIndex = vRtrMplsLspPathEntry + ".22";


    public enum VRtrMplsLspPathState {
        unknown(1),
        active(2),
        inactive(3),;

        private final int value;

        private VRtrMplsLspPathState(int value) {
            this.value = value;
        }

        public static VRtrMplsLspPathState get(int value) {
            for (VRtrMplsLspPathState type : VRtrMplsLspPathState.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalStateException();
        }
    }

    public enum VRtrMplsLspPathType {
        other(1),
        primary(2),
        standby(3),
        secondary(4),;

        private final int value;

        private VRtrMplsLspPathType(int value) {
            this.value = value;
        }

        public static VRtrMplsLspPathType get(int value) {
            for (VRtrMplsLspPathType type : VRtrMplsLspPathType.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalStateException();
        }
    }

}