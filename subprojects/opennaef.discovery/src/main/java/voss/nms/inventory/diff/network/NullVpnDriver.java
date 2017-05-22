package voss.nms.inventory.diff.network;

import voss.model.VlanModel;

public class NullVpnDriver implements VpnDriver {

    @Override
    public String getVpnPrefix(VlanModel model) {
        return null;
    }

}