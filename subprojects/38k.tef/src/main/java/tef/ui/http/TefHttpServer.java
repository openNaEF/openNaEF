package tef.ui.http;

import lib38k.logger.Logger;
import lib38k.net.httpd.HttpConnection;
import lib38k.net.httpd.HttpServer;

import java.net.Socket;

public class TefHttpServer extends HttpServer {

    public static final String PLUGIN_CONFIG_FILE_NAME = "TefHttpPluginsConfig.xml";
    public static final String EXTRA_RESPONSE_STATUS_HEADER_FIELD_NAME
            = "X-TEF-Response-Status";

    public TefHttpServer(Config config, Logger logger) {
        super(config, logger);
    }

    protected HttpConnection newConnection(Socket socket) {
        return new TefHttpConnection(this, socket);
    }
}
