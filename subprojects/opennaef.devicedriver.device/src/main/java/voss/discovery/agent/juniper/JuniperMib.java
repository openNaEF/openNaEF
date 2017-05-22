package voss.discovery.agent.juniper;

public interface JuniperMib {

    public static final String jnxBoxAnatomy = JuniperSmi.jnxMibs + ".1";

    public static final String jnxBoxDescr = jnxBoxAnatomy + ".2";
    public static final String jnxBoxSerialNo = jnxBoxAnatomy + ".3";
    public static final String jnxContentsTable = jnxBoxAnatomy + ".8";
    public static final String jnxContentsEntry = jnxContentsTable + ".1";
    public static final String jnxContentsDescr = jnxContentsEntry + ".6";
    public static final String jnxContentsSerialNo = jnxContentsEntry + ".7";
    public static final String jnxContentsRevision = jnxContentsEntry + ".8";
    public static final String jnxContentsPartNo = jnxContentsEntry + ".10";
    public static final String jnxFruTable = jnxBoxAnatomy + ".15";
    public static final String jnxFruEntry = jnxFruTable + ".1";
    public static final String jnxFruName = jnxFruEntry + ".5";
    public static final String jnxFruType = jnxFruEntry + ".6";
    public static final String jnxFruState = jnxFruEntry + ".8";


    enum JnxFruState {
        unknown(1),
        empty(2),
        present(3),
        ready(4),
        announceOnline(5),
        online(6),
        anounceOffline(7),
        offline(8),
        diagnostic(9),
        standby(10);

        private final int value;

        JnxFruState(int value) {
            this.value = value;
        }

        public static JnxFruState get(int value) {
            for (JnxFruState jnxFruState : JnxFruState.values()) {
                if (jnxFruState.value == value) {
                    return jnxFruState;
                }
            }
            return null;
        }
    }

    enum JnxFruType {
        other(1),
        clockGenerator(2),
        flexiblePicConcentrator(3),
        switchingAndForwardingModule(4),
        controlBoard(5),
        routingEngine(6),
        powerEntryModule(7),
        frontPanelModule(8),
        switchInterfaceBoard(9),
        processorMezzanineBoardForSIB(10),
        portInterfaceCard(11),
        craftInterfacePanel(12),
        fan(13),
        lineCardChassis(14),
        forwardingEngineBoard(15),
        protectedSystemDomain(16)
        ;

        private final int value;

        JnxFruType(int value) {
            this.value = value;
        }

        public static JnxFruType get(int value) {
            for (JnxFruType jnxFruType : JnxFruType.values()) {
                if (jnxFruType.value == value) {
                    return jnxFruType;
                }
            }
            return null;
        }
    }
}