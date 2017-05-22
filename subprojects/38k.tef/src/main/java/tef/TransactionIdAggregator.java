package tef;

import java.util.HashSet;
import java.util.Set;

public final class TransactionIdAggregator {

    private Set<TransactionId.W> transactionIds_ = new HashSet<TransactionId.W>();

    private final MVO.F1HistoryElementProcessor f1Processor_
            = new DefaultF1HistoryElementProcessor
            (new MonaxisEngine.HistoryElementProcessor() {
                public final void process
                        (Transaction historyElementTransaction,
                         Object historyElementValue) {
                    transactionIds_.add((TransactionId.W) historyElementTransaction.getId());
                }
            });

    private final MVO.S1HistoryElementProcessor s1Processor_
            = new DefaultS1HistoryElementProcessor
            (new MonaxisEngine.HistoryElementProcessor() {
                public void process
                        (Transaction historyElementTransaction,
                         Object historyElementValue) {
                    transactionIds_.add((TransactionId.W) historyElementTransaction.getId());
                }
            });

    private final MVO.M1HistoryElementProcessor m1Processor_
            = new DefaultM1HistoryElementProcessor
            (new MonaxisEngine.HistoryElementProcessor() {
                public void process
                        (Transaction historyElementTransaction,
                         Object historyElementValue) {
                    transactionIds_.add((TransactionId.W) historyElementTransaction.getId());
                }
            });

    private final MVO.F2HistoryElementProcessor f2Processor_
            = new DefaultF2HistoryElementProcessor
            (new BinaxesEngine.HistoryElementProcessor() {
                public final void process
                        (Transaction historyElementTransaction,
                         long historyElementTargetTime,
                         Object historyElementValue) {
                    transactionIds_.add((TransactionId.W) historyElementTransaction.getId());
                }
            });

    private final MVO.S2HistoryElementProcessor s2Processor_
            = new DefaultS2HistoryElementProcessor
            (new BinaxesEngine.HistoryElementProcessor() {
                public final void process
                        (Transaction historyElementTransaction,
                         long historyElementTargetTime,
                         Object historyElementValue) {
                    transactionIds_.add((TransactionId.W) historyElementTransaction.getId());
                }
            });

    private final MVO.M2HistoryElementProcessor m2Processor_
            = new DefaultM2HistoryElementProcessor
            (new BinaxesEngine.HistoryElementProcessor() {
                public final void process
                        (Transaction historyElementTransaction,
                         long historyElementTargetTime,
                         Object historyElementValue) {
                    transactionIds_.add((TransactionId.W) historyElementTransaction.getId());
                }
            });

    private final MVO.N2HistoryElementProcessor n2Processor_
            = new DefaultN2HistoryElementProcessor
            (new BinaxesMapEngine.BinaxesMapHistoryElementProcessor() {

                private final BinaxesEngine.HistoryElementProcessor elemProcessor_
                        = new BinaxesEngine.HistoryElementProcessor() {

                    @Override
                    public final void process
                            (Transaction historyElementTransaction,
                             long historyElementTargetTime,
                             Object historyElementValue) {
                        transactionIds_.add
                                ((TransactionId.W) historyElementTransaction.getId());
                    }
                };

                @Override
                public void preprocessEntry(Object key) {
                }

                @Override
                public void postprocessEntry(Object key) {
                }

                @Override
                public BinaxesEngine.HistoryElementProcessor getElementHistoryProcessor() {
                    return elemProcessor_;
                }
            });

    private final MVO.MvoFieldProcessor mvoProcessor_
            = new MVO.MvoFieldProcessor() {
        public void preprocess(MVO mvo) {
            transactionIds_.add((TransactionId.W) mvo.getInitialVersion());
        }

        public void postprocess(MVO mvo) {
        }

        public MVO.F1HistoryElementProcessor getF1Processor() {
            return f1Processor_;
        }

        public MVO.S1HistoryElementProcessor getS1Processor() {
            return s1Processor_;
        }

        public MVO.M1HistoryElementProcessor getM1Processor() {
            return m1Processor_;
        }

        public MVO.F2HistoryElementProcessor getF2Processor() {
            return f2Processor_;
        }

        public MVO.S2HistoryElementProcessor getS2Processor() {
            return s2Processor_;
        }

        public MVO.M2HistoryElementProcessor getM2Processor() {
            return m2Processor_;
        }

        public MVO.N2HistoryElementProcessor getN2Processor() {
            return n2Processor_;
        }
    };

    final TransactionId.W[] getTransactionIds() {
        TransactionId.W[] result = transactionIds_.toArray(new TransactionId.W[0]);
        TransactionId.sort(result);
        return result;
    }

    @TimeDimensioned(TimeDimension.ONE)
    public static final TransactionId.W[] getTransactionIds(MVO mvo) {
        TransactionIdAggregator aggregator = new TransactionIdAggregator();
        mvo.traverseMvoFields(aggregator.mvoProcessor_);
        return aggregator.getTransactionIds();
    }

    public static final TransactionId.W[] getTransactionIds(MVO.F1 f1) {
        TransactionIdAggregator aggregator = new TransactionIdAggregator();
        f1.traverseHistory(aggregator.f1Processor_);
        return aggregator.getTransactionIds();
    }

    public static final TransactionId.W[] getTransactionIds(MVO.S1 s1) {
        TransactionIdAggregator aggregator = new TransactionIdAggregator();
        s1.traverseHistory(aggregator.s1Processor_);
        return aggregator.getTransactionIds();
    }

    public static final TransactionId.W[] getTransactionIds(MVO.M1 m1) {
        TransactionIdAggregator aggregator = new TransactionIdAggregator();
        m1.traverseHistory(aggregator.m1Processor_);
        return aggregator.getTransactionIds();
    }

    public static final TransactionId.W[] getTransactionIds(MVO.F2 f2) {
        TransactionIdAggregator aggregator = new TransactionIdAggregator();
        f2.traverseHistory(aggregator.f2Processor_);
        return aggregator.getTransactionIds();
    }

    public static final TransactionId.W[] getTransactionIds(MVO.S2 s2) {
        TransactionIdAggregator aggregator = new TransactionIdAggregator();
        s2.traverseHistory(aggregator.s2Processor_);
        return aggregator.getTransactionIds();
    }

    public static final TransactionId.W[] getTransactionIds(MVO.M2 m2) {
        TransactionIdAggregator aggregator = new TransactionIdAggregator();
        m2.traverseHistory(aggregator.m2Processor_);
        return aggregator.getTransactionIds();
    }

    public static final TransactionId.W[] getTransactionIds(MVO.N2 n2) {
        TransactionIdAggregator aggregator = new TransactionIdAggregator();
        n2.traverseHistory(aggregator.n2Processor_);
        return aggregator.getTransactionIds();
    }
}
