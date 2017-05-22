package tef.ui.http;

import lib38k.net.httpd.HttpConnection;
import lib38k.net.httpd.HttpRequest;

public class TefHttpRequest extends HttpRequest {

    public TefHttpRequest(HttpConnection connection) throws RequestException {
        super(connection);
    }

    public String getTransactionDescription() {
        return ((TefHttpConnection) getConnection())
                .getTransactionDescription(TefHttpRequest.this);
    }
}
