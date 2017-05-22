package tef;

class DefaultF2HistoryElementProcessor implements MVO.F2HistoryElementProcessor {

    private BinaxesEngine.HistoryElementProcessor binaxesProcessor_;

    DefaultF2HistoryElementProcessor
            (BinaxesEngine.HistoryElementProcessor binaxesProcessor) {
        binaxesProcessor_ = binaxesProcessor;
    }

    public void preprocess(MVO.F2 target) {
    }

    public void postprocess(MVO.F2 target) {
    }

    public BinaxesEngine.HistoryElementProcessor getBinaxesProcessor() {
        return binaxesProcessor_;
    }
}
