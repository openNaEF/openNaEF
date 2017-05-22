package opennaef.notifier.webhook;

import java.net.URL;

/**
 * Webhook ping failed
 */
public class PingFailed extends Exception {
    private final long _hookId;
    private final URL _callbakURL;

    public PingFailed(long hookId, URL callbakURL) {
        super();
        _hookId = hookId;
        _callbakURL = callbakURL;
    }

    public long hookId() {
        return _hookId;
    }

    public URL callbackURL() {
        return _callbakURL;
    }
}
