package voss.core.server.util;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import voss.core.server.exception.HttpException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public final class URLUtils {
    private static final String ENCODING = "UTF-8";

    private URLUtils() {

    }

    public static InputStream openInputStream(final URL url)
            throws IOException, HttpException {
        return openInputStream(url, null);
    }

    public static InputStream openInputStream(final URL url,
                                              final ResultHandler handler) throws HttpException, IOException {
        GetMethod method = null;
        URL normalizedUrl = normalizeUrl(url);
        if ("file".equals(normalizedUrl.getProtocol())) {
            return url.openStream();
        }
        HttpClient client = new HttpClient();
        method = new GetMethod(normalizedUrl.toExternalForm());
        int result = client.executeMethod(method);
        if (null != handler) {
            return handler.handleResult(result, method);
        } else {
            if (result != HttpStatus.SC_OK) {
                String body = method.getResponseBodyAsString();
                throw new HttpException(result, body);
            }
            return method.getResponseBodyAsStream();
        }
    }

    public static String getContent(final URL url, final ResultHandler handler)
            throws IOException {
        InputStream is = URLUtils.openInputStream(url, handler);
        StringBuffer sb = new StringBuffer();
        int c = -1;
        while (-1 != (c = is.read())) {
            sb.append((char) c);
        }
        return sb.toString();
    }

    public static String getContent(final URL url) throws IOException {
        return getContent(url, null);
    }

    public static URL normalizeUrl(final URL url) throws MalformedURLException {
        String queryString = url.getQuery();
        if (null == queryString) {
            return url;
        }
        String original = url.toExternalForm();
        StringBuilder sb = new StringBuilder();
        String[] array = queryString.split("&");
        for (String str : array) {
            String[] pair = str.split("=");
            if (2 != pair.length) {
                throw new IllegalArgumentException("Parse failed: " + original);
            }
            try {
                sb.append(pair[0]).append("=");
                sb.append(URLEncoder.encode(pair[1], ENCODING));
                sb.append("&");
            } catch (UnsupportedEncodingException ex) {
                throw new IllegalArgumentException(ex);
            }
        }
        return new URL(original.replace(queryString, StringUtils.chop(sb
                .toString())));
    }

    public static interface ResultHandler {
        InputStream handleResult(final int resultCode, final HttpMethod method)
                throws IOException;
    }

    public static void main(final String[] argv) throws Exception {
        System.out.println(URLUtils.normalizeUrl(new URL(argv[0])));
    }
}