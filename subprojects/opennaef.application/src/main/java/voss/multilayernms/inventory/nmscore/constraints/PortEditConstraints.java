package voss.multilayernms.inventory.nmscore.constraints;

import naef.dto.HardPortDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.vlan.VlanIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.renderer.PortRenderer;


public class PortEditConstraints {

    private static final Logger log = LoggerFactory.getLogger(PortEditConstraints.class);

    public static Boolean isPortSetIpEnabled(PortDto port) {
        if (NodeRenderer.getNodeType(port.getNode()) != null) {
            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("SSG140")) {
                if (port instanceof HardPortDto) {
                    return Boolean.TRUE;
                } else if (port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else if (port instanceof VlanIfDto) {
                    return Boolean.TRUE;
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("FGT_1240B")) {
                if (port instanceof HardPortDto) {
                    return Boolean.TRUE;
                } else if (port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else if (port instanceof VlanIfDto) {
                    return Boolean.TRUE;
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("FGT_1000C")) {
                if (port instanceof HardPortDto) {
                    return Boolean.TRUE;
                } else if (port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else if (port instanceof VlanIfDto) {
                    return Boolean.TRUE;
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("FGT_310B")) {
                if (port instanceof HardPortDto) {
                    return Boolean.TRUE;
                } else if (port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else if (port instanceof VlanIfDto) {
                    return Boolean.TRUE;
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("FGT_300C")) {
                if (port instanceof HardPortDto) {
                    return Boolean.TRUE;
                } else if (port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else if (port instanceof VlanIfDto) {
                    return Boolean.TRUE;
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("WS-C2960-48TT-L")) {
                if (port instanceof HardPortDto) {
                    return Boolean.TRUE;
                } else if (port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else if (port instanceof VlanIfDto) {
                    if (PortRenderer.getSviEnabled(port)) {
                        return Boolean.TRUE;
                    } else {
                        return Boolean.TRUE;
                    }
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("WS-C2950T-24")) {
                if (port instanceof HardPortDto) {
                    return Boolean.TRUE;
                } else if (port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else if (port instanceof VlanIfDto) {
                    if (PortRenderer.getSviEnabled(port)) {
                        return Boolean.TRUE;
                    } else {
                        return Boolean.TRUE;
                    }
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("WS-C3560E-48TD-S")) {
                if (port instanceof HardPortDto) {
                    return Boolean.TRUE;
                } else if (port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else if (port instanceof VlanIfDto) {
                    if (PortRenderer.getSviEnabled(port)) {
                        return Boolean.TRUE;
                    } else {
                        return Boolean.TRUE;
                    }
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("WS-C3560X-48T-L")) {
                if (port instanceof HardPortDto) {
                    return Boolean.TRUE;
                } else if (port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else if (port instanceof VlanIfDto) {
                    if (PortRenderer.getSviEnabled(port)) {
                        return Boolean.TRUE;
                    } else {
                        return Boolean.TRUE;
                    }
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("WS-C3560X-48T-S")) {
                if (port instanceof HardPortDto) {
                    return Boolean.TRUE;
                } else if (port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else if (port instanceof VlanIfDto) {
                    if (PortRenderer.getSviEnabled(port)) {
                        return Boolean.TRUE;
                    } else {
                        return Boolean.TRUE;
                    }
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("WS-C3750X-48T-S")) {
                if (port instanceof HardPortDto) {
                    return Boolean.TRUE;
                } else if (port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else if (port instanceof VlanIfDto) {
                    if (PortRenderer.getSviEnabled(port)) {
                        return Boolean.TRUE;
                    } else {
                        return Boolean.TRUE;
                    }
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("Nexus 5596")) {
                if (port instanceof HardPortDto) {
                    return Boolean.TRUE;
                } else if (port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else if (port instanceof VlanIfDto) {
                    if (PortRenderer.getSviEnabled(port)) {
                        return Boolean.TRUE;
                    } else {
                        return Boolean.TRUE;
                    }
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("BladeNetwork Switch")) {
                if (port instanceof HardPortDto) {
                    return Boolean.FALSE;
                } else if (port instanceof EthLagIfDto) {
                    return Boolean.FALSE;
                } else if (port instanceof VlanIfDto) {
                    if (PortRenderer.getSviEnabled(port)) {
                        return Boolean.TRUE;
                    } else {
                        return Boolean.FALSE;
                    }
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("VMware vSwitch")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("VMware VM")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("")) {
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    public static Boolean isPortSetIpEnabled(NodeDto node) {
        if (NodeRenderer.getNodeType(node) != null) {
            if (NodeRenderer.getNodeType(node).contentEquals("SSG140")) {
                return Boolean.TRUE;
            }
            if (NodeRenderer.getNodeType(node).contentEquals("FGT_1240B")) {
                return Boolean.TRUE;
            }
            if (NodeRenderer.getNodeType(node).contentEquals("FGT_1000C")) {
                return Boolean.TRUE;
            }
            if (NodeRenderer.getNodeType(node).contentEquals("FGT_310B")) {
                return Boolean.TRUE;
            }
            if (NodeRenderer.getNodeType(node).contentEquals("FGT_300C")) {
                return Boolean.TRUE;
            }
            if (NodeRenderer.getNodeType(node).contentEquals("WS-C2950T-24")) {
                return Boolean.TRUE;
            }
            if (NodeRenderer.getNodeType(node).contentEquals("WS-C2960-48TT-L")) {
                return Boolean.TRUE;
            }
            if (NodeRenderer.getNodeType(node).contentEquals("WS-C3560E-48TD-S")) {
                return Boolean.TRUE;
            }
            if (NodeRenderer.getNodeType(node).contentEquals("WS-C3560X-48T-L")) {
                return Boolean.TRUE;
            }
            if (NodeRenderer.getNodeType(node).contentEquals("WS-C3560X-48T-S")) {
                return Boolean.TRUE;
            }
            if (NodeRenderer.getNodeType(node).contentEquals("WS-C3750X-48T-S")) {
                return Boolean.TRUE;
            }
            if (NodeRenderer.getNodeType(node).contentEquals("Nexus 5596")) {
                return Boolean.TRUE;
            }
            if (NodeRenderer.getNodeType(node).contentEquals("BladeNetwork Switch")) {
                return Boolean.FALSE;
            }
            if (NodeRenderer.getNodeType(node).contentEquals("VMware vSwitch")) {
                return Boolean.FALSE;
            }
            if (NodeRenderer.getNodeType(node).contentEquals("VMware VM")) {
                return Boolean.FALSE;
            }
            if (NodeRenderer.getNodeType(node).contentEquals("")) {
                return Boolean.FALSE;
            }
        } else if (NodeRenderer.getNodeType(node) == null) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }


    public static Boolean isPortSetConfigNameEnabled(PortDto port) {
        if (NodeRenderer.getNodeType(port.getNode()) != null) {
            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("SSG140")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("FGT_1240B")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("FGT_1000C")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("FGT_310B")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("FGT_300C")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("WS-C2950T-24")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("WS-C2960-48TT-L")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("WS-C3560E-48TD-S")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("WS-C3560X-48T-L")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("WS-C3560X-48T-S")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("WS-C3750X-48T-S")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("Nexus 5596")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("BladeNetwork Switch")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("VMware vSwitch")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("VMware VM")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("")) {
                return Boolean.FALSE;
            }
        }
        return Boolean.FALSE;
    }

    public static Boolean isPortDirectLine(PortDto port) {
        if (NodeRenderer.getNodeType(port.getNode()) != null) {
            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("SSG140")) {
                String nodeName = NodeRenderer.getNodeName(port.getNode());
                int ret = Integer.parseInt(nodeName.replaceAll("[^0-9]", ""));
                if (ret > 0) {
                    return isOdd(ret);
                }
            }
            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("FGT_1240B") ||
                    NodeRenderer.getNodeType(port.getNode()).contentEquals("FGT_1000C") ||
                    NodeRenderer.getNodeType(port.getNode()).contentEquals("FGT_310B") ||
                    NodeRenderer.getNodeType(port.getNode()).contentEquals("FGT_300C")
                    ) {
                String nodeName = NodeRenderer.getNodeName(port.getNode());
                int ret = Integer.parseInt(nodeName.replaceAll("[^0-9]", ""));
                if (ret > 0) {
                    return isOdd(ret);
                }
            }
        }
        return Boolean.FALSE;
    }

    private static Boolean isOdd(int ret) {
        if (ret % 2 == 0) {
            return Boolean.FALSE;
        } else {
            return Boolean.TRUE;
        }
    }

    public static Boolean isPortSetVlanIdEnabled(PortDto port) {
        if (NodeRenderer.getNodeType(port.getNode()) != null) {
            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("SSG140")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("FGT_1240B")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("FGT_1000C")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("FGT_310B")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("FGT_300C")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("WS-C2950T-24")) {
                if (port instanceof EthPortDto || port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("WS-C2960-48TT-L")) {
                if (port instanceof EthPortDto || port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("WS-C3560E-48TD-S")) {
                if (port instanceof EthPortDto || port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("WS-C3560X-48T-L")) {
                if (port instanceof EthPortDto || port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("WS-C3560X-48T-S")) {
                if (port instanceof EthPortDto || port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("WS-C3750X-48T-S")) {
                if (port instanceof EthPortDto || port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("Nexus 5596")) {
                if (port instanceof EthPortDto || port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("BladeNetwork Switch")) {
                if (port instanceof EthPortDto || port instanceof EthLagIfDto) {
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("VMware vSwitch")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("VMware VM")) {
                return Boolean.FALSE;
            }

            if (NodeRenderer.getNodeType(port.getNode()).contentEquals("")) {
                return Boolean.FALSE;
            }
        }
        return Boolean.FALSE;
    }
}