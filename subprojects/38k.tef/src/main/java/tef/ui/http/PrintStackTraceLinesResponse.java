package tef.ui.http;

import lib38k.net.httpd.*;
import tef.TransactionId;

import java.util.List;

public class PrintStackTraceLinesResponse extends TefHttpResponse {

    private static final String ARGNAME_TRANSACTION_ID = "transaction-id";

    public static HtmlATag getReferenceTag(HttpServer httpd, TransactionId.W transactionId) {
        String transactionIdStr = transactionId.getIdString();
        HttpUrl url
                = httpd.url(PrintStackTraceLinesResponse.class)
                .addQueryParam(ARGNAME_TRANSACTION_ID, transactionIdStr);
        return new HtmlATag(transactionIdStr).href(url);
    }

    public PrintStackTraceLinesResponse() {
    }

    public HttpResponseContents getContents() throws HttpException {
        String transactionIdStr = getRequest().getParameter(ARGNAME_TRANSACTION_ID);
        TransactionId.W transactionId
                = (TransactionId.W) TransactionId.getInstance(transactionIdStr);

        Html result = new Html(UTF8);
        result.setTitle("stacktrace: " + transactionIdStr);

        List<String> stacktraceLines = tef.TefUtils.getStackTraceLines(transactionId);
        if (stacktraceLines == null) {
            result.printbr("no stack-trace is available.");
        } else {
            for (String line : stacktraceLines) {
                result.printbr(line);
            }
        }

        return result;
    }
}
