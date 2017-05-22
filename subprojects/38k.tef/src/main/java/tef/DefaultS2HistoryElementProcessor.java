package tef;

class DefaultS2HistoryElementProcessor implements MVO.S2HistoryElementProcessor {

    private BinaxesEngine.HistoryElementProcessor elementHistoryProcessor_;

    DefaultS2HistoryElementProcessor
            (BinaxesEngine.HistoryElementProcessor elementHistoryProcessor) {
        elementHistoryProcessor_ = elementHistoryProcessor;
    }

    public void preprocessS2(MVO.S2 target) {
    }

    public void postprocessS2(MVO.S2 target) {
    }

    public void preprocessEntry(Object key) {
    }

    public void postprocessEntry(Object key) {
    }

    public BinaxesEngine.HistoryElementProcessor getElementHistoryProcessor() {
        return elementHistoryProcessor_;
    }
}
