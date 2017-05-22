package voss.nms.inventory.diff.network.diffunitbuilder;

import naef.dto.NodeDto;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.model.Device;
import voss.model.Port;
import voss.nms.inventory.diff.DiffSet;
import voss.nms.inventory.diff.network.DiffPolicy;
import voss.nms.inventory.diff.network.IpAddressDB;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class VirtualNodeDiffUnitBuilder extends AbstractNodeDiffUnitBuilder<Device> {

    public VirtualNodeDiffUnitBuilder(DiffSet set, DiffPolicy policy, IpAddressDB ipDB,
                                      Map<Port, String> portAbsoluteNameMap, String userName) {
        super(set, policy, ipDB, portAbsoluteNameMap, true, false, userName);
    }

    protected Map<String, Device> toDeviceMap(Collection<Device> virtualDevices) {
        Map<String, Device> result = new HashMap<String, Device>();
        for (Device virtualDevice : virtualDevices) {
            String id = AbsoluteNameFactory.getNodeName(virtualDevice);
            if (id == null) {
                continue;
            }
            result.put(id, virtualDevice);
        }
        return result;
    }

    protected Map<String, NodeDto> toNodeMap(Collection<NodeDto> nodes) {
        Map<String, NodeDto> result = new HashMap<String, NodeDto>();
        for (NodeDto node : nodes) {
            String id = getNodeId(node);
            if (id == null) {
                continue;
            }
            result.put(node.getName(), node);
        }
        return result;
    }
}