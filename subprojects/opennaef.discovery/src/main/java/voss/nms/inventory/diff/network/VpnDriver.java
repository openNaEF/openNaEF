package voss.nms.inventory.diff.network;

import voss.model.VlanModel;

public interface VpnDriver {
    String getVpnPrefix(VlanModel model);
}