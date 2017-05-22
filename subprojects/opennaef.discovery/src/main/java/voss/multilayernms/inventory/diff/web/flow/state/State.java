package voss.multilayernms.inventory.diff.web.flow.state;

import voss.multilayernms.inventory.diff.web.flow.FlowContext;

import javax.servlet.ServletException;
import java.io.IOException;


public interface State {

    StateId getStateId();

    void execute(FlowContext context) throws ServletException, IOException;

}