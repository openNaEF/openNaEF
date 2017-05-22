package tef;

public final class MvoSummaryComputer {

    private final MVO.F1HistoryElementProcessor f1Processor_
            = new DefaultF1HistoryElementProcessor
            (new MonaxisEngine.HistoryElementProcessor() {
                public final void process
                        (Transaction historyElementTransaction,
                         Object historyElementValue) {
                    if (historyElementTransaction.getId().serial
                            <= targetTransaction.serial) {
                        f1HistoryCount++;
                    }
                }
            });

    private final MVO.F2HistoryElementProcessor f2Processor_
            = new DefaultF2HistoryElementProcessor
            (new BinaxesEngine.HistoryElementProcessor() {
                public final void process
                        (Transaction historyElementTransaction,
                         long historyElementTargetTime,
                         Object historyElementValue) {
                    if (historyElementTransaction.getId().serial
                            <= targetTransaction.serial) {
                        f2HistoryCount++;
                    }
                }
            });

    private final MVO.S1HistoryElementProcessor s1Processor_
            = new MVO.S1HistoryElementProcessor() {
        private MonaxisEngine.HistoryElementProcessor elementHistoryProcessor_
                = new MonaxisEngine.HistoryElementProcessor() {

            public void process
                    (Transaction historyElementTransaction,
                     Object historyElementValue) {
                if (historyElementTransaction.getId().serial
                        <= targetTransaction.serial) {
                    s1HistoryCount++;
                    hasHistoryIncremented_ = true;
                }
            }
        };

        private boolean hasHistoryIncremented_;

        public void preprocessS1(MVO.S1 target) {
        }

        public void postprocessS1(MVO.S1 target) {
        }

        public void preprocessEntry(Object key) {
            resetHistoryIncrementedStatus();
        }

        private final void resetHistoryIncrementedStatus() {
            hasHistoryIncremented_ = false;
        }

        public void postprocessEntry(Object key) {
            if (hasHistoryIncremented_) {
                s1ElementsCount++;
            }
        }

        public MonaxisEngine.HistoryElementProcessor getElementHistoryProcessor() {
            return elementHistoryProcessor_;
        }
    };

    private final MVO.M1HistoryElementProcessor m1Processor_
            = new MVO.M1HistoryElementProcessor() {
        private MonaxisEngine.HistoryElementProcessor elementHistoryProcessor_
                = new MonaxisEngine.HistoryElementProcessor() {

            public void process
                    (Transaction historyElementTransaction,
                     Object historyElementValue) {
                if (historyElementTransaction.getId().serial
                        <= targetTransaction.serial) {
                    m1HistoryCount++;
                    hasHistoryIncremented_ = true;
                }
            }
        };

        private boolean hasHistoryIncremented_;

        public void preprocessM1(MVO.M1 target) {
        }

        public void postprocessM1(MVO.M1 target) {
        }

        public void preprocessEntry(Object key) {
            resetHistoryIncrementedStatus();
        }

        private final void resetHistoryIncrementedStatus() {
            hasHistoryIncremented_ = false;
        }

        public void postprocessEntry(Object key) {
            if (hasHistoryIncremented_) {
                m1ElementsCount++;
            }
        }

        public MonaxisEngine.HistoryElementProcessor getElementHistoryProcessor() {
            return elementHistoryProcessor_;
        }
    };

    private final MVO.S2HistoryElementProcessor s2Processor_
            = new MVO.S2HistoryElementProcessor() {
        private BinaxesEngine.HistoryElementProcessor elementHistoryProcessor_
                = new BinaxesEngine.HistoryElementProcessor() {

            public final void process
                    (Transaction historyElementTransaction,
                     long historyElementTargetTime,
                     Object historyElementValue) {
                if (historyElementTransaction.getId().serial
                        <= targetTransaction.serial) {
                    s2HistoryCount++;
                    hasHistoryIncremented_ = true;
                }
            }
        };

        private boolean hasHistoryIncremented_;

        public void preprocessS2(MVO.S2 target) {
        }

        public void postprocessS2(MVO.S2 target) {
        }

        public void preprocessEntry(Object key) {
            resetHistoryIncrementedStatus();
        }

        private final void resetHistoryIncrementedStatus() {
            hasHistoryIncremented_ = false;
        }

        public void postprocessEntry(Object key) {
            if (hasHistoryIncremented_) {
                s2ElementsCount++;
            }
        }

        public BinaxesEngine.HistoryElementProcessor getElementHistoryProcessor() {
            return elementHistoryProcessor_;
        }
    };

    private final MVO.M2HistoryElementProcessor m2Processor_
            = new MVO.M2HistoryElementProcessor() {
        private BinaxesEngine.HistoryElementProcessor elementHistoryProcessor_
                = new BinaxesEngine.HistoryElementProcessor() {

            public final void process
                    (Transaction historyElementTransaction,
                     long historyElementTargetTime,
                     Object historyElementValue) {
                if (historyElementTransaction.getId().serial
                        <= targetTransaction.serial) {
                    m2HistoryCount++;
                    hasHistoryIncremented_ = true;
                }
            }
        };

        private boolean hasHistoryIncremented_;

        public void preprocessM2(MVO.M2 target) {
        }

        public void postprocessM2(MVO.M2 target) {
        }

        public void preprocessEntry(Object key) {
            resetHistoryIncrementedStatus();
        }

        private final void resetHistoryIncrementedStatus() {
            hasHistoryIncremented_ = false;
        }

        public void postprocessEntry(Object key) {
            if (hasHistoryIncremented_) {
                m2ElementsCount++;
            }
        }

        public BinaxesEngine.HistoryElementProcessor getElementHistoryProcessor() {
            return elementHistoryProcessor_;
        }
    };

    private MVO.MvoFieldProcessor mvoProcessor_ = new MVO.MvoFieldProcessor() {

        public void preprocess(MVO mvo) {
            mvoCount++;
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
            return null;
        }
    };

    final TransactionId.W targetTransaction;

    public int mvoCount = 0;
    public int f1HistoryCount = 0;
    public int s1ElementsCount = 0;
    public int s1HistoryCount = 0;
    public int m1ElementsCount = 0;
    public int m1HistoryCount = 0;
    public int f2HistoryCount = 0;
    public int s2ElementsCount = 0;
    public int s2HistoryCount = 0;
    public int m2ElementsCount = 0;
    public int m2HistoryCount = 0;

    public MvoSummaryComputer(TransactionId.W targetTransaction) {
        this.targetTransaction = targetTransaction;
    }

    public void compute() {
        for (MVO mvo : TefService.instance().getMvoRegistry().list()) {
            compute(mvo);
        }
    }

    private void compute(MVO mvo) {
        if (!MvoUtils.isExistingAt(mvo, targetTransaction)) {
            return;
        }

        mvo.traverseMvoFields(mvoProcessor_);
    }

    MVO.F1HistoryElementProcessor getF1Processor() {
        return f1Processor_;
    }

    MVO.S1HistoryElementProcessor getS1Processor() {
        return s1Processor_;
    }

    MVO.M1HistoryElementProcessor getM1Processor() {
        return m1Processor_;
    }

    MVO.F2HistoryElementProcessor getF2Processor() {
        return f2Processor_;
    }

    MVO.S2HistoryElementProcessor getS2Processor() {
        return s2Processor_;
    }

    MVO.M2HistoryElementProcessor getM2Processor() {
        return m2Processor_;
    }
}
