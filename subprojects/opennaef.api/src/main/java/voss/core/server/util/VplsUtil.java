package voss.core.server.util;

import naef.dto.IdRange;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.vpls.VplsDto;
import naef.dto.vpls.VplsIfDto;
import naef.dto.vpls.VplsIntegerIdPoolDto;
import naef.dto.vpls.VplsStringIdPoolDto;
import voss.core.server.database.ATTR;
import voss.core.server.exception.InventoryException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VplsUtil {
    public static final String KEY_POOL_ID = "pool_id";
    public static final String KEY_VPLS_ID = "vpls_id";

    public static VplsDto getVpls(VplsStringIdPoolDto pool, String id) throws InventoryException {
        if (pool == null) {
            throw new IllegalArgumentException();
        } else if (id == null) {
            throw new IllegalArgumentException();
        }
        for (VplsDto vpls : pool.getUsers()) {
            if (vpls.getStringId().equals(id)) {
                return vpls;
            }
        }
        return null;
    }

    private static List<VplsIntegerIdPoolDto> getChildren(VplsIntegerIdPoolDto pool) {
        List<VplsIntegerIdPoolDto> result = new ArrayList<VplsIntegerIdPoolDto>();
        if (pool == null) {
            return result;
        }
        result.add(pool);
        Set<VplsIntegerIdPoolDto> children = pool.getChildren();
        if (children != null) {
            for (VplsIntegerIdPoolDto child : children) {
                result.addAll(getChildren(child));
            }
        }
        return result;
    }

    @SuppressWarnings("unused")
    private static List<VplsIntegerIdPoolDto> getChildrenOnly(VplsIntegerIdPoolDto pool) {
        List<VplsIntegerIdPoolDto> result = new ArrayList<VplsIntegerIdPoolDto>();
        if (pool == null) {
            return result;
        }
        Set<VplsIntegerIdPoolDto> children = pool.getChildren();
        if (children != null) {
            for (VplsIntegerIdPoolDto child : children) {
                result.addAll(getChildren(child));
            }
        }
        return result;
    }

    public static List<VplsIfDto> getMemberVplsIfs(VplsDto vpls) {
        List<VplsIfDto> result = new ArrayList<VplsIfDto>();
        for (VplsIfDto vplsIf : vpls.getMemberVplsifs()) {
            result.add(vplsIf);
        }
        return result;
    }

    public static List<VplsIfDto> getVplsIfs(NodeDto node) {
        List<VplsIfDto> result = new ArrayList<VplsIfDto>();
        for (PortDto port : node.getPorts()) {
            if (port instanceof VplsIfDto) {
                VplsIfDto vplsIf = (VplsIfDto) port;
                if (DtoUtil.mvoEquals(vplsIf.getNode(), node)) {
                    result.add(vplsIf);
                }
            }
        }
        return result;
    }

    public static List<PortDto> getAttachedPorts(VplsDto vpls) {
        List<PortDto> result = new ArrayList<PortDto>();
        if (vpls == null) {
            return result;
        }
        Set<VplsIfDto> members = vpls.getMemberVplsifs();
        for (VplsIfDto member : members) {
            result.addAll(getAttachedPorts(member));
        }
        return result;
    }

    public static List<PortDto> getAttachedPorts(VplsIfDto vplsIf) {
        List<PortDto> result = new ArrayList<PortDto>();
        for (PortDto port : vplsIf.getAttachedPorts()) {
            result.add(port);
        }
        return result;
    }

    public static VplsIfDto getVplsIf(NodeDto node, VplsDto vpls) {
        for (VplsIfDto vplsIf : vpls.getMemberVplsifs()) {
            if (NodeUtil.isSameNode(vplsIf, node)) {
                return vplsIf;
            }
        }
        return null;
    }

    public static VplsIfDto getOrphanedVplsIf(NodeDto node, VplsDto vpls) {
        for (VplsIfDto vplsIf : getVplsIfs(node)) {
            if (vplsIf.getName().equals(getVplsIfName(vpls))) {
                return vplsIf;
            }
        }
        return null;
    }


    public static long getTotalNumberOfIds(VplsIntegerIdPoolDto pool) {
        long total = 0;
        if (pool == null) {
            return total;
        }
        for (IdRange<Integer> range : pool.getIdRanges()) {
            int num = range.upperBound - range.lowerBound + 1;
            total = total + num;
        }
        return total;
    }

    public static String getVplsIfName(VplsDto vpls) {
        return "vpls" + vpls.getStringId();
    }

    public static String getVplsIfFqn(NodeDto node, VplsDto vpls) {
        return node.getName() + ATTR.NAME_DELIMITER_PRIMARY + vpls.getStringId();
    }

}