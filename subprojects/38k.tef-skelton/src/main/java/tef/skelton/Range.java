package tef.skelton;

import tef.MVO;
import tef.MvoHome;

public abstract class Range<T extends Object & Comparable<T>> extends MVO {

    public static final java.lang.String[] tokenizeRangeStr(java.lang.String rangeStr) {
        java.lang.String[] rangeTokens = rangeStr.split("-");
        if (rangeTokens.length != 2) {
            throw new FormatException("範囲の形式を確認してください.");
        }
        return rangeTokens;
    }

    public static final <T extends Comparable<T>> void checkOrder(T lower, T upper) {
        if (lower.compareTo(upper) > 0) {
            throw new FormatException("下限と上限の順序を確認してください.");
        }
    }

    public static final <T extends Comparable<T>> T selectLower(T t1, T t2) {
        return t1.compareTo(t2) <= 0 ? t1 : t2;
    }

    public static final <T extends Comparable<T>> T selectUpper(T t1, T t2) {
        return t1.compareTo(t2) <= 0 ? t2 : t1;
    }

    public static final class Integer extends Range<java.lang.Integer> {

        public static Range.Integer gainByRangeStr(java.lang.String rangeStr)
            throws FormatException 
        {
            java.lang.String[] rangeTokens = tokenizeRangeStr(rangeStr);
            java.lang.Integer lowerBound = ValueResolver.parseInteger(rangeTokens[0], false);
            java.lang.Integer upperBound = ValueResolver.parseInteger(rangeTokens[1], false);
            checkOrder(lowerBound, upperBound);
            return gain(lowerBound, upperBound);
        }

        public static Range.Integer gain(java.lang.Integer lowerBound, java.lang.Integer upperBound) {
            for (Range.Integer instance : home.list()) {
                if (instance.getLowerBound().equals(lowerBound)
                    && instance.getUpperBound().equals(upperBound))
                {
                    return instance;
                }
            }
            return new Range.Integer(lowerBound, upperBound);
        }

        public static final MvoHome<Range.Integer> home = new MvoHome<Range.Integer>(Range.Integer.class);

        private Integer(MvoId id) {
            super(id);
        }

        private Integer(java.lang.Integer lowerBound, java.lang.Integer upperBound) {
            super(lowerBound, upperBound);
        }

        @Override public Range.Integer newInstance(java.lang.Integer lowerBound, java.lang.Integer upperBound) {
            return gain(lowerBound, upperBound);
        }
    }

    public static final class Long extends Range<java.lang.Long> {

        public static Range.Long gainByRangeStr(java.lang.String rangeStr)
            throws FormatException 
        {
            java.lang.String[] rangeTokens = tokenizeRangeStr(rangeStr);
            java.lang.Long lowerBound = ValueResolver.parseLong(rangeTokens[0], false);
            java.lang.Long upperBound = ValueResolver.parseLong(rangeTokens[1], false);
            checkOrder(lowerBound, upperBound);
            return gain(lowerBound, upperBound);
        }

        public static Range.Long gain(java.lang.Long lowerBound, java.lang.Long upperBound) {
            for (Range.Long instance : home.list()) {
                if (instance.getLowerBound().equals(lowerBound)
                    && instance.getUpperBound().equals(upperBound))
                {
                    return instance;
                }
            }
            return new Range.Long(lowerBound, upperBound);
        }

        public static final MvoHome<Range.Long> home = new MvoHome<Range.Long>(Range.Long.class);

        private Long(MvoId id) {
            super(id);
        }

        private Long(java.lang.Long lowerBound, java.lang.Long upperBound) {
            super(lowerBound, upperBound);
        }

        @Override public Range.Long newInstance(java.lang.Long lowerBound, java.lang.Long upperBound) {
            return gain(lowerBound, upperBound);
        }
    }

    public static final class String extends Range<java.lang.String> {

        public static Range.String gainByRangeStr(java.lang.String rangeStr)
            throws FormatException 
        {
            java.lang.String[] rangeTokens = tokenizeRangeStr(rangeStr);
            java.lang.String lowerBound = rangeTokens[0];
            java.lang.String upperBound = rangeTokens[1];
            checkOrder(lowerBound, upperBound);
            return gain(lowerBound, upperBound);
        }

        public static Range.String gain(java.lang.String lowerBound, java.lang.String upperBound) {
            for (Range.String instance : home.list()) {
                if (instance.getLowerBound().equals(lowerBound)
                    && instance.getUpperBound().equals(upperBound))
                {
                    return instance;
                }
            }
            return new Range.String(lowerBound, upperBound);
        }

        public static final MvoHome<Range.String> home = new MvoHome<Range.String>(Range.String.class);

        private String(MvoId id) {
            super(id);
        }

        private String(java.lang.String lowerBound, java.lang.String upperBound) {
            super(lowerBound, upperBound);
        }

        @Override public Range.String newInstance(java.lang.String lowerBound, java.lang.String upperBound) {
            return gain(lowerBound, upperBound);
        }
    }

    private final F0<T> lowerBound_ = new F0<T>();
    private final F0<T> upperBound_ = new F0<T>();

    protected Range(MvoId id) {
        super(id);
    }

    protected Range(T lowerBound, T upperBound) {
        if (lowerBound == null || upperBound == null) {
            throw new IllegalArgumentException();
        }
        if (lowerBound.compareTo(upperBound) > 0) {
            throw new IllegalArgumentException();
        }

        lowerBound_.initialize(lowerBound);
        upperBound_.initialize(upperBound);
    }

    public T getLowerBound() {
        return lowerBound_.get();
    }

    public T getUpperBound() {
        return upperBound_.get();
    }

    public boolean isOverlap(T lowerBound, T upperBound) {
        return ! (getUpperBound().compareTo(lowerBound) < 0
                  || getLowerBound().compareTo(upperBound) > 0);
    }

    public boolean contains(T lowerBound, T upperBound) {
        return getLowerBound().compareTo(lowerBound) <= 0
            && getUpperBound().compareTo(upperBound) >= 0;
    }

    public boolean contains(Range<T> another) {
        return contains(another.getLowerBound(), another.getUpperBound());
    }

    public java.lang.String getRangeStr() {
        return getLowerBound().toString() + "-" + getUpperBound().toString();
    }

    public abstract Range<T> newInstance(T lowerBound, T upperBound);
}
