package voss.core.server.exception;

import java.io.IOException;

@SuppressWarnings("serial")
public class HttpException extends IOException {
    private final String b;
    private final int s;

    public HttpException(final int status, final String body) {
        this.b = body;
        this.s = status;
    }

    public String getBody() {
        return b;
    }

    public int getStatus() {
        return s;
    }
}