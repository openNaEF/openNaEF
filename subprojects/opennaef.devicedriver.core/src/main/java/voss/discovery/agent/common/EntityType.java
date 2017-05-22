package voss.discovery.agent.common;

public enum EntityType {
    UNDEFINED(-1), OTHER(1), UNKNOWN(2), CHASSIS(3), BACKPLANE(4), CONTAINER(5), POWERSUPPLY(
            6), FAN(7), SENSOR(8), MODULE(9), PORT(10), STACK(11);

    private int id;

    private EntityType(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static EntityType valueOf(int id) {
        for (EntityType value : EntityType.values()) {
            if (value.id == id) {
                return value;
            }
        }
        return UNDEFINED;
    }
}