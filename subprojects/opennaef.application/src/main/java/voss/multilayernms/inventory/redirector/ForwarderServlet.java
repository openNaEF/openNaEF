package voss.multilayernms.inventory.redirector;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.aaa.AAAUtil;
import voss.core.server.aaa.NotLoggedInException;
import voss.core.server.config.ServiceConfigRegistry;
import voss.multilayernms.inventory.MplsNmsLogCategory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


public class ForwarderServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger log = LoggerFactory.getLogger(MplsNmsLogCategory.LOG_AAA);
    private Distributer distributer;
    private byte[] inputStreamDataCache;

    @Override
    public void init() {
        try {
            String keyRootDir = ServiceConfigRegistry.KEY_ROOT_DIR;
            String defaultRootDirName = ServiceConfigRegistry.DEFAULT_ROOT_DIR_NAME;
            String configDirName = ServiceConfigRegistry.CONFIG_DIR_NAME;
            String dirName = System.getProperty(keyRootDir, defaultRootDirName);
            String fileName = "redirect.config";
            distributer = new Distributer(dirName + "/" + configDirName + "/" + fileName);
            inputStreamDataCache = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            doService(createGetMethod(req, distributer.getRedirectUrl(req)), req, resp);
        } catch (NotLoggedInException e) {
            sendError(resp, "Authentication failed", HttpServletResponse.SC_UNAUTHORIZED, getContentType(req));
        } catch (Exception e) {
            sendError(resp, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, getContentType(req));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        try {
            doService(createPutMethod(req, distributer.getRedirectUrl(req)), req, resp);
        } catch (NotLoggedInException e) {
            sendError(resp, "Authentication failed", HttpServletResponse.SC_UNAUTHORIZED, getContentType(req));
        } catch (Exception e) {
            sendError(resp, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, getContentType(req));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            doService(createPostMethod(req, distributer.getRedirectUrl(req)), req, resp);
        } catch (NotLoggedInException e) {
            sendError(resp, "Authentication failed", HttpServletResponse.SC_UNAUTHORIZED, getContentType(req));
        } catch (Exception e) {
            sendError(resp, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, getContentType(req));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        try {
            doService(createDeleteMethod(req, distributer.getRedirectUrl(req)), req, resp);
        } catch (NotLoggedInException e) {
            sendError(resp, "Authentication failed", HttpServletResponse.SC_UNAUTHORIZED, getContentType(req));
        } catch (Exception e) {
            sendError(resp, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, getContentType(req));
        }
    }

    private void addUserNameToHeader(HttpMethod method, HttpServletRequest req) throws ServletException,
            NotLoggedInException {
        method.addRequestHeader("X-VOSS-REQUESTED-USER", checkAAA(req));
    }

    private void addAcceptEncodingHeader(HttpMethod method, HttpServletRequest req) {
        method.addRequestHeader("Accept-encoding", "gzip,deflate");
    }

    @SuppressWarnings("unchecked")
    private HttpMethod createGetMethod(HttpServletRequest req, String uri) throws ServletException,
            NotLoggedInException {

        GetMethod get = new GetMethod(uri);
        addUserNameToHeader(get, req);
        addAcceptEncodingHeader(get, req);

        get.getParams().setContentCharset("UTF-8");
        HttpMethodParams params = new HttpMethodParams();
        Enumeration e = req.getParameterNames();
        while (e.hasMoreElements()) {
            String paramName = (String) e.nextElement();
            for (String value : req.getParameterValues(paramName)) {
                params.setParameter(paramName, value);
            }
        }
        get.setParams(params);
        return get;
    }


    @SuppressWarnings("unchecked")
    private HttpMethod createPutMethod(HttpServletRequest req, String redirectUrl) throws IOException,
            ServletException, NotLoggedInException {
        PutMethod put = new PutMethod(redirectUrl);
        addUserNameToHeader(put, req);
        addAcceptEncodingHeader(put, req);

        put.getParams().setContentCharset("UTF-8");
        HttpMethodParams params = new HttpMethodParams();
        Enumeration e = req.getParameterNames();
        while (e.hasMoreElements()) {
            String paramName = (String) e.nextElement();
            String[] values = req.getParameterValues(paramName);
            for (String value : values) {
                params.setParameter(paramName, value);
            }
        }
        put.setParams(params);

        InputStream in = null;
        if (inputStreamDataCache != null) {
            in = new ByteArrayInputStream(inputStreamDataCache);
        } else {
            in = req.getInputStream();
        }
        put.setRequestEntity(new InputStreamRequestEntity(in));
        return put;
    }

    @SuppressWarnings("unchecked")
    private HttpMethod createDeleteMethod(HttpServletRequest req, String redirectUrl) throws IOException, ServletException, NotLoggedInException {
        DeleteMethod delete = new DeleteMethod(redirectUrl);
        addUserNameToHeader(delete, req);
        addAcceptEncodingHeader(delete, req);

        delete.getParams().setContentCharset("UTF-8");
        HttpMethodParams params = new HttpMethodParams();
        Enumeration e = req.getParameterNames();
        while (e.hasMoreElements()) {
            String paramName = (String) e.nextElement();
            String[] values = req.getParameterValues(paramName);
            for (String value : values) {
                params.setParameter(paramName, value);
            }
        }
        delete.setParams(params);

        return delete;
    }

    private HttpMethod createPostMethod(HttpServletRequest req, String redirectUrl) throws ServletException,
            NotLoggedInException {
        PostMethod post = new PostMethod(redirectUrl);
        addUserNameToHeader(post, req);
        addAcceptEncodingHeader(post, req);

        post.getParams().setContentCharset("UTF-8");
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        @SuppressWarnings("unchecked")
        Enumeration e = req.getParameterNames();
        while (e.hasMoreElements()) {
            String paramName = (String) e.nextElement();
            String[] values = req.getParameterValues(paramName);
            for (String value : values) {
                NameValuePair pair = new NameValuePair(paramName, value);
                pairs.add(pair);
            }
        }
        post.addParameters(pairs.toArray(new NameValuePair[0]));
        return post;
    }

    private void doService(HttpMethod method, HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        outputRequestLog(req);

        InputStream iStream = null;
        ServletOutputStream oStream = null;
        try {
            long threadID = Thread.currentThread().getId();
            log.debug("[" + threadID + "] forwarded to " + distributer.getRedirectUrl(req));
            HttpClient client = new HttpClient();
            log.debug("[" + threadID + "]send request.");
            int resultCode = client.executeMethod(method);
            log.debug("[" + threadID + "]got response: result code is " + resultCode);
            res.setStatus(resultCode);
            for (Header header : method.getResponseHeaders()) {
                res.setHeader(header.getName(), header.getValue());
            }
            iStream = method.getResponseBodyAsStream();
            oStream = res.getOutputStream();

            writeOutputStream(iStream, oStream);

            log.debug("[" + threadID + "] response sent to client.");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServletException(e.getMessage(), e);
        } finally {
            if (iStream != null) {
                iStream.close();
            }
            if (oStream != null) {
                oStream.close();
            }
        }
    }

    private void writeOutputStream(InputStream in, ServletOutputStream out) throws IOException {
        int length;
        byte[] bytes = new byte[512];

        while ((length = in.read(bytes)) != -1) {
            out.write(bytes, 0, length);
            out.flush();
        }
    }

    private String checkAAA(HttpServletRequest req) throws ServletException, NotLoggedInException {
        try {
            OperationNameResolver resolver = new OperationNameResolver();
            String userName = AAAUtil.checkAAA(req.getRemoteHost(), resolver.getOperationName(req));
            inputStreamDataCache = resolver.getInputStreamData();
            return userName;
        } catch (NotLoggedInException e) {
            log.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServletException(e.getMessage(), e);
        }
    }

    private void outputRequestLog(HttpServletRequest req) {
        log.info("requestURL : " + req.getRequestURL().append(req.getQueryString()));
        log.info("remoteAddr : " + req.getRemoteAddr());
    }

    private void sendError(HttpServletResponse resp, String message, int status, String contentType) {
        try {
            resp.setContentType(contentType);
            resp.setContentLength(message.length());
            resp.setStatus(status);
            PrintWriter writer = resp.getWriter();
            writer.println(message);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.error("Error response write error.", e);
        }
    }

    private static final List<String> ContentTypeHtmlCmdList = new ArrayList<String>();

    static {
        ContentTypeHtmlCmdList.add("diffStatus");
        ContentTypeHtmlCmdList.add("statisticLogCsvDownload");
    }

    private String getContentType(HttpServletRequest req) {
        String result = "text/plane; charset=UTF-8";
        String cmd = req.getParameter("cmd");
        if (cmd != null && ContentTypeHtmlCmdList.contains(cmd)) {
            result = "text/html; charset=UTF-8";
        }
        return result;
    }

}