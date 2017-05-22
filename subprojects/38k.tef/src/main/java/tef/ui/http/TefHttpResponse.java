package tef.ui.http;

import lib38k.net.httpd.HttpResponse;
import tef.DateTime;
import tef.GlobalTransactionId;
import tef.TransactionContext;

public abstract class TefHttpResponse extends HttpResponse {

    protected TefHttpResponse() {
    }

    protected void beginDistributedTransaction() {
        GlobalTransactionId globalTransactionId
                = TransactionContext.beginDistributedTransaction();
        TransactionContext.resumeLocalTransaction(globalTransactionId);
    }

    protected void beginWriteTransaction() {
        TransactionContext
                .beginWriteTransaction
                        (((TefHttpRequest) getRequest()).getTransactionDescription());
    }

    protected void beginReadTransaction() {
        TransactionContext
                .beginReadTransaction(((TefHttpRequest) getRequest()).getTransactionDescription());
    }

    protected void commitTransaction() {
        TransactionContext.commit();
    }

    protected void rollbackTransaction() {
        TransactionContext.rollback();
    }

    protected void closeTransaction() {
        TransactionContext.close();
    }

    protected long getTransactionTargetTime() {
        return TransactionContext.getTargetTime();
    }

    protected void setTransactionTargetTime(long time) {
        TransactionContext.setTargetTime(time);
    }

    protected void setTransactionTargetTime(DateTime time) {
        setTransactionTargetTime(time.getValue());
    }
}
