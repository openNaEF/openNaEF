package voss.discovery.agent.alcatel.mib;

public class TimetraTcMib {

    public enum TmnxAdminState {
        noop(1),
        inService(2),
        outOfService(3);
        private final int value;

        private TmnxAdminState(int value) {
            this.value = value;
        }

        public static TmnxAdminState get(int value) {
            for (TmnxAdminState e : TmnxAdminState.values()) {
                if (e.value == value) {
                    return e;
                }
            }
            throw new IllegalStateException();
        }
    }

    public enum TmnxOperState {
        unknown(1),
        inService(2),
        outOfService(3),
        transition(4),;

        private final int value;

        private TmnxOperState(int value) {
            this.value = value;
        }

        public static TmnxOperState get(int value) {
            for (TmnxOperState e : TmnxOperState.values()) {
                if (e.value == value) {
                    return e;
                }
            }
            throw new IllegalStateException();
        }
    }
}