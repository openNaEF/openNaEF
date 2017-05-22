package voss.multilayernms.inventory.web.vrf;

import naef.dto.IdRange;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.vrf.VrfDto;
import naef.dto.vrf.VrfIfDto;
import naef.dto.vrf.VrfIntegerIdPoolDto;
import naef.dto.vrf.VrfStringIdPoolDto;
import org.apache.wicket.PageParameters;
import voss.core.server.database.ATTR;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.Util;
import voss.nms.inventory.util.NodeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VrfWebUtil {
    public static final String KEY_POOL_ID = "pool_id";
    public static final String KEY_VRF_ID = "vrf_id";

    public static VrfDto getVrf(VrfStringIdPoolDto pool, String id) throws InventoryException {
        if (pool == null) {
            throw new IllegalArgumentException();
        } else if (id == null) {
            throw new IllegalArgumentException();
        }
        for (VrfDto vrf : pool.getUsers()) {
            if (vrf.getStringId().equals(id)) {
                return vrf;
            }
        }
        return null;
    }

    private static List<VrfIntegerIdPoolDto> getChildren(VrfIntegerIdPoolDto pool) {
        List<VrfIntegerIdPoolDto> result = new ArrayList<VrfIntegerIdPoolDto>();
        if (pool == null) {
            return result;
        }
        result.add(pool);
        Set<VrfIntegerIdPoolDto> children = pool.getChildren();
        if (children != null) {
            for (VrfIntegerIdPoolDto child : children) {
                result.addAll(getChildren(child));
            }
        }
        return result;
    }

    @SuppressWarnings("unused")
    private static List<VrfIntegerIdPoolDto> getChildrenOnly(VrfIntegerIdPoolDto pool) {
        List<VrfIntegerIdPoolDto> result = new ArrayList<VrfIntegerIdPoolDto>();
        if (pool == null) {
            return result;
        }
        Set<VrfIntegerIdPoolDto> children = pool.getChildren();
        if (children != null) {
            for (VrfIntegerIdPoolDto child : children) {
                result.addAll(getChildren(child));
            }
        }
        return result;
    }

    public static List<VrfIfDto> getMemberVrfIfs(VrfDto vrf) {
        List<VrfIfDto> result = new ArrayList<VrfIfDto>();
        for (VrfIfDto vrfIf : vrf.getMemberVrfifs()) {
            result.add(vrfIf);
        }
        return result;
    }

    public static List<VrfIfDto> getVrfIfs(NodeDto node) {
        List<VrfIfDto> result = new ArrayList<VrfIfDto>();
        for (PortDto port : node.getPorts()) {
            if (port instanceof VrfIfDto) {
                VrfIfDto vrfIf = (VrfIfDto) port;
                if (DtoUtil.mvoEquals(vrfIf.getNode(), node)) {
                    result.add(vrfIf);
                }
            }
        }
        return result;
    }

    public static VrfIfDto getVrfIf(NodeDto node, VrfDto vrf) {
        for (VrfIfDto vrfIf : vrf.getMemberVrfifs()) {
            if (NodeUtil.isSameNode(vrfIf, node)) {
                return vrfIf;
            }
        }
        return null;
    }

    public static VrfIfDto getOrphanedVrfIf(NodeDto node, VrfDto vrf) {
        for (VrfIfDto vrfIf : getVrfIfs(node)) {
            if (vrfIf.getName().equals(getVrfIfName(vrf))) {
                return vrfIf;
            }
        }
        return null;
    }

    public static List<PortDto> getAttachedPorts(VrfDto vrf) {
        List<PortDto> result = new ArrayList<PortDto>();
        if (vrf == null) {
            return result;
        }
        Set<VrfIfDto> members = vrf.getMemberVrfifs();
        for (VrfIfDto member : members) {
            result.addAll(getAttachedPorts(member));
        }
        return result;
    }

    public static List<PortDto> getAttachedPorts(VrfIfDto vrfIf) {
        List<PortDto> result = new ArrayList<PortDto>();
        if (vrfIf == null) {
            return result;
        }
        for (PortDto port : vrfIf.getAttachedPorts()) {
            result.add(port);
        }
        return result;
    }

    public static PageParameters getParameters(VrfIntegerIdPoolDto pool) {
        PageParameters param = new PageParameters();
        param.add(KEY_POOL_ID, Util.encodeUTF8(pool.getName()));
        return param;
    }

    public static PageParameters getVrfParam(VrfIntegerIdPoolDto pool, VrfDto vrf) {
        PageParameters param = new PageParameters();
        param.add(KEY_POOL_ID, Util.encodeUTF8(pool.getName()));
        param.add(KEY_VRF_ID, vrf.getStringId());
        return param;
    }

    public static PageParameters getVrfParam(VrfIntegerIdPoolDto pool, Integer id) {
        PageParameters param = new PageParameters();
        param.add(KEY_POOL_ID, Util.encodeUTF8(pool.getName()));
        param.add(KEY_VRF_ID, id.toString());
        return param;
    }


    public static long getTotalNumberOfIds(VrfIntegerIdPoolDto pool) {
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

    public static String getVrfIfName(VrfDto vrf) {
        return "vrf" + vrf.getStringId();
    }

    public static String getVrfIfFqn(NodeDto node, VrfDto vrf) {
        return node.getName() + ATTR.NAME_DELIMITER_PRIMARY + vrf.getStringId();
    }

}