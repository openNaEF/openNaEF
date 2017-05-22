package tef;

class DefaultF1HistoryElementProcessor implements MVO.F1HistoryElementProcessor {

    private MonaxisEngine.HistoryElementProcessor monaxisProcessor_;

    DefaultF1HistoryElementProcessor
            (MonaxisEngine.HistoryElementProcessor monaxisProcessor) {
        monaxisProcessor_ = monaxisProcessor;
    }

    public void preprocess(MVO.F1 target) {
    }

    public void postprocess(MVO.F1 target) {
    }

    public MonaxisEngine.HistoryElementProcessor getMonaxisProcessor() {
        return monaxisProcessor_;
    }
}
