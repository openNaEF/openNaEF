package voss.core.server.util;

import naef.dto.NetworkDto;
import naef.dto.NodeDto;
import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.PseudowireLongIdPoolDto;
import naef.dto.mpls.PseudowireStringIdPoolDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

public class PseudoWireUtil {
    private static final Logger log = LoggerFactory.getLogger(PseudoWireUtil.class);
    public static final String KEY_POOL_ID = "pool_id";
    public static final String KEY_PSEUDOWIRE_ID = "pw_id";

    public static PseudowireLongIdPoolDto getPool(String name) throws InventoryException,
            ExternalServiceException, RemoteException, IOException {
        return InventoryConnector.getInstance().getPseudoWireLongIdPool(name);
    }

    public static PseudowireStringIdPoolDto getStringPool(String name) throws InventoryException,
            ExternalServiceException, RemoteException, IOException {
        return InventoryConnector.getInstance().getPseudoWireStringIdPool(name);
    }

    public static boolean isNodeRelatedPw(NodeDto node, PseudowireDto pw) {
        return isSameNode(pw.getAc1(), node) || isSameNode(pw.getAc2(), node);
    }

    public static PortDto getAcOnMe(PseudowireDto pw, NodeDto me) {
        if (!isNodeRelatedPw(me, pw)) {
            return null;
        }
        if (isSameNode(pw.getAc1(), me)) {
            return pw.getAc1();
        } else if (isSameNode(pw.getAc2(), me)) {
            return pw.getAc2();
        }
        return null;
    }

    public static PortDto getAcOnRemote(PseudowireDto pw, NodeDto me) {
        if (!isNodeRelatedPw(me, pw)) {
            return null;
        }
        if (!isSameNode(pw.getAc1(), me)) {
            return pw.getAc2();
        } else if (!isSameNode(pw.getAc2(), me)) {
            return pw.getAc1();
        }
        return null;
    }

    private static boolean isSameNode(PortDto port, NodeDto node) {
        if (port == null) {
            return false;
        } else if (node == null) {
            return false;
        }
        return port.getNode().getName().equals(node.getName());
    }

    public static PseudowireDto getPseudoWire(PseudowireLongIdPoolDto pool, String id) {
        if (pool == null) {
            log.error("pool is null.");
            return null;
        }
        for (PseudowireDto dto : pool.getUsers()) {
            if (dto.getLongId().toString().equals(id)) {
                return dto;
            }
        }
        log.debug("pseudoWire not found: " + pool.getName() + ":" + id);
        return null;
    }

    public static PseudowireDto getPseudoWire(PseudowireStringIdPoolDto pool, String id) {
        if (pool == null) {
            log.error("pool is null.");
            return null;
        }
        for (PseudowireDto dto : pool.getUsers()) {
            if (dto.getStringId().toString().equals(id)) {
                return dto;
            }
        }
        log.debug("pseudoWire not found: " + pool.getName() + ":" + id);
        return null;
    }

    public static List<PseudowireDto> getPseudoWiresOn(NodeDto node) throws
            InventoryException, ExternalServiceException, IOException, AuthenticationException {
        if (node == null) {
            return new ArrayList<PseudowireDto>();
        }
        InventoryConnector conn = InventoryConnector.getInstance();
        Set<PseudowireDto> pseudoWires = conn.getDtoFacade().getPseudowires(node);
        List<PseudowireDto> result = new ArrayList<PseudowireDto>();
        if (pseudoWires != null) {
            result.addAll(pseudoWires);
        }
        return result;
    }

    public static List<PseudowireDto> getPseudWiresOn(PortDto port) {
        List<PseudowireDto> result = new ArrayList<PseudowireDto>();
        for (NetworkDto network : port.getNetworks()) {
            if (network instanceof PseudowireDto) {
                result.add((PseudowireDto) network);
            }
        }
        return result;
    }

