package voss.multilayernms.inventory.diff.web.flow.state;

import org.apache.commons.configuration.ConfigurationException;
import voss.multilayernms.inventory.diff.service.RegularExecution;
import voss.multilayernms.inventory.diff.util.ConfigUtil;
import voss.multilayernms.inventory.diff.web.flow.FlowContext;

import javax.servlet.ServletException;
import java.io.IOException;


public class PropertyReloadState extends AbstractState {

    public PropertyReloadState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException,
            IOException {
        log.debug("diff property reload");
        try {
            if (!ConfigUtil.getInstance().reload()) {
                throw new IOException("property reload error.");
            }
            RegularExecution.getInstance().reScheduleAll();
        } catch (ConfigurationException e) {
            throw new ServletException(e.getMessage(), e);
        }
    }

}