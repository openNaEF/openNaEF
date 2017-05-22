package voss.multilayernms.inventory.nmscore.fwlb;

import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;

import javax.servlet.ServletException;

public interface FWLBController {
    public boolean canHandle(String action);

    public Object handleGet(FlowContext context) throws ServletException;

    public Object handlePut(FlowContext context, Object data) throws ServletException;
}