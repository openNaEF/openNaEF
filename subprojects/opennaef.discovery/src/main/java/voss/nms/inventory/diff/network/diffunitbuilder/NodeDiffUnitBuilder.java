package voss.nms.inventory.diff.network.diffunitbuilder;

import naef.dto.NodeDto;
import voss.model.Device;
import voss.model.Port;
import voss.nms.inventory.diff.DiffSet;
import voss.nms.inventory.diff.network.DiffPolicy;
import voss.nms.inventory.diff.network.IpAddressDB;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class NodeDiffUnitBuilder extends AbstractNodeDiffUnitBuilder<Device> {

    public NodeDiffUnitBuilder(DiffSet set, DiffPolicy policy, IpAddressDB ipDB,
                               Map<Port, String> portAbsoluteNameMap, boolean forVMware, String userName) {
        super(set, policy, ipDB, portAbsoluteNameMap, false, false, userName);
    }

    @Override
    protected Map<String, Device> toDeviceMap(Collection<Device> devices) {
        Map<String, Device> result = new HashMap<String, Device>();
        for (Device device : devices) {
            result.put(device.getDeviceName(), device);
        }
        return result;
    }

    @Override
    protected Map<String, NodeDto> toNodeMap(Collection<NodeDto> nodes) {
        Map<String, NodeDto> result = new HashMap<String, NodeDto>();
        for (NodeDto node : nodes) {
            result.put(node.getName(), node);
        }
        return result;
    }
}