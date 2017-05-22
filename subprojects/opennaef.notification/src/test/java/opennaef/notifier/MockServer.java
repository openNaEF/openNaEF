package opennaef.notifier;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.stream.Collectors;

/**
 * Webhook ping 受信用 Http server
 */
public class MockServer  implements HttpHandler {
    public static HttpServer start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new MockServer());
        server.start();
        return server;
    }

    public void handle(HttpExchange event) throws IOException {
        URI uri = event.getRequestURI();

        // favicon.ico, OPTION は 無視する
        if (uri.getPath().endsWith("favicon.ico")
                || event.getRequestMethod().equalsIgnoreCase("OPTION")) {
            sendOk(event);
            return;
        }

        System.out.println(generateCurl(event));
        sendOk(event);
    }

    public static void sendOk(HttpExchange event) throws IOException {
        try {
            event.sendResponseHeaders(200, 0);
        } finally {
            event.close();
        }
    }

    public static String generateCurl(HttpExchange event) throws IOException {
        // uri
        String uri = getSchema(event) + "://" + event.getLocalAddress()  + event.getRequestURI().toString();

        // header
        StringBuilder headers = new StringBuilder();
        event.getRequestHeaders().entrySet()
                .forEach(header -> header.getValue()
                        .forEach(value -> {
                            headers.append(" -H \"" + header.getKey() + ":" + value + "\"");
                        }));

        // body
        String body;
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(event.getRequestBody()))) {
            body = buffer.lines().collect(Collectors.joining("\n"));
        }

        String curl = "curl" +
                " -v" +
                " -X " + event.getRequestMethod() +
                " " + uri;
        if (headers.length() > 0) {
            curl += headers;
        }

        if (!body.isEmpty()) {
            curl += " -d '" + body + "'";
        }

        return curl;
    }

    private static String getSchema(HttpExchange event) {
        String protocol = event.getProtocol();
        if (protocol.startsWith("HTTPS")) return "https";
        if (protocol.startsWith("HTTP")) return "http";

        return "http";
    }
}