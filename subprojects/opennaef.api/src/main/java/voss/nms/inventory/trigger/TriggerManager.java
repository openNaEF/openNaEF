package voss.nms.inventory.trigger;

import naef.dto.NaefDto;
import naef.ui.NaefDtoFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.TransactionId;
import tef.skelton.dto.DtoChangeListener;
import tef.skelton.dto.DtoChanges;
import tef.skelton.dto.EntityDto;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.Util;
import voss.nms.inventory.config.InventoryConfiguration;
import voss.nms.inventory.constants.LogConstants;
import voss.nms.inventory.database.InventoryConnector;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("serial")
public class TriggerManager extends UnicastRemoteObject implements DtoChangeListener {
    private final Logger log = LoggerFactory.getLogger(LogConstants.TRIGGER_SERVICE);
    public static final String LISTENER_NAME = "trigger-manager";

    private static TriggerManager instance = null;

    public static TriggerManager getInstance() {
        if (instance == null) {
            try {
                String id = InventoryConfiguration.getInstance().getTriggerManagerID();
                int port = InventoryConfiguration.getInstance().getInventoryServicePort();
                instance = new TriggerManager(id, port);
            } catch (IOException e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }
        return instance;
    }

    private final String triggerListenerID;
    private boolean running = false;
    private ExecutorService pool = null;
    private final List<TriggerService> services = new ArrayList<TriggerService>();

    private TriggerManager(String id, int port) throws RemoteException {
        super(port);
        if (id == null) {
            throw new IllegalArgumentException("trigger listener id is missing.");
        }
        this.triggerListenerID = id;
        log.info("trigger-manager started on TCP/" + port + ".");
    }

    public synchronized void start() {
        if (this.running) {
            return;
        }
        log.info("start trigger-manager.");
        if (this.pool != null) {
            try {
                this.pool.shutdownNow();
                this.pool = null;
            } catch (Exception e) {
            }
        }
        this.pool = Executors.newFixedThreadPool(1);
        try {
            NaefDtoFacade facade = InventoryConnector.getInstance().getDtoFacade();
            facade.addDtoChangeListener(this.triggerListenerID, this);
        } catch (Exception e) {
            throw new IllegalStateException("failed to add trigger-manager as dto-changes-listener.", e);
        }
        this.running = true;
    }

    public synchronized void stop() {
        if (!this.running) {
            return;
        }
        log.info("stop trigger-manager.");
        this.running = false;
        try {
            this.pool.shutdownNow();
        } finally {
            this.pool = null;
        }
    }

    public synchronized void register(TriggerService service) {
        log.info("register: " + service.getServiceName());
        this.services.add(service);
    }

    public synchronized void unregister(TriggerService service) {
        log.info("unregister: " + service.getServiceName());
        service.stop();
        this.services.remove(service);
    }

    public synchronized void clearServices() {
        for (TriggerService service : this.services) {
            try {
                service.stop();
            } catch (Exception e) {
                log.debug("got exception on stop:" + service.getServiceName(), e);
            }
        }
        this.services.clear();
    }

    public synchronized List<TriggerService> getTriggerServices() {
        List<TriggerService> services = new ArrayList<TriggerService>();
        services.addAll(this.services);
        return services;
    }

    @Override
    public void transactionCommitted(TransactionId id, Set<EntityDto> newObjects,
                                     Set<EntityDto> changedObjects, DtoChanges changes) throws RemoteException {
        if (!this.running) {
            return;
        }
        TriggerDistributor th = new TriggerDistributor(getTriggerServices(), id, newObjects, changedObjects, changes);
        th.start();
    }

    public static class TriggerDistributor extends Thread {
        private final Logger log = LoggerFactory.getLogger(LogConstants.TRIGGER_SERVICE);
        private final List<TriggerService> services;
        private final TransactionId id;
        private final Set<EntityDto> newObjects;
        private final Set<EntityDto> changedObjects;
        private final DtoChanges changes;

        public TriggerDistributor(List<TriggerService> services, TransactionId id, Set<EntityDto> newObjects,
                                  Set<EntityDto> changedObjects, DtoChanges changes) {
            this.services = services;
            this.id = id;
            this.newObjects = newObjects;
            this.changedObjects = changedObjects;
            this.changes = changes;
        }

        public void run() {
            dump();
            for (TriggerService service : this.services) {
                try {
                    service.update(this.id, this.newObjects, this.changedObjects, this.changes);
                } catch (Throwable th) {
                    log.warn("failed to notify trigger: " + service.getServiceName(), th);
                }
            }
        }

        private void dump() {
            if (!this.log.isDebugEnabled()) {
                return;
            }
            log.debug("dto-changes: new-object: " + newObjects.size() + ", changed-object: " + this.changedObjects.size());
            for (EntityDto newObject : this.newObjects) {
                log.debug("new: " + DtoUtil.getAbsoluteName(newObject));
            }
            for (EntityDto changedObject : this.changedObjects) {
                logChanges(changedObject);
            }
        }

        private void logChanges(EntityDto changedObject) {
            log.debug("changed: " + DtoUtil.getAbsoluteName(changedObject));
            Set<String> changedAttributes = this.changes.getChangedAttributeNames(changedObject);
            for (String changedAttribute : changedAttributes) {
                Object pre = this.changes.getPreChangeValue(changedObject, changedAttribute);
                Object now = changedObject.getValue(changedAttribute);
                int level = 0;
                if (log.isTraceEnabled()) {
                    level = 1;
                }
                log.debug("- " + changedAttribute + " " + logChanges(pre, now, level));
                DtoChanges.CollectionChange collectionchange = this.changes.getCollectionChange(changedObject, changedAttribute);
                if (collectionchange.getAddedValues().size() > 0 || collectionchange.getRemovedValues().size() > 0) {
                    log.debug("\tadded: " + collectionchange.getAddedValues().size());
                    for (Object o : collectionchange.getAddedValues()) {
                        log.debug("\t+ " + DtoUtil.toStringOrMvoString(o));
                    }
                    log.debug("\tremoved: " + collectionchange.getRemovedValues().size());
                    for (Object o : collectionchange.getRemovedValues()) {
                        log.debug("\t+ " + DtoUtil.toStringOrMvoString(o));
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        private String logChanges(Object pre, Object now, int level) {
            if (Collection.class.isInstance(pre)) {
                Collection<Object> pres = (Collection<Object>) pre;
                Collection<Object> nows = (Collection<Object>) now;
                StringBuilder sb = new StringBuilder();
                logCollectionChanges(sb, level, pres, nows);
                return sb.toString();
            } else if (NaefDto.class.isInstance(pre) || NaefDto.class.isInstance(now)) {
                if (DtoUtil.mvoEquals((NaefDto) pre, (NaefDto) now)) {
                    if (level == 0) {
                        return "";
                    } else {
                        return "[" + DtoUtil.toStringOrMvoString(now) + "]";
                    }
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Changes:[");
                    sb.append(DtoUtil.toStringOrMvoString(pre));
                    sb.append("]->[");
                    sb.append(DtoUtil.toStringOrMvoString(now));
                    sb.append("]");
                    return sb.toString();
                }
            } else {
                if (Util.equals(pre, now)) {
                    if (level == 0) {
                        return "";
                    } else {
                        return DtoUtil.toStringOrMvoString(now);
                    }
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Changes:[");
                    sb.append(DtoUtil.toStringOrMvoString(pre));
                    sb.append("]->[");
                    sb.append(DtoUtil.toStringOrMvoString(now));
                    sb.append("]");
                    return sb.toString();
                }
            }
        }

        private void logCollectionChanges(StringBuilder sb, int level, Collection<Object> pre, Collection<Object> current) {
            List<Object> added = Util.getAddedList(pre, current);
            List<Object> removed = Util.getRemovedList(pre, current);
            int total = added.size() + removed.size();
            sb.append("Changed(").append(total).append("):{");
            for (Object add : added) {
                sb.append("+");
                sb.append(DtoUtil.toStringOrMvoString(add, null));
                sb.append(", ");
            }
            for (Object remove : removed) {
                sb.append("-");
                sb.append(DtoUtil.toStringOrMvoString(remove, null));
                sb.append(", ");
            }
            sb.append("}");
            if (level == 0) {
                return;
            }
            sb.append(" Retained:[");
            List<?> commons = Util.getCommonList(pre, current);
            for (Object common : commons) {
                sb.append(DtoUtil.toStringOrMvoString(common, null));
                sb.append(", ");
            }
            sb.append("]");
        }
    }
}