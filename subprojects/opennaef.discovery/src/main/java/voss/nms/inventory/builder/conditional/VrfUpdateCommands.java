package voss.nms.inventory.builder.conditional;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.vrf.VrfIfDto;
import naef.ui.NaefDtoFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ATTR;
import voss.core.server.naming.naef.IfNameFactory;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.NodeUtil;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.SimpleNodeBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.*;

public class VrfUpdateCommands extends ConditionalCommands<VrfIfDto> {
    private static final long serialVersionUID = 1L;
    private final Map<String, String> vpnAddresses = new HashMap<String, String>();
    private final Map<String, String> addressAndMask = new HashMap<String, String>();

    public VrfUpdateCommands(VrfIfDto vrfIf, String editorName) {
        super(vrfIf, editorName);
    }

    public void addMemberPorts(String ifName, String ipAddress, String subnetMaskLength) {
        if (vpnAddresses.containsKey(ifName)) {
            return;
        }
        this.vpnAddresses.put(ifName, ipAddress);
        this.addressAndMask.put(ipAddress, subnetMaskLength);
    }

    @Override
    protected void evaluateDiffInner(ShellCommands cmd) {
        Logger log = LoggerFactory.getLogger(VrfUpdateCommands.class);
        cmd.addCommand("# built by VrfUpdateCommands.");
        try {
            VrfIfDto vrfIf = getDto(VrfIfDto.class);
            if (vrfIf == null) {
                throw new IllegalStateException("vrfIf is null.");
            }
            vrfIf.renew();
            NodeDto node = vrfIf.getNode();
            NaefDtoFacade facade = DtoUtil.getNaefDtoFacade(node);
            InventoryBuilder.changeContext(cmd, vrfIf);
            cmd.addLastEditCommands();

            Set<String> totalIfNames = new HashSet<String>(this.vpnAddresses.keySet());
            List<String> toAdd = new ArrayList<String>(this.vpnAddresses.keySet());
            List<PortDto> removedPorts = new ArrayList<PortDto>();
            getAttachmentPortDiff(vrfIf.getAttachedPorts(), totalIfNames, toAdd, removedPorts);
            List<PortDto> addedPorts = getAddedPortsByIfName(toAdd, node, facade);
            for (PortDto ac : vrfIf.getAttachedPorts()) {
                if (removedPorts.contains(ac)) {
                    InventoryBuilder.buildDisconnectPortFromNetworkIfCommands(cmd, vrfIf, ac);
                    recordChange("attachment port", DtoUtil.getStringOrNull(ac, MPLSNMS_ATTR.IFNAME), null);
                    InventoryBuilder.changeContext(cmd, ac);
                    InventoryBuilder.buildPortIpUnbindAsPrimaryCommand(cmd);
                    continue;
                }
                String ifName = DtoUtil.getStringOrNull(ac, MPLSNMS_ATTR.IFNAME);
                String vpnIpAddress = this.vpnAddresses.get(ifName);
                IpIfDto vpnIpIf = ac.getPrimaryIpIf();
                if (vpnIpIf == null && vpnIpAddress == null) {
                    continue;
                } else if (Util.equals(vpnIpAddress, DtoUtil.getStringOrNull(vpnIpIf, MPLSNMS_ATTR.IP_ADDRESS))) {
                    continue;
                } else if (vpnIpAddress == null) {
                    log.debug("lost vpn ip-address on " + ac.getAbsoluteName());
                    InventoryBuilder.changeContext(cmd, ac);
                    InventoryBuilder.buildPortIpUnbindAsPrimaryCommand(cmd);
                    recordChange("vpn ip-address", DtoUtil.getStringOrNull(vpnIpIf, MPLSNMS_ATTR.IP_ADDRESS), null);
                    continue;
                }
                maintainVpnIpAddress(cmd, vrfIf, ac, vpnIpAddress);
                recordChange("vpn ip-address", DtoUtil.getStringOrNull(vpnIpIf, MPLSNMS_ATTR.IP_ADDRESS), vpnIpAddress);
            }
            for (PortDto added : addedPorts) {
                InventoryBuilder.buildConnectPortToNetworkIfCommands(cmd, vrfIf, added);
                String ifName = DtoUtil.getStringOrNull(added, MPLSNMS_ATTR.IFNAME);
                recordChange("attachment port", null, ifName);
                String vpnIpAddress = this.vpnAddresses.get(ifName);
                if (vpnIpAddress == null) {
                    continue;
                }
                maintainVpnIpAddress(cmd, vrfIf, added, vpnIpAddress);
            }
            Set<String> activeVpnIpAddresses = getVpnIpAddresses();
            for (PortDto sub : vrfIf.getParts()) {
                if (!(sub instanceof IpIfDto)) {
                    continue;
                }
                IpIfDto ip = (IpIfDto) sub;
                String ipAddr = DtoUtil.getStringOrNull(ip, MPLSNMS_ATTR.IP_ADDRESS);
                if (activeVpnIpAddresses.contains(ipAddr)) {
                    continue;
                }
                InventoryBuilder.changeContext(cmd, vrfIf);
                SimpleNodeBuilder.buildPortDeletionCommands(cmd, ip);
            }
            clearAssertions();
            addAssertion(vrfIf);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void maintainVpnIpAddress(ShellCommands cmd, VrfIfDto vrfIf, PortDto added, String vpnIpAddress) {
        IpIfDto vpnIpIf = NodeUtil.getVpnIpIfByIpAddress(vrfIf, vpnIpAddress);
        if (vpnIpIf == null) {
            InventoryBuilder.changeContext(cmd, vrfIf);
            SimpleNodeBuilder.buildPortCreationCommands(cmd, ATTR.TYPE_IP_PORT, vpnIpAddress);
            String ipifVariableName = "IPIF_" + Integer.toHexString(System.identityHashCode(this));
            InventoryBuilder.assignVar(cmd, ipifVariableName);
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.IFNAME, IfNameFactory.getVpnIpIfIfName(vrfIf.getName(), vpnIpAddress));
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.IP_ADDRESS, vpnIpAddress);
            String subnetMask = this.addressAndMask.get(vpnIpAddress);
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.MASK_LENGTH, subnetMask);
            InventoryBuilder.changeContext(cmd, added);
            InventoryBuilder.buildPortIpBindAsPrimaryCommand(cmd, "$;" + ipifVariableName);
        } else {
            InventoryBuilder.changeContext(cmd, added);
            InventoryBuilder.buildPortIpBindAsPrimaryCommand(cmd, vpnIpIf.getAbsoluteName());
        }
    }

    private Set<String> getVpnIpAddresses() {
        Set<String> set = new HashSet<String>();
        for (String ip : this.vpnAddresses.values()) {
            if (ip == null) {
                continue;
            }
            set.add(ip);
        }
        return set;
    }

}