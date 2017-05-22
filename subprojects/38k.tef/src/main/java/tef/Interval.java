package tef;

/**
 * 区間を表すオブジェクトです。
 */
public class Interval implements java.io.Serializable {

    public final double leftBound;
    public final boolean isLeftClosed;

    public final double rightBound;
    public final boolean isRightClosed;

    public Interval
            (double leftBound, boolean isLeftClosed, double rightBound, boolean isRightClosed) {
        if (rightBound < leftBound) {
            throw new IllegalArgumentException();
        }

        if ((leftBound == rightBound) && (isLeftClosed != isRightClosed)) {
            throw new IllegalArgumentException();
        }

        this.leftBound = leftBound;
        this.isLeftClosed = isLeftClosed;
        this.rightBound = rightBound;
        this.isRightClosed = isRightClosed;
    }

    public boolean isWithin(double value) {
        return (this.isLeftClosed
                ? this.leftBound <= value
                : this.leftBound < value)
                && (this.isRightClosed
                ? value <= this.rightBound
                : value < this.rightBound);
    }

    public boolean isWithin(Interval another) {
        return (this.leftBound < another.leftBound
                || (this.leftBound == another.leftBound
                && (this.isLeftClose() || another.isLeftOpen())))
                && (another.rightBound < this.rightBound
                || (another.rightBound == this.rightBound
                && (this.isRightClose() || another.isRightOpen())));
    }

    public boolean isLeftClose() {
        return isLeftClosed;
    }

    public boolean isLeftOpen() {
        return !isLeftClosed;
    }

    public boolean isRightClose() {
        return isRightClosed;
    }

    public boolean isRightOpen() {
        return !isRightClosed;
    }

    public boolean isDisjoint(Interval another) {
        if (another.rightBound < this.leftBound) {
            return true;
        }
        if (another.rightBound == this.leftBound) {
            return !(another.isRightClosed && this.isLeftClosed);
        }

        if (this.rightBound < another.leftBound) {
            return true;
        }
        if (this.rightBound == another.leftBound) {
            return !(this.isRightClosed && another.isLeftClosed);
        }

        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof Interval)) {
            return false;
        }

        Interval another = (Interval) obj;
        return this.leftBound == another.leftBound
                && this.isLeftClosed == another.isLeftClosed
                && this.rightBound == another.rightBound
                && this.isRightClosed == another.isRightClosed;
    }

    @Override
    public int hashCode() {
        long x
                = Double.doubleToRawLongBits(leftBound)
                + (isLeftClosed ? 0 : 1)
                + Double.doubleToRawLongBits(rightBound)
                + (isRightClosed ? 0 : 1);
        return (int) (x ^ (x >>> 32));
    }
}
