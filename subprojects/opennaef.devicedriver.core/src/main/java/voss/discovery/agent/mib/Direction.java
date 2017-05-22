package voss.discovery.agent.mib;

public enum Direction {
    unknown(-1), outbound(1), inbound(2);

    private int id;

    private Direction(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static Direction getById(int id) {
        for (Direction value : Direction.values()) {
            if (value.id == id) {
                return value;
            }
        }
        return Direction.unknown;
    }

}