package voss.multilayernms.inventory.nmscore.web.flow.event;

import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;

import javax.servlet.ServletException;


public interface EventCondition {

    boolean matches(FlowContext context) throws ServletException;

}