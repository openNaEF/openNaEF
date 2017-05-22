package voss.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("serial")
public class EAConverter extends AbstractEthernetSwitch {

    public EAConverter() {
    }

    public AtmVlanBridge[] getAtmVlanBridges() {
        return selectPorts(AtmVlanBridge.class);
    }

    public AtmPhysicalPort[] getAtmPhysicalPorts() {
        return selectPorts(AtmPhysicalPort.class);
    }

    public AtmVp[] getVps() {
        return selectPorts(AtmVp.class);
    }

    public AtmPvc[] getPvcs() {
        return selectPorts(AtmPvc.class);
    }

    public AtmVlanBridge getAtmVlanBridgeByBridgePortNumber(int bridgePortNumber) {
        AtmVlanBridge result = null;
        for (AtmVlanBridge bridge : getAtmVlanBridges()) {
            if (bridge.getBridgePortNumber() != null
                    && bridge.getBridgePortNumber().intValue() == bridgePortNumber) {
                if (result != null) {
                    throw new IllegalStateException
                            ("duplicated bridge port number: "
                                    + getDeviceName() + " " + bridgePortNumber);
                } else {
                    result = bridge;
                }
            }
        }
        return result;
    }

    public AtmVlanBridge getAtmVlanBridge(AtmPvc pvc) {
        AtmVlanBridge result = null;

        AtmVlanBridge[] bridges = getAtmVlanBridges();
        for (int i = 0; i < bridges.length; i++) {
            AtmVlanBridge bridge = bridges[i];
            if (bridge.getPvc() == pvc) {
                if (result != null) {
                    throw new IllegalStateException
                            ("multiple bridges: " + pvc.getFullyQualifiedName());
                }

                result = bridge;
            }
        }

        return result;
    }

    public Set<AtmVlanBridge> getUntaggedAtmVlanBridges(VlanIf vlanif) {
        Set<AtmVlanBridge> result = new HashSet<AtmVlanBridge>();
        for (AtmVlanBridge bridge : getAtmVlanBridges()) {
            if (bridge.getUntaggedVlanIf() == vlanif) {
                result.add(bridge);
            }
        }
        return result;
    }

    public Set<AtmVlanBridge> getTaggedAtmVlanBridges(VlanIf vlanif) {
        Set<AtmVlanBridge> result = new HashSet<AtmVlanBridge>();
        for (AtmVlanBridge bridge : getAtmVlanBridges()) {
            if (Arrays.asList(bridge.getTaggedVlanIfs()).contains(vlanif)) {
                result.add(bridge);
            }
        }
        return result;
    }

    public AtmVlanBridge[] getAtmVlanBridges(VlanIf vlanif) {
        Set<AtmVlanBridge> result = new HashSet<AtmVlanBridge>();
        result.addAll(getUntaggedAtmVlanBridges(vlanif));
        result.addAll(getTaggedAtmVlanBridges(vlanif));
        return result.toArray(new AtmVlanBridge[0]);
    }

    public AtmPvc[] getPvcs(AtmVp vp) {
        Set<AtmPvc> result = new HashSet<AtmPvc>();

        AtmPvc[] pvcs = getPvcs();
        for (int i = 0; i < pvcs.length; i++) {
            AtmPvc pvc = pvcs[i];
            if (pvc.getVp() == vp) {
                result.add(pvc);
            }
        }

        return (AtmPvc[]) result.toArray(new AtmPvc[0]);
    }
}