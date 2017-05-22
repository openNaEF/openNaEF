package tef;

class DefaultM1HistoryElementProcessor implements MVO.M1HistoryElementProcessor {

    private MonaxisEngine.HistoryElementProcessor elementHistoryProcessor_;

    DefaultM1HistoryElementProcessor
            (MonaxisEngine.HistoryElementProcessor elementHistoryProcessor) {
        elementHistoryProcessor_ = elementHistoryProcessor;
    }

    public void preprocessM1(MVO.M1 target) {
    }

    public void postprocessM1(MVO.M1 target) {
    }

    public void preprocessEntry(Object key) {
    }

    public void postprocessEntry(Object key) {
    }

    public MonaxisEngine.HistoryElementProcessor getElementHistoryProcessor() {
        return elementHistoryProcessor_;
    }
}
