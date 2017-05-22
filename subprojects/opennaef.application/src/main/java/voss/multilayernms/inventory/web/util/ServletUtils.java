package voss.multilayernms.inventory.web.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.util.AAAWebUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ServletUtils {
    public static final String ENCODING_SJIS = "Windows-31J";
    public static final String ENCODING_UTF8 = "UTF-8";
    public static final String ENCODING = ENCODING_UTF8;

    public static final String SERVLET_LOGGER = "servletLog";

    private ServletUtils() {
    }

    public static String empty2null(String s) {
        if (s == null) {
            return null;
        } else if (s.length() == 0) {
            return null;
        }
        return s;
    }

    public static String escape(String s) {
        if (s == null) {
            return s;
        }
        return s.trim().replaceAll("[<>&?:;()]", "");
    }

    protected static String encode(String s, String code)
            throws UnsupportedEncodingException {
        return URLEncoder.encode(s, code);
    }

    public static Throwable getRootCause(Throwable cause) {
        if (cause == null) {
            return null;
        }
        Throwable root = cause;
        while (cause != null) {
            root = cause;
            cause = cause.getCause();
        }
        return root;
    }

    public static String translateCrLf(String str) throws IOException {
        if (str == null || str.equals("")) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new StringReader(str));
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("<br>\r\n");
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return sb.toString();
    }

    public static String authenticate(HttpServletRequest req, String operationName)
            throws ServletException, IOException {
        String ipAddress = req.getRemoteAddr();
        if (ipAddress == null) {
            throw new IllegalArgumentException("remote address is null.");
        }
        try {
            Logger log = LoggerFactory.getLogger(ServletUtils.class);
            log.info("request [" + operationName + "] from [" + ipAddress + "]");
            String editorName = AAAWebUtil.checkAAA(ipAddress, operationName);
            return editorName;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

}