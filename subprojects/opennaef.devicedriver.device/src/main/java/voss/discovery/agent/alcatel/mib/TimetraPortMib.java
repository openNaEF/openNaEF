package voss.discovery.agent.alcatel.mib;

public interface TimetraPortMib {

    public static final String tmnxPortObjs = TimetraChassisMib.tmnxHwObjs + ".4";

    public static final String tmnxPortTable = tmnxPortObjs + ".2";
    public static final String tmnxPortEntry = tmnxPortTable + ".1";
    public static final String tmnxPortName = tmnxPortEntry + ".6";
    public static final String tmnxPortEncapType = tmnxPortEntry + ".12";
    public static final String tmnxPortConnectorType = tmnxPortEntry + ".17";
    public static final String tmnxPortState = tmnxPortEntry + ".39";
    public static final String tmnxPortParentPortID = tmnxPortEntry + ".54";

    public static final String tmnxPortConnectTypeTable = tmnxPortObjs + ".8";
    public static final String tmnxPortConnectTypeEntry = tmnxPortConnectTypeTable + ".1";
    public static final String tmnxPortConnectTypeName = tmnxPortConnectTypeEntry + ".2";

    public static final String tmnxDS0ChanGroupTable = tmnxPortObjs + ".13";
    public static final String tmnxDS0ChanGroupEntry = tmnxDS0ChanGroupTable + ".1";
    public static final String tmnxDS0ChanGroupTimeSlots = tmnxDS0ChanGroupEntry + ".2";
    public static final String tmnxDS0ChanGroupSpeed = tmnxDS0ChanGroupEntry + ".3";

    public enum TmnxPortState {
        none(1),
        ghost(2),
        linkDown(3),
        linkUp(4),
        up(5),
        diagnose(6),;

        private final int value;

        private TmnxPortState(int value) {
            this.value = value;
        }

        public static TmnxPortState get(int value) {
            for (TmnxPortState e : TmnxPortState.values()) {
                if (e.value == value) {
                    return e;
                }
            }
            throw new IllegalStateException();
        }
    }

    public enum TmnxPortClass {
        none(1),
        faste(2),
        gige(3),
        xgige(4),
        sonet(5),
        vport(6),
        unused(7),
        xcme(8),
        tdm(9)
        ;
        private final int value;

        private TmnxPortClass(int value) {
            this.value = value;
        }

        public static TmnxPortClass get(int value) {
            for (TmnxPortClass e : TmnxPortClass.values()) {
                if (e.value == value) {
                    return e;
                }
            }
            throw new IllegalStateException();
        }
    }

    public enum TmnxDS0ChanGroupSpeed {
        speed_56(1, 56000L),
        speed_64(2, 64000L);
        private final int value;
        private final long speed;

        private TmnxDS0ChanGroupSpeed(int value, long speed) {
            this.value = value;
            this.speed = speed;
        }

        public static TmnxDS0ChanGroupSpeed get(int value) {
            for (TmnxDS0ChanGroupSpeed e : TmnxDS0ChanGroupSpeed.values()) {
                if (e.value == value) {
                    return e;
                }
            }
            throw new IllegalStateException();
        }

        public long getSpeed() {
            return speed;
        }
    }

    public enum TmnxPortEncapType {
        unknown(0),
        nullEncap(1),
        qEncap(2),
        mplsEncap(3),
        bcpNullEncap(4),
        bcpDot1qEncap(5),
        ipcpEncap(6),
        frEncap(7),
        pppAutoEncap(8),
        atmEncap(9),
        qinqEncap(10),
        wanMirrorEncap(11),
        ciscoHDLCEncap(12),
        cemEncap(13);
        private final int value;

        TmnxPortEncapType(int value) {
            this.value = value;
        }

        public static TmnxPortEncapType get(int value) {
            for (TmnxPortEncapType e : TmnxPortEncapType.values()) {
                if (e.value == value) {
                    return e;
                }
            }
            throw new IllegalStateException();
        }
    }
}