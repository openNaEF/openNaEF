package voss.core.common.exception;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ExceptionHolder implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final Throwable th;
    private long time;
    private String processName;
    private String processIpAddress;
    private boolean deleted = false;

    public ExceptionHolder() {
        this.id = System.currentTimeMillis();
        this.th = null;
        this.time = System.currentTimeMillis();
    }

    public ExceptionHolder(final long id, final Throwable th) {
        this.id = id;
        this.th = th;
        this.time = System.currentTimeMillis();
    }

    public long getID() {
        return this.id;
    }

    public Throwable getThrowable() {
        return this.th;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(final String name) {
        processName = name;
    }

    public void setDateAsNow() {
        this.time = System.currentTimeMillis();
    }

    public Calendar getDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(this.time);
        return cal;
    }

    public void setProcessIpAddress(String ipAddress) {
        this.processIpAddress = ipAddress;
    }

    public String getProcessIpAddress() {
        return this.processIpAddress;
    }

    public void setDeleted() {
        this.deleted = true;
    }

    public void restore() {
        this.deleted = false;
    }

    public boolean isDeleted() {
        return this.deleted;
    }

    public String toString() {
        final SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss,SSS");
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());
        sb.append(":id=").append(id);
        sb.append(":sourceSystem=").append(this.processName);
        sb.append(":sourceIP=").append(this.processIpAddress);
        sb.append("\r\n");
        sb.append("\tsaved date=").append(df.format(new Date(this.time)));
        sb.append("\r\n");
        sb.append("\tsaved exception=").append(this.th.getMessage());
        sb.append("\r\n");
        sb.append("\tdeleted? ").append(this.deleted);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return (int) (this.id % Integer.MAX_VALUE) + 37;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o instanceof ExceptionHolder) {
            return this.id == ((ExceptionHolder) o).id;
        }
        return false;
    }
}