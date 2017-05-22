package voss.discovery.agent.mib;

public enum PhysicalClass {
    other(1),
    unknown(2),
    chassis(3),
    backplane(4),
    container(5),
    powerSupply(6),
    fan(7),
    sensor(8),
    module(9),
    port(10),
    stack(11),
    cpu(12),;

    private final int index;

    private PhysicalClass(int index) {
        this.index = index;
    }

    public static PhysicalClass getByIndex(int index) {
        for (PhysicalClass value : values()) {
            if (value.index == index) {
                return value;
            }
        }
        return null;
    }
}