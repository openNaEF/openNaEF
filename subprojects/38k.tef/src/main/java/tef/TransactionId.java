package tef;

public abstract class TransactionId implements java.io.Serializable {

    public static TransactionId getInstance(String transactionIdStr) {
        char typeId = transactionIdStr.charAt(0);
        int serial
                = Integer.parseInt
                (transactionIdStr.substring(1, transactionIdStr.length()), 16);

        if (typeId == TransactionId.W.TYPE_ID) {
            return new TransactionId.W(serial);
        } else if (typeId == TransactionId.R.TYPE_ID) {
            return new TransactionId.R(serial);
        } else {
            throw new RuntimeException("'" + typeId + "'");
        }
    }

    public abstract char getTypeId();

    public static final class W extends TransactionId implements Comparable<W> {

        static final char TYPE_ID = 'w';

        public W(int serial) {
            super(serial);
        }

        public char getTypeId() {
            return TYPE_ID;
        }

        public int compareTo(TransactionId.W another) {
            return this.serial < another.serial
                    ? -1
                    : (this.serial == another.serial
                    ? 0
                    : 1);
        }
    }

    public static final class R extends TransactionId {

        static final char TYPE_ID = 'r';

        public R(int serial) {
            super(serial);
        }

        public char getTypeId() {
            return TYPE_ID;
        }
    }

    public static final TransactionId.W PREHISTORIC_TRANSACTION_ID
            = new TransactionId.W(Integer.MIN_VALUE);

    public final int serial;

    protected TransactionId(int serial) {
        this.serial = serial;
    }

    public String getIdString() {
        return getTypeId() + Integer.toString(serial, 16);
    }

    @Override
    public int hashCode() {
        return serial;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null
                && getClass() == obj.getClass()
                && serial == ((TransactionId) obj).serial;
    }

    @Override
    public String toString() {
        return getIdString();
    }

    static String getIdString(TransactionId obj) {
        return obj == null ? null : obj.getIdString();
    }

    static int hashCode(TransactionId id) {
        return id == null ? 0 : id.serial;
    }

    static boolean equals(TransactionId id1, TransactionId id2) {
        return id1 == null
                ? id2 == null
                : (id2 != null
                && id1.getClass() == id2.getClass()
                && id1.serial == id2.serial);
    }

    public static final void sort(TransactionId.W[] writeTransactionIds) {
        java.util.Arrays.sort
                (writeTransactionIds,
                        new java.util.Comparator<TransactionId.W>() {

                            public int compare(TransactionId.W o1, TransactionId.W o2) {
                                return o1.serial - o2.serial;
                            }
                        });
    }
}
