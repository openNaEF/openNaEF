package voss.multilayernms.inventory.web.mpls;

import naef.dto.PathHopDto;
import naef.dto.PortDto;
import naef.dto.mpls.*;
import naef.dto.vlan.VlanIdPoolDto;
import org.apache.wicket.PageParameters;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.Util;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.nms.inventory.util.VlanUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MplsPageUtil {
    public static final String PARAM_POOL_NAME = "poolName";

    public static PageParameters getRsvpLspPoolParam(RsvpLspIdPoolDto pool) {
        if (pool == null) {
            throw new IllegalArgumentException("pool is null.");
        }
        PageParameters param = new PageParameters();
        param.add(PARAM_POOL_NAME, Util.encodeUTF8(pool.getName()));
        return param;
    }

    public static PageParameters getRsvpLspPathPoolParam(RsvpLspHopSeriesIdPoolDto pool) {
        if (pool == null) {
            throw new IllegalArgumentException("pool is null.");
        }
        PageParameters param = new PageParameters();
        param.add(PARAM_POOL_NAME, Util.encodeUTF8(pool.getName()));
        return param;
    }

    public static PageParameters getVlanIdPoolParam(VlanIdPoolDto pool) {
        if (pool == null) {
            throw new IllegalArgumentException("pool is null.");
        }
        PageParameters param = new PageParameters();
        param.add(PARAM_POOL_NAME, Util.encodeUTF8(pool.getName()));
        return param;
    }

    public static VlanIdPoolDto getVlanIdPool(PageParameters param) throws InventoryException, IOException {
        String poolName = param.getString(PARAM_POOL_NAME);
        poolName = Util.decodeUTF8(poolName);
        List<VlanIdPoolDto> pools = new ArrayList<VlanIdPoolDto>(VlanUtil.getPools());
        for (VlanIdPoolDto pool : pools) {
            if (poolName == null || poolName.length() == 0) {
                if (Util.stringToNull(pool.getName()) == null) {
                    return pool;
                }
            } else {
                if (pool.getName() != null && pool.getName().equals(poolName)) {
                    return pool;
                }
            }
        }
        return null;
    }

    public static PageParameters getPseudoWirePoolParam(PseudowireLongIdPoolDto pool) {
        if (pool == null) {
            throw new IllegalArgumentException("pool is null.");
        }
        PageParameters param = new PageParameters();
        param.add(PARAM_POOL_NAME, Util.encodeUTF8(pool.getName()));
        return param;
    }

    public static RsvpLspIdPoolDto getRsvpLspPool(PageParameters param) throws ExternalServiceException, IOException {
        String poolName = param.getString(PARAM_POOL_NAME);
        poolName = Util.decodeUTF8(poolName);
        if (poolName == null || poolName.length() == 0) {
            List<RsvpLspIdPoolDto> pools = MplsNmsInventoryConnector.getInstance().getRsvpLspIdPool();
            for (RsvpLspIdPoolDto pool : pools) {
                if (Util.stringToNull(pool.getName()) == null) {
                    return pool;
                }
            }
            return null;
        } else {
            return MplsNmsInventoryConnector.getInstance().getRsvpLspIdPool(poolName);
        }
    }

    public static RsvpLspHopSeriesIdPoolDto getRsvpLspPathPool(PageParameters param) throws ExternalServiceException, IOException {
        String poolName = param.getString(PARAM_POOL_NAME);
        poolName = Util.decodeUTF8(poolName);
        List<RsvpLspHopSeriesIdPoolDto> pools = MplsNmsInventoryConnector.getInstance().getRsvpLspHopSeriesIdPool();
        for (RsvpLspHopSeriesIdPoolDto pool : pools) {
            if (poolName == null || poolName.length() == 0) {
                if (Util.stringToNull(pool.getName()) == null) {
                    return pool;
                }
            } else {
                if (pool.getName() != null && pool.getName().equals(poolName)) {
                    return pool;
                }
            }
        }
        return null;
    }

    public static PseudowireLongIdPoolDto getPseudowireIdPool(PageParameters param) throws ExternalServiceException, IOException {
        String poolName = param.getString(PARAM_POOL_NAME);
        poolName = Util.decodeUTF8(poolName);
        List<PseudowireLongIdPoolDto> pools = MplsNmsInventoryConnector.getInstance().getPseudoWireLongIdPools();
        if (pools != null) {
            for (PseudowireLongIdPoolDto pool : pools) {
                if (poolName == null || poolName.length() == 0) {
                    if (Util.stringToNull(pool.getName()) == null) {
                        return pool;
                    }
                } else {
                    if (pool.getName() != null && pool.getName().equals(poolName)) {
                        return pool;
                    }
                }
            }
        }
        return null;
    }

    public static PseudowireStringIdPoolDto getPseudowireStringIdPool(PageParameters param) throws ExternalServiceException, IOException {
        String poolName = param.getString(PARAM_POOL_NAME);
        List<PseudowireStringIdPoolDto> pools2 = MplsNmsInventoryConnector.getInstance().getPseudoWireStringIdPools();
        if (pools2 != null) {
            for (PseudowireStringIdPoolDto pool : pools2) {
                if (poolName == null || poolName.length() == 0) {
                    if (Util.stringToNull(pool.getName()) == null) {
                        return pool;
                    }
                } else {
                    if (pool.getName() != null && pool.getName().equals(poolName)) {
                        return pool;
                    }
                }
            }
        }
        return null;
    }

    public static boolean containsNode(RsvpLspIdPoolDto pool, String nodeName) {
        if (pool == null) {
            throw new IllegalArgumentException();
        } else if (nodeName == null) {
            return false;
        }
        for (RsvpLspDto user : pool.getUsers()) {
            RsvpLspHopSeriesDto path = user.getActiveHopSeries();
            for (PathHopDto hop : path.getHops()) {
                PortDto from = hop.getSrcPort();
                if (Util.equals(from.getNode().getName().toLowerCase(), nodeName.toLowerCase())) {
                    return true;
                }
                PortDto to = hop.getDstPort();
                if (Util.equals(to.getNode().getName().toLowerCase(), nodeName.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean containsId(RsvpLspIdPoolDto pool, String id) {
        if (id == null) {
            return false;
        } else if (pool == null) {
            throw new IllegalArgumentException();
        }
        try {
            for (RsvpLspDto lsp : pool.getUsers()) {
                if (lsp.getName().contains(id)) {
                    return true;
                }
            }
            return false;
        } catch (NumberFormatException e) {
        }
        return false;
    }

}