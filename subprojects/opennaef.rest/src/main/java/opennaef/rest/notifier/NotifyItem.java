package opennaef.rest.notifier;

import tef.TransactionId;

import java.util.Date;

public class NotifyItem {
    private final TransactionId _tx;
    private final Date _time;
    private final boolean _done;

    public NotifyItem(TransactionId tx, Date time) {
        this(tx, time, false);
    }

    public NotifyItem(TransactionId tx, Date time, boolean done) {
        if (tx == null || time == null) {
            throw new IllegalArgumentException("tx or time is null.");
        }

        _tx = tx;
        _time = new Date(time.getTime());
        _done = done;
    }

    public TransactionId tx() {
        return _tx;
    }

    public Date time() {
        return new Date(_time.getTime());
    }

    public long timeMillis() {
        return _time.getTime();
    }

    public boolean isDone() {
        return _done;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (super.equals(obj)) return true;
        if (this.getClass() != obj.getClass()) return false;

        NotifyItem o = (NotifyItem) obj;
        if (!this.tx().equals(o.tx())) return false;
        if (!this.time().equals(o.time())) return false;
        if (!this.isDone() ^ !o.isDone()) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = 7873;
        result = 31 * result + tx().serial;
        result = 31 * result + time().hashCode();
        result = 31 * result + Boolean.hashCode(isDone());
        return result;
    }
}
