package voss.model;

import java.io.Serializable;
import java.util.*;


public class Description implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String description;
    private long timestamp;
    private boolean committed = false;
    private final Set<NodeInfoRef> nodeinforefs = new HashSet<NodeInfoRef>();
    private Map<String, String> notes = new HashMap<String, String>();

    public Description(String description, long timestamp) {
        this.description = description;
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return this.description;
    }

    public void addNodeInfo(NodeInfo nodeinfo) {
        assert nodeinfo != null;
        NodeInfoRef ref = NodeInfoRef.get(nodeinfo);
        nodeinforefs.add(ref);
    }

    public void addNodeInfo(NodeInfoRef ref) {
        assert ref != null;
        nodeinforefs.add(ref);
    }

    public Set<NodeInfoRef> getNodeInfoRefs() {
        Set<NodeInfoRef> result = new HashSet<NodeInfoRef>();
        result.addAll(this.nodeinforefs);
        return Collections.unmodifiableSet(result);
    }

    public NodeInfoRef getNodeInfoRef(String site, String nodeID) {
        for (NodeInfoRef ref : this.nodeinforefs) {
            if (ref.getSite().equals(site) && ref.getNodeIdentifier().equals(nodeID)) {
                return ref;
            }
        }
        return null;
    }

    public Calendar getTimestampAsCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(this.timestamp);
        return cal;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isCommitted() {
        return this.committed;
    }

    public void setCommitted() {
        this.committed = true;
    }

    public void setCommitted(boolean value) {
        this.committed = value;
    }

    public synchronized void addNotes(String key, String value) {
        this.notes.put(key, value);
    }

    public synchronized void removeNotes(String key) {
        this.notes.remove(key);
    }

    public synchronized Map<String, String> getNotes() {
        HashMap<String, String> result = new HashMap<String, String>();
        result.putAll(this.notes);
        return Collections.unmodifiableMap(result);
    }
}