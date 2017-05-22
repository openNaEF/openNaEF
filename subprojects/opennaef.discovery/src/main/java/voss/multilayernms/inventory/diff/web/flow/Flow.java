package voss.multilayernms.inventory.diff.web.flow;

import voss.multilayernms.inventory.diff.web.flow.event.Any;
import voss.multilayernms.inventory.diff.web.flow.event.Event;
import voss.multilayernms.inventory.diff.web.flow.event.EventCondition;
import voss.multilayernms.inventory.diff.web.flow.event.ParameterMatch;
import voss.multilayernms.inventory.diff.web.flow.state.*;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Flow {

    private List<Event> events = new ArrayList<Event>();
    private Map<StateId, State> stateMap = new HashMap<StateId, State>();

    public Flow() {

        addState(new CreateDiffState(StateId.createDiff));
        addEvent(new ParameterMatch("cmd", StateId.createDiff.toString()), StateId.createDiff);

        addState(new CreateDiffInterruptState(StateId.createDiffInterrupt));
        addEvent(new ParameterMatch("cmd", StateId.createDiffInterrupt.toString()), StateId.createDiffInterrupt);

        addState(new ApplyDiffState(StateId.applyDiff));
        addEvent(new ParameterMatch("cmd", StateId.applyDiff.toString()), StateId.applyDiff);

        addState(new IgnoreDiffState(StateId.ignoreDiff));
        addEvent(new ParameterMatch("cmd", StateId.ignoreDiff.toString()), StateId.ignoreDiff);

        addState(new ViewDiffSetState(StateId.viewDiff));
        addEvent(new ParameterMatch("cmd", StateId.viewDiff.toString()), StateId.viewDiff);

        addState(new PropertyReloadState(StateId.diffPropertyReload));
        addEvent(new ParameterMatch("cmd", StateId.diffPropertyReload.toString()), StateId.diffPropertyReload);

        addState(new PropertyViewState(StateId.diffPropertyView));
        addEvent(new ParameterMatch("cmd", StateId.diffPropertyView.toString()), StateId.diffPropertyView);

        addState(new LockState(StateId.lock));
        addEvent(new ParameterMatch("cmd", StateId.lock.toString()), StateId.lock);

        addState(new UnlockState(StateId.unlock));
        addEvent(new ParameterMatch("cmd", StateId.unlock.toString()), StateId.unlock);

        addState(new UnlockForceState(StateId.unlockForce));
        addEvent(new ParameterMatch("cmd", StateId.unlockForce.toString()), StateId.unlockForce);

        addState(new DiffStatusState(StateId.diffStatus, "/WEB-INF/pages/lockStatus.jsp"));
        addEvent(new ParameterMatch("cmd", StateId.diffStatus.toString()), StateId.diffStatus);

        addState(new UnknownRequestState(StateId.unknownRequest));
        addEvent(new Any(), StateId.unknownRequest);
    }

    private void addState(State state) {
        stateMap.put(state.getStateId(), state);
    }

    private void addEvent(EventCondition condition, StateId stateId) {
        State state = stateMap.get(stateId);
        if (state == null) {
            throw new IllegalStateException("state " + stateId + " was not found");
        }
        events.add(new Event(condition, state));
    }

    public void execute(ServletContext servletContext, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        FlowContext context = new FlowContext(this, servletContext, req, resp);

        for (Event event : events) {
            if (event.isAdaptable(context)) {
                event.execute(context);
                break;
            }
        }
    }

}