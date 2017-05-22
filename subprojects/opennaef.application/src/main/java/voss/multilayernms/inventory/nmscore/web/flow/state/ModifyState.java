package voss.multilayernms.inventory.nmscore.web.flow.state;

import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;

import java.io.IOException;


public interface ModifyState {

    Object getTargets(FlowContext context) throws IOException;

    String getUserName(FlowContext context) throws IOException;

}