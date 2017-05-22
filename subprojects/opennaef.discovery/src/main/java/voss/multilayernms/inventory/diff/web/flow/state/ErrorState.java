package voss.multilayernms.inventory.diff.web.flow.state;

import voss.multilayernms.inventory.diff.web.ResponseCodeException;
import voss.multilayernms.inventory.diff.web.flow.FlowContext;

import javax.servlet.ServletException;


public class ErrorState implements State {

    private final StateId id;
    private final int statusCode;
    private final String message;

    public ErrorState(StateId id, int statusCode, String message) {
        this.id = id;
        this.statusCode = statusCode;
        this.message = message;
    }

    public StateId getStateId() {
        return id;
    }

    public void execute(FlowContext context) throws ServletException                  {
        throw new ResponseCodeException(statusCode, message);
    }

}