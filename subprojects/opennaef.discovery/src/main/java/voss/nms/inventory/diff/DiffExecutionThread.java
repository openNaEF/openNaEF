package voss.nms.inventory.diff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.InventoryException;
import voss.nms.inventory.constants.LogConstants;

import java.io.IOException;
import java.util.List;

public abstract class DiffExecutionThread extends Thread {
    private final Logger log = LoggerFactory.getLogger(LogConstants.DIFF_SERVICE);
    private final DiffCategory category;
    private final DiffSetManagerImpl manager;
    private final List<ExecutionTask> tasks;
    private boolean isRunning = false;

    public DiffExecutionThread(DiffSetManagerImpl manager, DiffCategory category) {
        this.manager = manager;
        this.category = category;
        this.tasks = getExecutionTask();
    }

    public void run() {
        boolean success = true;
        try {
            doPreProcess();
            doExecute();
        } catch (Exception e) {
            warn("error while processing task.", e);
            success = false;
        } finally {
            try {
                doPostProcess(success);
            } catch (Exception e) {
                warn("got exception in Thread.", e);
            }
        }
    }

    abstract protected void doPreProcess() throws IOException, InventoryException;

    abstract protected void doPostProcess(boolean success) throws IOException, InventoryException;

    abstract protected void doExecute() throws IOException, InventoryException;

    public DiffCategory getCategory() {
        return this.category;
    }

    protected DiffSetManagerImpl getManager() {
        return this.manager;
    }

    public void setRunning(boolean value) {
        this.isRunning = value;
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    protected String getLogString(String s) {
        return Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ") " + s;
    }

    protected void error(String s) {
        log.error(getLogString(s));
    }

    protected void error(String s, Throwable t) {
        log.error(getLogString(s), t);
    }

    protected void warn(String s) {
        log.warn(getLogString(s));
    }

    protected void warn(String s, Throwable t) {
        log.warn(getLogString(s), t);
    }

    protected void info(String s) {
        log.info(getLogString(s));
    }

    protected void info(String s, Throwable t) {
        log.info(getLogString(s), t);
    }

    protected void debug(String s) {
        log.debug(getLogString(s));
    }

    protected void debug(String s, Throwable t) {
        log.debug(getLogString(s), t);
    }

    protected void trace(String s) {
        log.trace(getLogString(s));
    }

    protected void trace(String s, Throwable t) {
        log.trace(getLogString(s), t);
    }

    private List<ExecutionTask> getExecutionTask() {
        return tasks;
    }

    public static interface ExecutionTask {
        String getTaskName();
    }

}