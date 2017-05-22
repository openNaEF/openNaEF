package opennaef.notifier;

import opennaef.notifier.util.ISO8601;
import opennaef.notifier.util.Logs;
import tef.TransactionId;
import tef.skelton.dto.DtoChangeListener;
import tef.skelton.dto.DtoChanges;
import tef.skelton.dto.EntityDto;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NaEFからDtoChangesを受け取る
 */
public class NaefSubscriber extends UnicastRemoteObject implements DtoChangeListener {
    public static final String LISTENER_NAME = "opennaef-notifier";
    private static transient NaefSubscriber _instance;
    private transient List<NotifyListener> _listeners = new CopyOnWriteArrayList<>();

    private NaefSubscriber() throws RemoteException {
        super();
    }

    public static NaefSubscriber instance() throws RemoteException {
        if (_instance == null) {
            _instance = new NaefSubscriber();
        }
        return NaefSubscriber._instance;
    }

    @Override
    public void transactionCommitted(
            final TransactionId txId,
            final Set<EntityDto> newObjects,
            final Set<EntityDto> changedObjects,
            final DtoChanges dtoChanges
    ) throws RemoteException {
        Logs.naef.info(
                "dto-changes event. [{}] time = {}, new = {}, changed = {}",
                txId.getIdString(),
                ISO8601.format(dtoChanges.getTargetTime()),
                newObjects.size(),
                changedObjects.size());

        _listeners.parallelStream()
                .forEach(l -> l.transactionCommitted(txId, dtoChanges));
    }

    public NotifyListener addListener(NotifyListener listener) {
        if (_listeners.contains(listener)) {
            return listener;
        }
        boolean added = _listeners.add(listener);
        return added ? listener : null;
    }

    public NotifyListener removeListener(NotifyListener listener) {
        boolean removed = _listeners.remove(listener);
        return removed ? listener : null;
    }
}
