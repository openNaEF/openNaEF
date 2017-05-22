package tef.ui.http;

import lib38k.net.httpd.HttpConnection;
import lib38k.net.httpd.HttpRequest;
import lib38k.net.httpd.HttpServer;
import tef.TransactionContext;

import java.net.Socket;

public class TefHttpConnection extends HttpConnection {

    public TefHttpConnection(HttpServer httpd, Socket socket) {
        super(httpd, socket);
    }

    protected void processConnection() {
        try {
            super.processConnection();
        } finally {
            TransactionContext.close();
        }
    }

    protected HttpRequest createHttpRequest() throws HttpRequest.RequestException {
        return new TefHttpRequest(this);
    }

    protected String getTransactionDescription(HttpRequest request) {
        return null;
    }
}
