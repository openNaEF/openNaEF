package voss.multilayernms.inventory.diff.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.diff.util.ConfigUtil;
import voss.nms.inventory.diff.DiffCategory;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class RegularExecution {
    private static final Logger log = LoggerFactory.getLogger(RegularExecution.class);
    private static final RegularExecution instance = new RegularExecution();

    public static RegularExecution getInstance() {
        return instance;
    }

    private Timer timer;
    private DiscoveryTask discoveryTask;

    private RegularExecution() {
        timer = new Timer(true);
        discoveryTask = new DiscoveryTask();
    }

    public void reScheduleAll() {
        ConfigUtil util = ConfigUtil.getInstance();
        Date discoveryStartDate = util.getDate(ConfigUtil.KEY_DISCOVERY_REGULAR_EXECUTION_TIME);
        int discoveryInterval = util.getPlusInt(ConfigUtil.KEY_DISCOVERY_REGULAR_EXECUTION_INTERVAL);
        if (discoveryStartDate != null && discoveryInterval > 0) {
            reScheduleDiscovery(discoveryStartDate, discoveryInterval);
        } else if (discoveryInterval == 0) {
            log.info("[NW] regular execution disabled.");
        } else {
            log.warn("[NW] unexpected config.");
        }
    }

    private Date nextTime(Date src, long period) {
        long next = src.getTime();
        long now = Calendar.getInstance().getTimeInMillis();
        while (now > next) {
            next += period;
        }
        return new Date(next);
    }

    public void reScheduleDiscovery(Date firstTime, int min) {
        discoveryTask.cancel();
        discoveryTask = new DiscoveryTask();
        long period = min * 60 * 1000;
        Date nextTime = nextTime(firstTime, period);
        log.debug("Discovery Task schedule[" + String.format("%1$tY/%1$tm/%1$td %tT", nextTime) + "] period[" + period + "]");
        timer.scheduleAtFixedRate(discoveryTask, nextTime, period);
    }

    public void cancel() {
        timer.cancel();
        timer = new Timer(true);
    }

    private class DiscoveryTask extends TimerTask {

        @Override
        public void run() {
            log.debug("DiscoveryTask run.");
            try {
                DiffSetManager.getInstance().start(DiffCategory.DISCOVERY);
            } catch (Exception e) {
                log.error("", e);
            }
        }

    }

}