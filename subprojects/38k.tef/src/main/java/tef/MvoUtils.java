package tef;

public final class MvoUtils {

    private MvoUtils() {
    }

    public static boolean isUpToDate(MVO mvo, TransactionId.W transactionId) {
        return mvo.getLatestVersion().serial <= transactionId.serial;
    }

    public static int compareMvoId(MVO o1, MVO o2) {
        int transactionId1 = o1.getInitialVersion().serial;
        int transactionId2 = o2.getInitialVersion().serial;
        if (transactionId1 != transactionId2) {
            return transactionId1 - transactionId2;
        }

        int transactionLocalSerial1 = o1.getTransactionLocalSerial();
        int transactionLocalSerial2 = o2.getTransactionLocalSerial();
        return transactionLocalSerial1 - transactionLocalSerial2;
    }

    @TimeDimensioned(TimeDimension.ONE)
    public static boolean isExistingAt(MVO mvo) {
        return isExistingAt(mvo, TransactionContext.getTargetVersion());
    }

    @TimeDimensioned(TimeDimension.NONE)
    public static boolean isExistingAt(MVO mvo, TransactionId.W targetVersion) {
        return mvo.getInitialVersion().serial <= targetVersion.serial;
    }
}
