package voss.model;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AlaxalaQosFlowProfile implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String qosFlowName;
    private final String qosFlowKey;
    private final List<Port> qosFlowAppliedPorts = new ArrayList<Port>();
    private final List<VlanIf> qosFlowAppliedVlanIfs = new ArrayList<VlanIf>();

    public AlaxalaQosFlowProfile() {
        qosFlowName = null;
        qosFlowKey = null;
    }

    public AlaxalaQosFlowProfile(String flowName, String key) {
        this.qosFlowName = flowName;
        this.qosFlowKey = key;
    }

    public synchronized void addPort(Port _port) {
        if (_port == null || this.qosFlowAppliedPorts.contains(_port)) {
            return;
        }
        qosFlowAppliedPorts.add(_port);
    }

    public synchronized void addVlanIf(VlanIf vlan) {
        if (vlan == null || this.qosFlowAppliedVlanIfs.contains(vlan)) {
            return;
        }
        qosFlowAppliedVlanIfs.add(vlan);
    }

    public String getQosFlowProfileName() {
        return qosFlowName;
    }

    public String getQosFlowProfileKey() {
        return qosFlowKey;
    }

    public Port[] getQosFlowAppliedPorts() {
        return qosFlowAppliedPorts.toArray(new Port[this.qosFlowAppliedPorts.size()]);
    }

    public VlanIf[] getQosFlowAppliedVlanIfs() {
        return qosFlowAppliedVlanIfs.toArray(new VlanIf[this.qosFlowAppliedVlanIfs.size()]);
    }

    public String toString() {
        String result = this.qosFlowName + " (Key=[" + this.qosFlowKey + "])\r\n";
        result = result + "Ports:" + "\r\n";
        for (Port port : getQosFlowAppliedPorts()) {
            result = result + "\t" + port.getIfName() + "\r\n";
        }
        result = result + "Vlans:" + "\r\n";
        for (VlanIf vlan : getQosFlowAppliedVlanIfs()) {
            result = result + "\t" + vlan.getVlanId() + " (" + vlan.getVlanName() + ")\r\n";
        }
        return result;
    }

}