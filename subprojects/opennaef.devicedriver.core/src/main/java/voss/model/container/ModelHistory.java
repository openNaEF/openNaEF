package voss.model.container;


import voss.model.VlanModel;

import java.util.*;


public class ModelHistory {
    private final String id;
    private final Map<HistoryTag, VlanModel> history = Collections.synchronizedMap(new HashMap<HistoryTag, VlanModel>());

    public ModelHistory(String id) {
        this.id = id;
    }

    public String getID() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o instanceof ModelHistory) {
            return ((ModelHistory) o).id.equals(this.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (this.getClass().getCanonicalName() + ":" + this.id).hashCode();
    }

    public synchronized void addHistory(VlanModel model) {
        HistoryTag tag = new HistoryTag();
        this.history.put(tag, model);
    }

    public synchronized void addHistory(VlanModel model, String desc) {
        HistoryTag tag = new HistoryTag(desc);
        this.history.put(tag, model);
    }

    public synchronized void addHistory(VlanModel model, long time) {
        HistoryTag tag = new HistoryTag(time);
        this.history.put(tag, model);
    }

    public synchronized void addHistory(VlanModel model, long time, String desc) {
        HistoryTag tag = new HistoryTag(time, desc);
        this.history.put(tag, model);
    }

    public synchronized List<HistoryTag> getHistory() {
        List<HistoryTag> result = new ArrayList<HistoryTag>();
        result.addAll(this.history.keySet());
        Collections.sort(result, HistoryTag.getComparator());
        return result;
    }

    public synchronized VlanModel getModel(HistoryTag tag) {
        return this.history.get(tag);
    }

    public synchronized VlanModel getRecentModel() {
        List<HistoryTag> tags = getHistory();
        HistoryTag recent = tags.get(tags.size() - 1);
        return this.history.get(recent);
    }

    public synchronized HistoryTag getRecentHistoryTag() {
        List<HistoryTag> tags = getHistory();
        HistoryTag recent = tags.get(tags.size() - 1);
        return recent;
    }

    public synchronized void reset() {
        this.history.clear();
    }
}