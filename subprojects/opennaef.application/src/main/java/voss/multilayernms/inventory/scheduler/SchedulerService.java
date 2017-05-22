package voss.multilayernms.inventory.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.MplsNmsLogCategory;
import voss.multilayernms.inventory.config.MplsNmsConfiguration;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class SchedulerService {
    private static final Logger log = LoggerFactory.getLogger(MplsNmsLogCategory.LOG_DEBUG);
    private static SchedulerService instance = null;

    public static SchedulerService getInstance() {
        if (instance == null) {
            instance = new SchedulerService();
        }
        return instance;
    }

    private int poolSize = 2;
    private ScheduledJob rttJob;
    private ScheduledJob operStatusJob;
    private ScheduledThreadPoolExecutor executor;
    private ScheduledFuture<?> rttFuture;
    private ScheduledFuture<?> operStatusFuture;

    private SchedulerService() {
    }

    public synchronized void startService() {
        if (this.executor != null) {
            return;
        }
        this.executor = new ScheduledThreadPoolExecutor(this.poolSize);
        try {
            MplsNmsConfiguration config = MplsNmsConfiguration.getInstance();
        } catch (Exception e) {
            log.error("cannot start scheduler-service.", e);
            stopService();
        }
    }

    private long getInitialDelay(Date baseTime, int interval) {
        long initialDelay = 0L;
        long current = System.currentTimeMillis();
        if (baseTime.getTime() > current) {
            initialDelay = baseTime.getTime() - current;
        } else if (baseTime.getTime() < current) {
            long diff = current - baseTime.getTime();
            long intervalMill = (long) interval * 60L * 1000L;
            long remains = diff % (intervalMill);
            if (remains != 0) {
                initialDelay = intervalMill - remains;
            }
        }
        return initialDelay / 1000L / 60L;
    }

    public synchronized void stopService() {
        if (this.executor != null) {
            this.executor.shutdownNow();
        }
        this.executor = null;
        this.rttJob = null;
        this.operStatusJob = null;
    }

    public synchronized boolean isServicing() {
        return this.executor != null;
    }

    public synchronized ScheduledJob getRttJob() {
        return this.rttJob;
    }

    public synchronized ScheduledFuture<?> getRttFuture() {
        return this.rttFuture;
    }

    public synchronized ScheduledJob getOperStatusJob() {
        return this.operStatusJob;
    }

    public synchronized ScheduledFuture<?> getOperStatusFuture() {
        return this.operStatusFuture;
    }
}