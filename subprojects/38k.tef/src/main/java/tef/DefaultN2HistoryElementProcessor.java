package tef;

class DefaultN2HistoryElementProcessor implements MVO.N2HistoryElementProcessor {

    private BinaxesMapEngine.BinaxesMapHistoryElementProcessor elementHistoryProcessor_;

    DefaultN2HistoryElementProcessor
            (BinaxesMapEngine.BinaxesMapHistoryElementProcessor elementHistoryProcessor) {
        elementHistoryProcessor_ = elementHistoryProcessor;
    }

    @Override
    public void preprocessN2(MVO.N2 target) {
    }

    @Override
    public void postprocessN2(MVO.N2 target) {
    }

    @Override
    public void preprocessKey(Object key) {
    }

    @Override
    public void postprocessKey(Object key) {
    }

    @Override
    public BinaxesMapEngine.BinaxesMapHistoryElementProcessor
    getElementHistoryProcessor() {
        return elementHistoryProcessor_;
    }
}
