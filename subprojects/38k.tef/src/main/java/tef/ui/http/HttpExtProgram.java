package tef.ui.http;

import lib38k.net.httpd.HttpResponseContents;
import tef.TransactionContext;

public abstract class HttpExtProgram {

    public static class HttpExtProgramException extends Exception {

        public HttpExtProgramException(String message) {
            super(message);
        }
    }

    protected TefHttpRequest request;
    protected TefHttpResponse response;

    protected HttpExtProgram() {
    }

    public abstract HttpResponseContents run() throws HttpExtProgramException;

    protected void beginWriteTransaction() {
        TransactionContext.beginWriteTransaction(request.getTransactionDescription());
    }

    protected void beginReadTransaction() {
        TransactionContext.beginReadTransaction(request.getTransactionDescription());
    }
}
