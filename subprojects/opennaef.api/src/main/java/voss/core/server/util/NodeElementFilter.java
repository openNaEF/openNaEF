package voss.core.server.util;

import naef.dto.*;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.fr.FrPvcIfDto;
import naef.dto.ip.IpIfDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vpls.VplsDto;
import naef.dto.vpls.VplsIfDto;
import naef.dto.vrf.VrfDto;
import naef.dto.vrf.VrfIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.MVO.MvoId;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class NodeElementFilter implements Serializable {
    private static final long serialVersionUID = 1L;

    private final NaefDto dto;

    private final Set<MvoId> childrens = new HashSet<MvoId>();

    private boolean enableTarget = true;
    private boolean enablePool = false;
    private boolean enableHardware = false;
    private boolean enableHardPort = false;
    private boolean enableLag = false;
    private boolean enableRouterVlanIf = false;
    private boolean enableVlanIf = false;
    private boolean enableVplsIf = false;
    private boolean enableVrfIf = false;
    private boolean enableVlan = false;
    private boolean enableRsvpLsp = false;
    private boolean enablePseudoWire = false;
    private boolean enableVpls = false;
    private boolean enableVrf = false;
    private boolean enableAtmPvc = false;
    private boolean enableFrPvc = false;
    private boolean enableIpIf = false;
    private boolean enableLink = false;

    public NodeElementFilter(NaefDto dto) {
        this.dto = dto;
        if (dto instanceof NodeElementDto) {
            NodeUtil.populateHardware((NodeElementDto) dto, childrens);
        }
    }

    public boolean match(NaefDto element) {
        if (element == null) {
            return false;
        } else if (dto == null) {
            return false;
        }
        if (element instanceof NodeElementDto && dto instanceof NodeElementDto) {
            long time = System.currentTimeMillis();
            log().trace("element=" + element.getAbsoluteName());
            NodeElementDto ne1 = (NodeElementDto) element;
            NodeElementDto neBase = (NodeElementDto) dto;
            NodeElementDto upperElement = ne1;
            boolean hasParentChildRelationship = false;
            while (upperElement != null) {
                if (upperElement instanceof HardwareDto) {
                    hasParentChildRelationship = childrens.contains(DtoUtil.getMvoId(upperElement));
                    break;
                } else {
                    hasParentChildRelationship = DtoUtil.isSameMvoEntity(neBase, upperElement);
                    if (hasParentChildRelationship) {
                        break;
                    }
                    upperElement = upperElement.getOwner();
                }
            }
            log().trace("time=" + (System.currentTimeMillis() - time) + ", parent=" + (upperElement == null ? "" : upperElement.getAbsoluteName()));
            if (!hasParentChildRelationship) {
                return true;
            }
        }
        if (DtoUtil.mvoEquals(element, this.dto) && enableTarget) {
            return false;
        } else if (element instanceof VlanIfDto) {
            VlanIfDto vlanIf = (VlanIfDto) element;
            NodeElementDto owner = vlanIf.getOwner();
            if (owner != null && (owner instanceof PortDto)) {
                return !enableRouterVlanIf;
            } else if (owner != null && (owner instanceof NodeDto)) {
                return !enableVlanIf;
            } else if (owner == null) {
                throw new IllegalStateException("owner is null: " + vlanIf.getAbsoluteName());
            } else {
                throw new IllegalStateException("unknown pattern: " + vlanIf.getAbsoluteName() + " on " + owner.getAbsoluteName());
            }
        } else if (element instanceof VlanDto && !enableVlan) {
            return true;
        } else if (element instanceof VplsIfDto && !enableVplsIf) {
            return true;
        } else if (element instanceof VplsDto && !enableVpls) {
            return true;
        } else if (element instanceof VrfIfDto && !enableVrfIf) {
            return true;
        } else if (element instanceof VrfDto && !enableVrf) {
            return true;
        } else if (element instanceof PseudowireDto && !enablePseudoWire) {
            return true;
        } else if (element instanceof AtmPvcIfDto && !enableAtmPvc) {
            return true;
        } else if (element instanceof FrPvcIfDto && !enableFrPvc) {
            return true;
        } else if (element instanceof IpIfDto && !enableIpIf) {
            return true;
        } else if (element instanceof EthLagIfDto && !enableLag) {
            return true;
        } else if (element instanceof HardPortDto && !enableHardPort) {
            return true;
        } else if (element instanceof HardwareDto && !enableHardware) {
            return true;
        } else if (element instanceof IdPoolDto<?, ?, ?> && !enablePool) {
            return true;
        } else if (element instanceof LinkDto && !enableLink) {
            return true;
        }
        return false;
    }

    public boolean isEnableTarget() {
        return enableTarget;
    }

    public void setEnableTarget(boolean enableTarget) {
        this.enableTarget = enableTarget;
    }

    public boolean isEnablePool() {
        return enablePool;
    }

    public void setEnablePool(boolean enablePool) {
        this.enablePool = enablePool;
    }

    public boolean isEnableLag() {
        return enableLag;
    }

    public void setEnableLag(boolean enableLag) {
        this.enableLag = enableLag;
    }

    public boolean isEnableHardPort() {
        return enableHardPort;
    }

    public void setEnableHardPort(boolean enableHardPort) {
        this.enableHardPort = enableHardPort;
    }

    public boolean isEnableVlan() {
        return enableVlan;
    }

    public void setEnableVlan(boolean enableVlan) {
        this.enableVlan = enableVlan;
    }

    public boolean isEnablePseudoWire() {
        return enablePseudoWire;
    }

    public void setEnablePseudoWire(boolean enablePseudoWire) {
        this.enablePseudoWire = enablePseudoWire;
    }

    public boolean isEnableRsvpLsp() {
        return this.enableRsvpLsp;
    }

    public void setEnableRsvpLsp(boolean value) {
        this.enableRsvpLsp = value;
    }

    public boolean isEnableVpls() {
        return enableVpls;
    }

    public void setEnableVpls(boolean enableVpls) {
        this.enableVpls = enableVpls;
    }

    public boolean isEnableVrf() {
        return enableVrf;
    }

    public void setEnableVrf(boolean enableVrf) {
        this.enableVrf = enableVrf;
    }

    public boolean isEnableAtmPvc() {
        return enableAtmPvc;
    }

    public void setEnableAtmPvc(boolean enableAtmPvc) {
        this.enableAtmPvc = enableAtmPvc;
    }

    public boolean isEnableFrPvc() {
        return enableFrPvc;
    }

    public void setEnableFrPvc(boolean enableFrPvc) {
        this.enableFrPvc = enableFrPvc;
    }

    public boolean isEnableLink() {
        return enableLink;
    }

    public void setEnableLink(boolean enableLink) {
        this.enableLink = enableLink;
    }

    public boolean isEnableVlanIf() {
        return enableVlanIf;
    }

    public void setEnableVlanIf(boolean enableVlanIf) {
        this.enableVlanIf = enableVlanIf;
    }

    public boolean isEnableRouterVlanIf() {
        return enableRouterVlanIf;
    }

    public void setEnableRouterVlanIf(boolean enableRouterVlanIf) {
        this.enableRouterVlanIf = enableRouterVlanIf;
    }

    public boolean isEnableVplsIf() {
        return enableVplsIf;
    }

    public void setEnableVplsIf(boolean enableVplsIf) {
        this.enableVplsIf = enableVplsIf;
    }

    public boolean isEnableVrfIf() {
        return enableVrfIf;
    }

    public void setEnableVrfIf(boolean enableVrfIf) {
        this.enableVrfIf = enableVrfIf;
    }

    public boolean isEnableIpIf() {
        return enableIpIf;
    }

    public void setEnableIpIf(boolean enableIpIf) {
        this.enableIpIf = enableIpIf;
    }

    private Logger log() {
        return LoggerFactory.getLogger(NodeElementFilter.class);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("dto=").append(this.dto.getAbsoluteName());
        sb.append("\r\n\tsettings={");
        if (this.enableAtmPvc) {
            sb.append(" enableAtmPvc");
        }
        if (this.enableFrPvc) {
            sb.append(" enableFrPvc");
        }
        if (this.enableHardPort) {
            sb.append(" enableHardPort");
        }
        if (this.enableIpIf) {
            sb.append(" enableIpIf");
        }
        if (this.enableLag) {
            sb.append(" enableLag");
        }
        if (this.enableLink) {
            sb.append(" enableLink");
        }
        if (this.enablePool) {
            sb.append(" enablePool");
        }
        if (this.enablePseudoWire) {
            sb.append(" enablePseudoWire");
        }
        if (this.enableRsvpLsp) {
            sb.append(" enableRsvpLsp");
        }
        if (this.enableTarget) {
            sb.append(" enableTarget");
        }
        if (this.enableVlan) {
            sb.append(" enableVlan");
        }
        if (this.enableVlanIf) {
            sb.append(" enableVlanIf");
        }
        if (this.enableVpls) {
            sb.append(" enableVpls");
        }
        if (this.enableVrf) {
            sb.append(" enableVrf");
        }
        if (this.enableVrfIf) {
            sb.append(" enableVrfIf");
        }
        sb.append("}");
        return sb.toString();
    }


}