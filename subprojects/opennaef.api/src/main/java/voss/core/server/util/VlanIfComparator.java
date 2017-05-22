package voss.core.server.util;

import naef.dto.NodeDto;
import naef.dto.vlan.VlanIfDto;

import java.io.Serializable;
import java.util.Comparator;

public class VlanIfComparator implements Comparator<VlanIfDto>, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(VlanIfDto o1, VlanIfDto o2) {
        NodeDto node1 = o1.getNode();
        NodeDto node2 = o2.getNode();
        int r = node1.getName().compareTo(node2.getName());
        if (r != 0) {
            return r;
        }
        return o1.getName().compareTo(o2.getName());
    }

}