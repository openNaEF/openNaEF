package voss.multilayernms.inventory.nmscore.web.flow;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import net.phalanx.core.expressions.ObjectFilterQuery;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class Operation {
    public static final String COMMAND_NAME = "cmd";

    public static Object getTargets(FlowContext context) throws IOException {
        return getTargets(getInputData(context.getHttpServletRequest()));
    }

    public static Object getTargets(HttpServletRequest req) throws IOException {
        return getTargets(getInputData(req));
    }

    public static Object getTargets(byte[] inputData) throws IOException {
        XStream xstream = new XStream(new DomDriver("UTF-8"));
        return xstream.fromXML(new GZIPInputStream(new ByteArrayInputStream(inputData)));
    }

    public static ObjectFilterQuery getQuery(FlowContext context) throws IOException {
        return getQuery(getInputData(context.getHttpServletRequest()));
    }

    public static ObjectFilterQuery getQuery(HttpServletRequest req) throws IOException {
        return getQuery(getInputData(req));
    }

    public static ObjectFilterQuery getQuery(byte[] inputData) throws IOException {
        XStream xstream = new XStream(new DomDriver("UTF-8"));
        return (ObjectFilterQuery) xstream.fromXML(new GZIPInputStream(new ByteArrayInputStream(inputData)));
    }

    public static String getObjectType(FlowContext context) {
        return context.getHttpServletRequest().getParameter("objectType");
    }

    public static String getObjectType(HttpServletRequest req) {
        return req.getParameter("objectType");
    }

    public static byte[] getInputData(HttpServletRequest req) throws IOException {
        BufferedInputStream in = new BufferedInputStream(req.getInputStream());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int ch;
        while ((ch = in.read()) != -1) {
            out.write(ch);
        }
        out.flush();
        out.close();
        return out.toByteArray();
    }


}