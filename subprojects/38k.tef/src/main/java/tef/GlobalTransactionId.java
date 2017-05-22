package tef;

public final class GlobalTransactionId implements java.io.Serializable {

    public static GlobalTransactionId parse(String str) {
        if (!str.matches("[0-9]+-[0-9]+")) {
            return null;
        }

        long time = Long.parseLong(str.substring(0, str.indexOf("-")));
        int id = Integer.parseInt(str.substring(str.indexOf("-") + 1));
        return new GlobalTransactionId(id, time);
    }

    private static int lastId__ = 0;

    private long time_;
    private int id_;

    private GlobalTransactionId(int id, long time) {
        id_ = id;
        time_ = time;
    }

    GlobalTransactionId() {
        this(getNextId(), System.currentTimeMillis());
    }

    private static synchronized int getNextId() {
        int result = ++lastId__;
        lastId__ = result;
        return result;
    }

    public String getIdString() {
        return Long.toString(time_) + "-" + Integer.toString(id_);
    }

    public long time() {
        return time_;
    }

    public int id() {
        return id_;
    }

    @Override
    public int hashCode() {
        return id_;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null
                || getClass() != obj.getClass()) {
            return false;
        }

        GlobalTransactionId another = (GlobalTransactionId) obj;
        return time_ == another.time_
                && id_ == another.id_;
    }

    @Override
    public String toString() {
        return Integer.toString(id_);
    }
}
