package voss.multilayernms.inventory.nmscore.web.flow.state;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class UnificUIViewState implements State {

    static Logger log = LoggerFactory.getLogger(UnificUIViewState.class);
    private final StateId stateId;
    private Object xmlObject;

    public UnificUIViewState(StateId stateId) {
        this.stateId = stateId;
    }

    @Override
    public StateId getStateId() {
        return stateId;
    }

    protected void setXmlObject(Object xmlObject) {
        this.xmlObject = xmlObject;
    }

    @Override
    public void execute(FlowContext context) throws ServletException, IOException, InventoryException, ExternalServiceException {

        HttpServletResponse res = context.getHttpServletResponse();
        res.setCharacterEncoding("UTF-8");
        res.setContentType("text/xml; charset=UTF-8");

        XStream stream = new XStream(new DomDriver("UTF-8"));
        stream.toXML(xmlObject, res.getWriter());
    }

}