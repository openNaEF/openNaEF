package voss.core.common.url;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UrlBuilder implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(UrlBuilder.class);
    private final URLCodec codec = new URLCodec("UTF-8");
    private String protocol = "http";
    private String host = "localhost";
    private String port = null;
    private List<PathValue> pathElements = new ArrayList<PathValue>();
    private List<KeyValue> keyValueElements = new ArrayList<KeyValue>();

    public UrlBuilder() {
    }

    public UrlBuilder(String url) {
        if (url == null || url.length() == 0) {
            return;
        }
        int idx0 = url.indexOf("://");
        if (idx0 == -1) {
            return;
        }
        setProtocol(decode(url.substring(0, idx0)));
        url = url.substring(idx0 + 3);

        int idx1 = url.indexOf('/');
        int idx2 = url.indexOf('?');
        String hostPort = "";
        String pathSeries = "";
        String keyValues = "";
        if (idx1 == -1 && idx2 == -1) {
            hostPort = url;
        } else if (idx1 > -1 && idx2 > -1) {
            hostPort = url.substring(0, idx1);
            pathSeries = url.substring(idx1 + 1, idx2);
            keyValues = url.substring(idx2 + 1);
        } else if (idx1 > -1) {
            hostPort = url.substring(0, idx1);
            pathSeries = url.substring(idx1 + 1);
        } else if (idx2 > -1) {
            hostPort = url.substring(0, idx2);
            keyValues = url.substring(idx2 + 1);
        } else {
            throw new IllegalArgumentException();
        }

        if (hostPort != null) {
            setHostPort(hostPort);
        }

        int next = pathSeries.indexOf('/');
        while (next > -1) {
            String path = pathSeries.substring(0, next + 1);
            addPath(path);
            pathSeries = pathSeries.substring(next + 1);
            next = pathSeries.indexOf('/');
        }
        if (pathSeries.length() > 0) {
            addPath(pathSeries);
        }

        if (keyValues.length() == 0) {
            return;
        }
        next = keyValues.indexOf('&');
        while (next > -1) {
            String s = keyValues.substring(0, next);
            addKeyValue(s);
            keyValues = keyValues.substring(next + 1);
            next = keyValues.indexOf('&');
        }
        if (keyValues.length() > 0) {
            addKeyValue(keyValues);
        }

    }

    private void addPath(String path) {
        PathValue pv = new PathValue();
        if (path != null && path.length() > 0) {
            if (path.charAt(path.length() - 1) == '/') {
                pv.setPath(decode(path.substring(0, path.length() - 1)));
                pv.setTrailingSlash(true);
            } else {
                pv.setPath(decode(path));
                pv.setTrailingSlash(false);
            }
            this.pathElements.add(pv);
        }
    }

    private void addKeyValue(String s) {
        KeyValue kv = new KeyValue();
        if (s != null && s.length() > 0) {
            int i = s.indexOf('=');
            if (i == -1) {
                kv.setKey(decode(s));
            } else {
                kv.setKey(decode(s.substring(0, i)));
                kv.setValue(decode(s.substring(i + 1)));
            }
            this.keyValueElements.add(kv);
        }
    }

    public String getUrl() {
        StringBuilder sb = new StringBuilder();
        sb.append(protocol).append("://").append(convert(host));
        if (port != null) {
            sb.append(":").append(port);
        }
        sb.append("/");
        for (PathValue pathElement : this.pathElements) {
            sb.append(pathElement.get());
        }
        if (this.keyValueElements.size() > 0) {
            sb.append("?");
            boolean firstElement = true;
            for (KeyValue kv : this.keyValueElements) {
                if (firstElement) {
                    firstElement = false;
                } else {
                    sb.append("&");
                }
                sb.append(kv.getKeyValue());
            }
        }
        return sb.toString();
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setHostPort(String hostPort) {
        if (hostPort == null) {
            return;
        }
        int index = hostPort.indexOf(':');
        if (index == -1) {
            this.host = hostPort;
        } else {
            this.host = hostPort.substring(0, index);
            this.port = hostPort.substring(index + 1);
        }
    }

    public void addPathElement(String element) {
        PathValue pe = new PathValue();
        pe.setPath(element);
        this.pathElements.add(pe);
    }

    public List<PathValue> getPathElements() {
        List<PathValue> result = new ArrayList<PathValue>();

        return result;
    }

    public void addKeyValue(String key, String value) {
        KeyValue kv = new KeyValue();
        kv.setKey(key);
        kv.setValue(value);
        this.keyValueElements.add(kv);
    }

    public List<KeyValue> getKeyValueElements() {
        List<KeyValue> result = new ArrayList<KeyValue>();
        for (KeyValue kv : this.keyValueElements) {
            KeyValue copy = kv.copy();
            result.add(copy);
        }
        return result;
    }

    public void replace(String before, String after) {
        if (this.protocol != null && this.protocol.contains(before)) {
            this.protocol = this.protocol.replace(before, after);
        }
        if (this.host != null && this.host.contains(before)) {
            this.host = this.host.replace(before, after);
        }
        if (this.port != null && this.port.contains(before)) {
            this.port = this.port.replace(before, after);
        }
        for (PathValue pv : this.pathElements) {
            if (pv == null || pv.getPath() == null) {
                continue;
            }
            pv.replace(before, after);
        }
        for (KeyValue kv : this.keyValueElements) {
            if (kv == null || kv.getKey() == null) {
                continue;
            }
            kv.replace(before, after);
        }
    }

    public String decode(String value) {
        try {
            return this.codec.decode(value);
        } catch (DecoderException e) {
            throw new IllegalArgumentException("cannot decode [" + value + "]", e);
        }
    }

    public String convert(String value) {
        if (value == null) {
            return null;
        }
        try {
            String s = codec.encode(value);
            if (s.contains("+")) {
                s = s.replaceAll("\\+", "%20");
            }
            log.debug("** [" + value + "]->[" + s + "]");
            return s;
        } catch (EncoderException e) {
            throw new IllegalArgumentException("illegal value: [" + value + "]", e);
        }
    }

    private class PathValue {
        private String path;
        private boolean hasTrailingSlash = false;

        public void setPath(String path) {
            this.path = path;
        }

        public String getPath() {
            return this.path;
        }

        public boolean hasTrailingSlash() {
            return this.hasTrailingSlash;
        }

        public void setTrailingSlash(boolean value) {
            this.hasTrailingSlash = value;
        }

        public String get() {
            return convert(this.path) + (this.hasTrailingSlash ? "/" : "");
        }

        public void replace(String before, String after) {
            if (this.path == null) {
                return;
            }
            this.path = path.replace(before, after);
        }

        public String toString() {
            return this.path + (this.hasTrailingSlash ? "/" : "");
        }
    }

    private class KeyValue {
        private String key;
        private String value;

        public String getKey() {
            return this.key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return this.value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getKeyValue() {
            StringBuilder sb = new StringBuilder();
            sb.append(convert(this.key));
            sb.append("=");
            if (value != null) {
                sb.append(convert(this.value));
            }
            return sb.toString();
        }

        public void replace(String before, String after) {
            if (this.key != null && this.key.contains(before)) {
                this.key = this.key.replace(before, after);
            }
            if (this.value != null && this.value.contains(before)) {
                this.value = this.value.replace(before, after);
            }
        }

        public KeyValue copy() {
            KeyValue kv = new KeyValue();
            kv.setKey(this.key);
            kv.setValue(this.value);
            return kv;
        }

        public String toString() {
            return this.key + "=" + this.value;
        }
    }

}