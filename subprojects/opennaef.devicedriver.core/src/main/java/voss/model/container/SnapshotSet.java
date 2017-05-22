package voss.model.container;


import voss.model.NodeInfoRef;
import voss.model.VlanModel;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class SnapshotSet implements Serializable {
    private final long id;
    private final long revision;
    private final long timestamp;
    private final Map<NodeInfoRef, VlanModel> targets = new HashMap<NodeInfoRef, VlanModel>();

    public SnapshotSet(long id, long revision) {
        this.id = id;
        this.revision = revision;
        this.timestamp = System.currentTimeMillis();
    }

    public synchronized long getID() {
        return this.id;
    }

    public synchronized long getRevision() {
        return this.revision;
    }

    public synchronized long getTimestamp() {
        return this.timestamp;
    }

    public synchronized void addSnapshot(NodeInfoRef node, VlanModel model) {
        this.targets.put(node, model);
    }

    public synchronized void removeSnapshot(NodeInfoRef id) {
        this.targets.remove(id);
    }

    public synchronized Map<NodeInfoRef, VlanModel> getSnapshots() {
        Map<NodeInfoRef, VlanModel> result = new HashMap<NodeInfoRef, VlanModel>();
        result.putAll(this.targets);
        return Collections.unmodifiableMap(result);
    }

    public synchronized VlanModel getSnapshot(NodeInfoRef node) {
        return this.targets.get(node);
    }
}