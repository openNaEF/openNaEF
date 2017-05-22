package tef;

class DefaultS1HistoryElementProcessor implements MVO.S1HistoryElementProcessor {

    private MonaxisEngine.HistoryElementProcessor elementHistoryProcessor_;

    DefaultS1HistoryElementProcessor
            (MonaxisEngine.HistoryElementProcessor elementHistoryProcessor) {
        elementHistoryProcessor_ = elementHistoryProcessor;
    }

    public void preprocessS1(MVO.S1 target) {
    }

    public void postprocessS1(MVO.S1 target) {
    }

    public void preprocessEntry(Object key) {
    }

    public void postprocessEntry(Object key) {
    }

    public MonaxisEngine.HistoryElementProcessor getElementHistoryProcessor() {
        return elementHistoryProcessor_;
    }
}
