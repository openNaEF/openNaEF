package tef;

enum TransactionHistoryTraverseMode {

    AFFECTED_BY_TARGET_VERSION(TimeDimension.ONE),
    DO_NOT_AFFECTED_BY_TARGET_VERSION(TimeDimension.NONE);

    private final TimeDimension timeDimension_;

    TransactionHistoryTraverseMode(TimeDimension timeDimension) {
        timeDimension_ = timeDimension;
    }

    public TimeDimension getTimeDimension() {
        return timeDimension_;
    }
}
