package tef.skelton.shell;

import tef.MVO;
import tef.skelton.Model;
import tef.skelton.VariableHolder;
import tef.ui.shell.ShellConnection;
import tef.ui.shell.ShellSession;

import java.util.HashMap;
import java.util.Map;

public class SkeltonShellSession extends ShellSession implements VariableHolder {

    private Model context_;
    private String promptSuffix_;
    private final Map<String, Object> variables_ = new HashMap<String, Object>();

    public SkeltonShellSession(ShellConnection connection, lib38k.logger.Logger logger) {
        super(connection, logger);
    }

    @Override protected void batchPostprocess() {
        context_ = null;
    }

    @Override protected synchronized String getPromptBody() {
        return super.getPromptBody()
            + (context_ instanceof MVO
                ? " " + ((MVO) context_).getMvoId().getLocalStringExpression()
                : "")
            + (promptSuffix_ == null ? "" : " " + promptSuffix_);
    }

    public synchronized void setContext(Model model, String promptSuffix) {
        context_ = model;
        promptSuffix_ = promptSuffix;
    }

    public synchronized Model getContext() {
        return context_;
    }

    public synchronized String getPromptSuffix() {
        return promptSuffix_;
    }

    @Override public synchronized void setVariable(String key, Object value) {
        variables_.put(key, value);
    }

    @Override public synchronized Object getVariable(String key) {
        return variables_.get(key);
    }
}
