package tef;

/**
 * <p>
 * 日付と時刻をミリ秒精度で表現するクラスです。{@link MVO.MvoField} へ代入可能です。このクラスは
 * <code>java.util.Date</code> が可変オブジェクト(mutable)であることによって生じる問題を避ける
 * ために作成されたもので、<code>java.util.Date</code> との相互変換メソッドが用意されています。
 */
public class DateTime implements java.lang.Comparable<DateTime>, java.io.Serializable {

    public final long value;

    public DateTime(long value) {
        this.value = value;
    }

    public final long getValue() {
        return value;
    }

    public java.util.Date toJavaDate() {
        return new java.util.Date(value);
    }

    public static java.util.Date toJavaDate(DateTime datetime) {
        return datetime == null ? null : datetime.toJavaDate();
    }

    public static DateTime valueOf(java.util.Date javaDate) {
        return javaDate == null ? null : valueOf(javaDate.getTime());
    }

    public static DateTime valueOf(Long rawValue) {
        return rawValue == null ? null : valueOf(rawValue.longValue());
    }

    public static DateTime valueOf(long rawValue) {
        return new DateTime(rawValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        return obj instanceof DateTime
                && ((DateTime) obj).value == value;
    }

    @Override
    public int hashCode() {
        return (int) (value ^ (value >>> 32));
    }

    @Override
    public int compareTo(DateTime another) {
        return this.value < another.value ? -1 : (this.value == another.value ? 0 : 1);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