    public static PseudowireDto getPseudoWireByName(String poolName, Long id) throws
            InventoryException, ExternalServiceException, RemoteException, IOException {
        if (id == null) {
            throw new IllegalStateException("vc id is null.");
        } else if (poolName == null) {
            throw new IllegalStateException("poolName is null.");
        }
        try {
            PseudowireLongIdPoolDto pool = InventoryConnector.getInstance().getPseudoWireLongIdPool(poolName);
            if (pool == null) {
                throw new InventoryException("PseudoWire ID pool does not exist. [" + poolName + "]");
            }
            for (PseudowireDto pseudoWire : pool.getUsers()) {
                if (pseudoWire.getLongId().equals(id)) {
                    return pseudoWire;
                }
            }
            return null;
        } catch (RemoteException e) {
            throw ExceptionUtils.getExternalServiceException(e);
        } catch (IOException e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static PseudowireDto getPseudoWireByName(String poolName, String id) throws
            InventoryException, ExternalServiceException, RemoteException, IOException {
        if (id == null) {
            throw new IllegalStateException("vc id is null.");
        } else if (poolName == null) {
            throw new IllegalStateException("poolName is null.");
        }
        try {
            PseudowireStringIdPoolDto pool = InventoryConnector.getInstance().getPseudoWireStringIdPool(poolName);
            if (pool == null) {
                throw new InventoryException("PseudoWire ID pool does not exist. [" + poolName + "]");
            }
            for (PseudowireDto pseudoWire : pool.getUsers()) {
                if (pseudoWire.getStringId().equals(id)) {
                    return pseudoWire;
                }
            }
            return null;
        } catch (RemoteException e) {
            throw ExceptionUtils.getExternalServiceException(e);
        } catch (IOException e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static Map<String, PseudowireDto> getPseudowires() throws InventoryException, ExternalServiceException {
        Map<String, PseudowireDto> pseudowireAcs = new HashMap<String, PseudowireDto>();
        try {
            for (PseudowireDto pw : getPseudoWires()) {
                if (pw.getAc1() != null) {
                    pseudowireAcs.put(DtoUtil.getMvoId(pw.getAc1()).toString(), pw);
                }
                if (pw.getAc2() != null) {
                    pseudowireAcs.put(DtoUtil.getMvoId(pw.getAc2()).toString(), pw);
                }
            }
            return pseudowireAcs;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static Set<PseudowireDto> getPseudoWires() throws InventoryException, ExternalServiceException {
        Set<PseudowireDto> pseudowires = new HashSet<PseudowireDto>();
        try {
            InventoryConnector conn = InventoryConnector.getInstance();
            List<PseudowireLongIdPoolDto> pools = conn.getPseudoWireLongIdPools();
            if (pools != null) {
                for (PseudowireLongIdPoolDto pool : pools) {
                    pseudowires.addAll(pool.getUsers());
                }
            }
            List<PseudowireStringIdPoolDto> pools2 = conn.getPseudoWireStringIdPools();
            if (pools2 != null) {
                for (PseudowireStringIdPoolDto pool : pools2) {
                    pseudowires.addAll(pool.getUsers());
                }
            }
            return pseudowires;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static Map<String, PseudowireDto> getPseudowires(String poolName) throws InventoryException,
            ExternalServiceException {
        Map<String, PseudowireDto> pseudowireAcs = new HashMap<String, PseudowireDto>();
        try {
            InventoryConnector conn = InventoryConnector.getInstance();
            PseudowireLongIdPoolDto pool = conn.getPseudoWireLongIdPool(poolName);
            if (pool == null) {
                return pseudowireAcs;
            }
            for (PseudowireDto pw : pool.getUsers()) {
                if (pw.getAc1() != null) {
                    pseudowireAcs.put(DtoUtil.getMvoId(pw.getAc1()).toString(), pw);
                }
                if (pw.getAc2() != null) {
                    pseudowireAcs.put(DtoUtil.getMvoId(pw.getAc2()).toString(), pw);
                }
            }
            return pseudowireAcs;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static Map<String, PseudowireDto> getStringPseudowires(String poolName) throws InventoryException,
            ExternalServiceException {
        Map<String, PseudowireDto> pseudowireAcs = new HashMap<String, PseudowireDto>();
        try {
            InventoryConnector conn = InventoryConnector.getInstance();
            PseudowireStringIdPoolDto pool = conn.getPseudoWireStringIdPool(poolName);
            if (pool == null) {
                return pseudowireAcs;
            }
            for (PseudowireDto pw : pool.getUsers()) {
                if (pw.getAc1() != null) {
                    pseudowireAcs.put(DtoUtil.getMvoId(pw.getAc1()).toString(), pw);
                }
                if (pw.getAc2() != null) {
                    pseudowireAcs.put(DtoUtil.getMvoId(pw.getAc2()).toString(), pw);
                }
            }
            return pseudowireAcs;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static Map<NodeElementDto, PseudowireDto> getPseudowires(List<NodeElementDto> nodeElements)
            throws InventoryException, ExternalServiceException {
        Map<NodeElementDto, PseudowireDto> result = new HashMap<NodeElementDto, PseudowireDto>();
        Map<String, PseudowireDto> pseudowireAcs = getPseudowires();
        for (NodeElementDto nodeElement : nodeElements) {
            PseudowireDto pw = pseudowireAcs.get(DtoUtil.getMvoId(nodeElement).toString());
            if (pw != null) {
                result.put(nodeElement, pw);
            }
        }
        return result;
    }

    public static String getInterworking(PseudowireDto pw) {
        if (pw == null) {
            return null;
        }
        if (pw.getAc1() != null && pw.getAc2() != null) {
            String type1 = pw.getAc1().getObjectTypeName();
            String type2 = pw.getAc2().getObjectTypeName();
            if (!type1.equals(type2)) {
                if (type1.startsWith("eth") || type2.startsWith("eth")) {
                    return "ethernet";
                } else {
                    return "ip";
                }
            }
        }
        return null;
    }

    public static String getPseudoWireType(PseudowireDto target) {
        return DtoUtil.getStringOrNull(target, MPLSNMS_ATTR.PSEUDOWIRE_TYPE);
    }

    public static String getPseudoWireId(PseudowireDto pw) {
        if (pw == null) {
            return null;
        } else if (pw.getLongId() != null) {
            return pw.getLongId().toString();
        } else if (pw.getStringId() != null) {
            return pw.getStringId();
        }
        return null;
    }
}