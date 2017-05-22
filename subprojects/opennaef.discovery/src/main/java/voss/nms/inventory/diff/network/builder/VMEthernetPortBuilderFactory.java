package voss.nms.inventory.diff.network.builder;

import naef.dto.HardPortDto;
import naef.dto.eth.EthPortDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.PhysicalPortCommandBuilder;
import voss.nms.inventory.constants.PortType;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.analyzer.AbstractHardPortRenderer;
import voss.nms.inventory.diff.network.analyzer.VMEthernetPortRenderer;

public class VMEthernetPortBuilderFactory extends AbstractPortBuilderFactory {
    private final HardPortDto target;
    private final AbstractHardPortRenderer<?> renderer;
    private final String editorName;

    public VMEthernetPortBuilderFactory(EthPortDto port, VMEthernetPortRenderer renderer, String editorName) {
        if (Util.isAllNull(port, renderer)) {
            throw new IllegalArgumentException();
        }
        this.target = port;
        this.renderer = renderer;
        this.editorName = editorName;
    }

    public VMEthernetPortBuilderFactory(VMEthernetPortRenderer renderer, String editorName) {
        this(null, renderer, editorName);
    }

    @Override
    public CommandBuilder getBuilder() {
        PhysicalPortCommandBuilder builder;
        if (this.target == null) {
            builder = new PhysicalPortCommandBuilder(this.renderer.getParentAbsoluteName(), editorName);
        } else {
            builder = new PhysicalPortCommandBuilder(this.target.getOwner(), this.target, editorName);
        }
        builder.setPreCheckEnable(false);
        setEthernetPortAttributes(builder);
        if (this.target == null) {
            builder.setSource(DiffCategory.DISCOVERY.name());
        }
        return builder;
    }

    private void setEthernetPortAttributes(PhysicalPortCommandBuilder builder) {
        builder.setConstraint(EthPortDto.class);
        if (renderer == null) {
            return;
        }
        VMEthernetPortRenderer renderer = (VMEthernetPortRenderer) this.renderer;
        String ifName = renderer.getValue(VMEthernetPortRenderer.Attr.IFNAME);
        builder.setPortName(ifName);
        builder.setIfName(ifName);
        String configName = renderer.getValue(VMEthernetPortRenderer.Attr.CONFIGNAME);
        if (configName != null) {
            builder.setConfigName(configName);
        }
        builder.setPortType(PortType.VM_ETHERNET);
        String portModeValue = renderer.getValue(VMEthernetPortRenderer.Attr.PORT_MODE);
        if (portModeValue == null) {
            builder.setSwitchPortMode(null);
            builder.setPortMode(null);
        } else {
            builder.setPortMode(portModeValue);
            builder.setSwitchPortMode(renderer.getValue(VMEthernetPortRenderer.Attr.SWITCH_PORT_MODE));
        }

        String ipIfName = renderer.getValue(VMEthernetPortRenderer.Attr.IPIFNAME);
        builder.setIpIfName(ipIfName);
        builder.setIndependentIP(ipIfName != null);
        setBandwidth(builder, VMEthernetPortRenderer.Attr.PORTSPEED_OPER);
        String ip = renderer.getValue(VMEthernetPortRenderer.Attr.IP_ADDRESS);
        String mask = renderer.getValue(VMEthernetPortRenderer.Attr.SUBNETMASK);
        setIpAddress(builder, renderer.getModel(), ip, mask);
        String description = renderer.getValue(VMEthernetPortRenderer.Attr.DESCRIPTION);
        builder.setPortDescription(description);
        builder.setOspfAreaID(renderer.getValue(VMEthernetPortRenderer.Attr.OSPF_AREA_ID));
        Integer cost = Integer.valueOf(renderer.getValue(VMEthernetPortRenderer.Attr.IGP_COST));
        builder.setIgpCost(cost);
    }

    private void setBandwidth(PhysicalPortCommandBuilder builder, Enum<?> attr) {
        Logger log = LoggerFactory.getLogger(VMEthernetPortBuilderFactory.class);
        String speed = renderer.getValue(attr);
        if (speed != null) {
            try {
                long bandwidth = Long.parseLong(speed);
                builder.setBandwidth(bandwidth);
            } catch (NumberFormatException e) {
                log.error("cannot parse speed:[" + speed + "]", e);
            }
        }
    }
}