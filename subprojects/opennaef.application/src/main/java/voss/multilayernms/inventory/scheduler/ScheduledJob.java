package voss.multilayernms.inventory.scheduler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.MplsNmsLogCategory;

public class ScheduledJob extends Thread {
    private final ScheduledLogic logic;
    private boolean running = false;

    public ScheduledJob(ScheduledLogic logic) {
        this.logic = logic;
    }

    public boolean isRunning() {
        return this.running;
    }

    public void begin() {
        if (this.running) {
            return;
        }
        this.running = true;
        run();
    }

    public void end() {
        if (!this.running) {
            return;
        }
        interrupt();
        this.running = false;
    }

    public void interrupt() {
        if (this.logic != null) {
            this.logic.interrupt();
        }
        super.interrupt();
    }

    @Override
    public void run() {
        Logger log = LoggerFactory.getLogger(MplsNmsLogCategory.LOG_DEBUG);
        log.info("begin execution.");
        this.running = true;
        try {
            logic.execute();
            log.info("execution success.");
        } catch (Exception e) {
            log.error("execution failed.", e);
        } finally {
            log.info("execution completed.");
            this.running = false;
        }
    }

}