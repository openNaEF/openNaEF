package naef.dto;

public abstract class IdRange<T extends Object & Comparable<T>> 
    implements Comparable<IdRange<T>>, java.io.Serializable 
{
    public static class Integer extends IdRange<java.lang.Integer> {

        public Integer(java.lang.Integer lowerBound, java.lang.Integer upperBound) {
            super(lowerBound, upperBound);
        }

        @Override public long getNumberOfIds() {
            return upperBound - lowerBound + 1;
        }
    }

    public static class Long extends IdRange<java.lang.Long> {

        public Long(java.lang.Long lowerBound, java.lang.Long upperBound) {
            super(lowerBound, upperBound);
        }

        @Override public long getNumberOfIds() {
            return upperBound - lowerBound + 1;
        }
    }

    public static class String extends IdRange<java.lang.String> {

        public String(java.lang.String lowerBound, java.lang.String upperBound) {
            super(lowerBound, upperBound);
        }

        @Override public long getNumberOfIds() {
            return java.lang.Long.MAX_VALUE;
        }
    }

    public final T lowerBound;
    public final T upperBound;

    public IdRange(T lowerBound, T upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    abstract public long getNumberOfIds();

    @Override public int compareTo(IdRange<T> another) {
        if (lowerBound.compareTo(another.lowerBound) < 0) {
            return -1;
        } else if (lowerBound.compareTo(another.lowerBound) > 0) {
            return 1;
        } else if (upperBound.compareTo(another.upperBound) < 0) {
            return -1;
        } else if (upperBound.compareTo(another.upperBound) > 0) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override public int hashCode() {
        return (lowerBound == null ? 0 : lowerBound.hashCode())
            + (upperBound == null ? 0 : upperBound.hashCode());
    }

    @Override public boolean equals(Object o) {
        if (o == null
            || o.getClass() != this.getClass())
        {
            return false;
        }

        IdRange<T> another = (IdRange<T>) o;
        return (this.lowerBound == null
                ? another.lowerBound == null
                : this.lowerBound.equals(another.lowerBound))
            && (this.upperBound == null
                ? another.upperBound == null
                : this.upperBound.equals(another.upperBound));
    }
}
