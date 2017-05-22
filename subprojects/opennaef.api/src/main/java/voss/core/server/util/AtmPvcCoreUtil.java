package voss.core.server.util;

import naef.dto.NodeDto;
import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import naef.dto.atm.AtmApsIfDto;
import naef.dto.atm.AtmPortDto;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.atm.AtmPvpIfDto;
import naef.dto.pos.PosApsIfDto;
import naef.dto.pos.PosPortDto;
import naef.dto.serial.SerialPortDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ATTR;
import voss.core.server.database.CoreConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AtmPvcCoreUtil {
    public static final String KEY_ATM_PORT = "atm";
    public static final String KEY_ATM_PVC_VPI = "vpi";
    public static final String KEY_ATM_PVC_VCI = "vci";

    public static boolean isAtmPvcEnabled(PortDto port) {
        if (port == null) {
            return false;
        }
        return DtoUtil.getStringOrNull(port, ATTR.FEATURE_ATM_PVC) != null;
    }

    private static Logger log() {
        return LoggerFactory.getLogger(AtmPvcCoreUtil.class);
    }

    public static AtmPvcIfDto getAtmPvc(String nodeName, String atmIfName, int vpi, int vci)
            throws InventoryException, ExternalServiceException {
        NodeDto node = NodeUtil.getNode(nodeName);
        PortDto atmPort = NodeUtil.getPortByIfName(node, atmIfName);
        if (atmPort == null) {
            return null;
        } else if (!isAtmCapablePort(atmPort)) {
            return null;
        }
        for (NodeElementDto sub : atmPort.getSubElements()) {
            if (!(sub instanceof AtmPvpIfDto)) {
                continue;
            }
            AtmPvpIfDto vp = (AtmPvpIfDto) sub;
            for (NodeElementDto sub2 : vp.getSubElements()) {
                if (!(sub2 instanceof AtmPvcIfDto)) {
                    continue;
                }
                AtmPvcIfDto pvc = (AtmPvcIfDto) sub2;
                if (pvc.getVpi() != null && pvc.getVpi().intValue() == vpi
                        && pvc.getVci() != null && pvc.getVci().intValue() == vci) {
                    return pvc;
                }
            }
        }
        return null;
    }

    public static PortDto getAtmPort(PortDto port) {
        if (port == null) {
            return null;
        }
        if (port instanceof AtmPvcIfDto) {
            AtmPvcIfDto pvc = (AtmPvcIfDto) port;
            return pvc.getPhysicalPort();
        } else if (port instanceof AtmPvpIfDto) {
            AtmPvpIfDto pvp = (AtmPvpIfDto) port;
            return pvp.getPhysicalPort();
        } else {
            return null;
        }
    }

    public static List<PortDto> getAtmPorts(NodeDto node)
            throws InventoryException {
        if (node == null) {
            try {
                List<PortDto> result = new ArrayList<PortDto>();
                CoreConnector conn = CoreConnector.getInstance();
                for (NodeDto aNode : conn.getNodes()) {
                    result.addAll(getAtmPortsOn(aNode));
                }
                return result;
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        } else {
            return getAtmPortsOn(node);
        }
    }

    public static List<PortDto> getAtmPortsOn(NodeDto node) {
        List<PortDto> result = new ArrayList<PortDto>();
        if (node == null) {

        } else {
            for (PortDto port : node.getPorts()) {
                if (!isAtmCapablePort(port)) {
                    continue;
                }
                if (port.getValue(ATTR.FEATURE_ATM_PVC) == null) {
                    continue;
                }
                result.add(port);
            }
        }
        return result;
    }

    public static AtmPvpIfDto getPvp(PortDto atmPort, int vpi) throws InventoryException, ExternalServiceException {
        if (atmPort == null) {
            return null;
        }
        try {
            Set<NodeElementDto> subElements = atmPort.getSubElements();
            for (NodeElementDto sub : subElements) {
                if (sub instanceof AtmPvpIfDto) {
                    AtmPvpIfDto pvp = (AtmPvpIfDto) sub;
                    Integer vpi_ = pvp.getVpi();
                    if (vpi_ == null) {
                        log().warn("no vpi on AtmPvpIfDto:" + pvp.getAbsoluteName());
                        continue;
                    }
                    if (vpi_.intValue() == vpi) {
                        return pvp;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static AtmPvcIfDto getPvc(AtmPvpIfDto vp, int vci) {
        if (vp == null) {
            return null;
        }
        for (NodeElementDto sub : vp.getSubElements()) {
            if (!(sub instanceof AtmPvcIfDto)) {
                continue;
            }
            AtmPvcIfDto pvc = (AtmPvcIfDto) sub;
            if (pvc.getVci() == null) {
                log().warn("no vci on pvc: " + pvc.getAbsoluteName());
                continue;
            } else if (pvc.getVci().intValue() == vci) {
                return pvc;
            }
        }
        return null;
    }

    public static List<AtmPvcIfDto> getAtmPvcIfs(NodeDto node)
            throws InventoryException {
        if (node == null) {
            try {
                List<AtmPvcIfDto> result = new ArrayList<AtmPvcIfDto>();
                CoreConnector conn = CoreConnector.getInstance();
                for (NodeDto aNode : conn.getNodes()) {
                    result.addAll(getAtmPvcIfsOn(aNode));
                }
                return result;
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        } else {
            return getAtmPvcIfsOn(node);
        }
    }

    public static List<AtmPvcIfDto> getAtmPvcIfsOn(NodeDto node) {
        List<AtmPvcIfDto> result = new ArrayList<AtmPvcIfDto>();
        for (PortDto port : node.getPorts()) {
            if (port instanceof AtmPvcIfDto) {
                result.add((AtmPvcIfDto) port);
            }
        }
        return result;
    }

    public static List<AtmPvcIfDto> getAtmPvcIfs(PortDto port) {
        List<AtmPvcIfDto> result = new ArrayList<AtmPvcIfDto>();
        if (port == null) {
            return result;
        }
        if (port instanceof AtmPvcIfDto) {
            result.add((AtmPvcIfDto) port);
            return result;
        } else if (port instanceof AtmPvpIfDto) {
            for (NodeElementDto p : port.getSubElements()) {
                if (p instanceof AtmPvcIfDto) {
                    result.add((AtmPvcIfDto) p);
                }
            }
            return result;
        } else if (port instanceof PortDto) {
            if (port.getValue(ATTR.FEATURE_ATM_PVC) == null) {
                return result;
            }
            for (NodeElementDto p : port.getSubElements()) {
                if (p instanceof AtmPvcIfDto) {
                    result.add((AtmPvcIfDto) p);
                } else if (p instanceof AtmPvpIfDto) {
                    result.addAll(getAtmPvcIfs((AtmPvpIfDto) p));
                }
            }
            return result;
        }
        return result;
    }

    public static boolean isAtmCapablePort(PortDto port) {
        if (port == null) {
            return false;
        }
        if (port instanceof AtmPortDto) {
            return true;
        } else if (port instanceof AtmApsIfDto) {
            return true;
        } else if (port instanceof PosPortDto) {
            return true;
        } else if (port instanceof PosApsIfDto) {
            return true;
        } else if (port instanceof SerialPortDto) {
            return true;
        }
        return false;
    }

    public static boolean isAtmCapablePort(String portType) {
        if (portType == null) {
            return false;
        }
        if (portType.equals(ATTR.TYPE_ATM_PORT)) {
            return true;
        } else if (portType.equals(ATTR.TYPE_ATM_APS_PORT)) {
            return true;
        } else if (portType.equals(ATTR.TYPE_POS_PORT)) {
            return true;
        } else if (portType.equals(ATTR.TYPE_POS_APS_PORT)) {
            return true;
        } else if (portType.equals(ATTR.TYPE_SERIAL_PORT)) {
            return true;
        }
        return false;
    }
}