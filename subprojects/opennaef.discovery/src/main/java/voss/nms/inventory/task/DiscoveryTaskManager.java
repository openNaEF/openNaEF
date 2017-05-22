package voss.nms.inventory.task;

import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.config.InventoryConfiguration;
import voss.nms.inventory.diff.network.DiscoveryResult;
import voss.nms.inventory.diff.network.DiscoveryTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DiscoveryTaskManager {

    private static DiscoveryTaskManager instance = null;

    public static DiscoveryTaskManager getInstance() {
        if (instance == null) {
            instance = new DiscoveryTaskManager();
        }
        return instance;
    }

    private int maxThreadSize = 10;
    private ExecutorService threadPool;

    private DiscoveryTaskManager() {
        try {
            this.maxThreadSize = InventoryConfiguration.getInstance().getDiscoveryMaxTask();
            this.threadPool = Executors.newFixedThreadPool(maxThreadSize);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    protected void configUpdated() {
    }

    public Future<DiscoveryResult> addTask(DiscoveryTask worker) {
        Future<DiscoveryResult> future = this.threadPool.submit(worker);
        return future;
    }

    public void abortTask(long threadID) {

    }


}