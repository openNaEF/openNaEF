package tef;

class DefaultM2HistoryElementProcessor implements MVO.M2HistoryElementProcessor {

    private BinaxesEngine.HistoryElementProcessor elementHistoryProcessor_;

    DefaultM2HistoryElementProcessor
            (BinaxesEngine.HistoryElementProcessor elementHistoryProcessor) {
        elementHistoryProcessor_ = elementHistoryProcessor;
    }

    public void preprocessM2(MVO.M2 target) {
    }

    public void postprocessM2(MVO.M2 target) {
    }

    public void preprocessEntry(Object key) {
    }

    public void postprocessEntry(Object key) {
    }

    public BinaxesEngine.HistoryElementProcessor getElementHistoryProcessor() {
        return elementHistoryProcessor_;
    }
}
