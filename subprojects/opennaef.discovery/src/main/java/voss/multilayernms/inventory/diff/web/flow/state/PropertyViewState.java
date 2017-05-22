package voss.multilayernms.inventory.diff.web.flow.state;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import voss.multilayernms.inventory.diff.util.ConfigUtil;
import voss.multilayernms.inventory.diff.web.flow.FlowContext;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;


public class PropertyViewState extends AbstractState {

    public PropertyViewState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException,
            IOException {
        try {
            PrintWriter writer = context.getHttpServletResponse().getWriter();
            XStream stream = new XStream(new DomDriver("UTF-8"));
            stream.toXML(ConfigUtil.getInstance().getPropertiesConfiguration(), writer);
        } catch (IOException e) {
            log.error(e.toString());
        }

    }

}