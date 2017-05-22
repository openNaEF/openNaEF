package voss.model.container;


import voss.model.Description;
import voss.model.VlanModel;

import java.io.Serializable;
import java.util.*;

@SuppressWarnings("serial")
public class SnapShotInfo implements Serializable {
    private final Description description;
    private Map<String, String> nodeNames = new HashMap<String, String>();
    private Set<VlanModel> snapshotDevices = new HashSet<VlanModel>();
    private Set<VlanModel> baseDevices = new HashSet<VlanModel>();

    public SnapShotInfo(Description description) {
        assert description != null;
        this.description = description;
    }

    public Description getDescription() {
        return this.description;
    }

    public void addNodeIdAndNodeName(String id, String name) {
        this.nodeNames.put(id, name);
    }

    public Map<String, String> getNodeIdAndName() {
        return Collections.unmodifiableMap(this.nodeNames);
    }

    public void addSnapshotDevice(VlanModel d) {
        this.snapshotDevices.add(d);
    }

    public void addBaseDevice(VlanModel d) {
        this.baseDevices.add(d);
    }

    public Set<VlanModel> getSnapshotDevice() {
        HashSet<VlanModel> result = new HashSet<VlanModel>();
        result.addAll(this.snapshotDevices);
        return result;
    }

    public Set<VlanModel> getBaseDevices() {
        HashSet<VlanModel> result = new HashSet<VlanModel>();
        result.addAll(this.baseDevices);
        return result;
    }
}