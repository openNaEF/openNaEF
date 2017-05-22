package voss.multilayernms.inventory.nmscore.web;

import javax.servlet.ServletException;

public class ResponseCodeException extends ServletException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public ResponseCodeException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

}