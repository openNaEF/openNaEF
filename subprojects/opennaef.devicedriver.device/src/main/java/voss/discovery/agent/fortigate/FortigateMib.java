package voss.discovery.agent.fortigate;

public class FortigateMib {

    public static final String fortinetMib = ".1.3.6.1.4.1.12356";

    public static final String fnSystem = fortinetMib + ".1";
    public static final String fnSysHaMode = fnSystem + ".6.0";
    public static final String fnSysOpMode = fnSystem + ".7.0";
    public static final String fnHa = fnSystem + ".100";
    public static final String fnHaGroupId = fnHa + ".1.0";
    public static final String fnHaPriority = fnHa + ".2.0";
    public static final String fnHaOverride = fnHa + ".3.0";
    public static final String fnHaAutoSync = fnHa + ".4.0";
    public static final String fnHaSchedule = fnHa + ".5.0";
    public static final String fnHaStatsTable = fnHa + ".6.0";
    public static final String fnHaGroupName = fnHa + ".7.0";
    public static final String fnHaTrapMemberSerial = fnHa + ".8.0";

    public static final String fnVdTable = fortinetMib + ".2.2";
    public static final String fnVdEntry = fnVdTable + ".1";
    public static final String fnVdEntIndex = fnVdEntry + ".1";
    public static final String fnVdEntName = fnVdEntry + ".2";
    public static final String fnVdEntOpMode = fnVdEntry + ".3";

    public static final String fnIntfTable = fortinetMib + ".3.1";
    public static final String fnIntfEntry = fnIntfTable + ".1";
    public static final String fnIntfEntVdom = fnIntfEntry + ".1";

    public static enum FortigateHaSync {
        disabled(1), enabled(2);
        private final int mode;

        private FortigateHaSync(int mode) {
            this.mode = mode;
        }

        public int getMode() {
            return this.mode;
        }

        public static FortigateHaSync getByMode(int mode) {
            for (FortigateHaSync value : values()) {
                if (value.mode == mode) {
                    return value;
                }
            }
            return null;
        }
    }

    public static enum FortigateHaSchedule {
        none(1), hub(2), leastConnections(3), roundRobin(4), weightedRoundRobin(5), random(6), ipBased(7), ipPortBased(8);
        private final int mode;

        private FortigateHaSchedule(int mode) {
            this.mode = mode;
        }

        public int getMode() {
            return this.mode;
        }

        public static FortigateHaSchedule getByMode(int mode) {
            for (FortigateHaSchedule value : values()) {
                if (value.mode == mode) {
                    return value;
                }
            }
            return null;
        }
    }

    public static enum FortigateHaMode {
        STANDALONE(1),
        ACTIVE_ACTIVE(2),
        ACTIVE_PASSIVE(3),;
        private final int mode;

        private FortigateHaMode(int mode) {
            this.mode = mode;
        }

        public int getMode() {
            return this.mode;
        }

        public static FortigateHaMode getByMode(int mode) {
            for (FortigateHaMode value : values()) {
                if (value.mode == mode) {
                    return value;
                }
            }
            return null;
        }
    }

    public static final String fnFortiGateMib = fortinetMib + ".101";
    public static final String fgVirtualDomain = fnFortiGateMib + ".3";
    public static final String fgVdTables = fgVirtualDomain + ".2";
    public static final String fgVdTable = fgVdTables + ".1";
    public static final String fgVdEntry = fgVdTable + ".1";
    public static final String fgVdEntName = fgVdEntry + ".2";

    public static final String fgIntf = fnFortiGateMib + ".7";
    public static final String fgIntfTables = fgIntf + ".2";
    public static final String fgIntfTable = fgIntfTables + ".1";
    public static final String fgIntfEntry = fgIntfTable + ".1";
    public static final String fgIntfEntVdom = fgIntfEntry + ".1";
}