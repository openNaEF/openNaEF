package voss.nms.inventory.diff.network.builder;

import naef.dto.HardPortDto;
import naef.dto.atm.AtmPortDto;
import naef.dto.eth.EthPortDto;
import naef.dto.pos.PosPortDto;
import naef.dto.serial.SerialPortDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.Util;
import voss.model.SerialPort;
import voss.model.SerialPortImpl;
import voss.nms.inventory.builder.PhysicalPortCommandBuilder;
import voss.nms.inventory.constants.PortType;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.analyzer.*;

import java.util.List;

public class PhysicalPortBuilderFactory extends AbstractPortBuilderFactory {
    private final HardPortDto target;
    private final AbstractHardPortRenderer<?> renderer;
    private final String editorName;

    public PhysicalPortBuilderFactory(HardPortDto port, AbstractHardPortRenderer<?> renderer, String editorName) {
        if (Util.isAllNull(port, renderer)) {
            throw new IllegalArgumentException();
        }
        this.target = port;
        this.renderer = renderer;
        this.editorName = editorName;
    }

    public PhysicalPortBuilderFactory(EthernetPortRenderer renderer, String editorName) {
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
        PortType portType = getPortType();
        switch (portType) {
            case ETHERNET:
                setEthernetPortAttributes(builder);
                break;
            case ATM:
                setAtmPortAttributes(builder);
                break;
            case SERIAL:
                setSerialPortAttributes(builder);
                break;
            case POS:
                setPosPortAttributes(builder);
                break;
            default:
                throw new IllegalStateException();
        }
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
        EthernetPortRenderer renderer = (EthernetPortRenderer) this.renderer;
        String ifName = renderer.getValue(EthernetPortRenderer.Attr.IFNAME);
        builder.setPortName(String.valueOf(renderer.getModel().getPortIndex()));
        builder.setIfName(ifName);
        String configName = renderer.getValue(EthernetPortRenderer.Attr.CONFIGNAME);
        if (configName != null) {
            builder.setConfigName(configName);
        }
        builder.setPortType(PortType.ETHERNET);
        String portModeValue = renderer.getValue(EthernetPortRenderer.Attr.PORT_MODE);
        if (portModeValue == null) {
            builder.setSwitchPortMode(null);
            builder.setPortMode(null);
        } else {
            builder.setPortMode(portModeValue);
            builder.setSwitchPortMode(renderer.getValue(EthernetPortRenderer.Attr.SWITCH_PORT_MODE));
        }
        String ipIfName = renderer.getValue(EthernetPortRenderer.Attr.IPIFNAME);
        builder.setIpIfName(ipIfName);
        builder.setIndependentIP(ipIfName != null);
        String adminStatus = renderer.getValue(EthernetPortRenderer.Attr.STATUS_ADMIN);
        builder.setAdminStatus(adminStatus);
        String adminSpeed = renderer.getValue(EthernetPortRenderer.Attr.PORTSPEED_ADMIN);
        builder.setAdminBandwidth(adminSpeed);
        String adminDuplex = renderer.getValue(EthernetPortRenderer.Attr.DUPLEX_ADMIN);
        builder.setAdminDuplex(adminDuplex);
        setBandwidth(builder, EthernetPortRenderer.Attr.PORTSPEED_OPER);
        String ip = renderer.getValue(EthernetPortRenderer.Attr.IP_ADDRESS);
        String mask = renderer.getValue(EthernetPortRenderer.Attr.SUBNETMASK);
        setIpAddress(builder, renderer.getModel(), ip, mask);
        String description = renderer.getValue(EthernetPortRenderer.Attr.DESCRIPTION);
        builder.setPortDescription(description);
        builder.setOspfAreaID(renderer.getValue(EthernetPortRenderer.Attr.OSPF_AREA_ID));
        Integer cost = Integer.valueOf(renderer.getValue(EthernetPortRenderer.Attr.IGP_COST));
        builder.setIgpCost(cost);
        String level = renderer.getValue(EthernetPortRenderer.Attr.STORMCONTROL_BROADCAST_LEVEL);
        builder.setStormControlBroadcastLevel(level);
        List<String> actions = renderer.getValues(EthernetPortRenderer.Attr.STORMCONTROL_ACTION);
        builder.setStormControlActions(actions);
    }

    private void setSerialPortAttributes(PhysicalPortCommandBuilder builder) {
        builder.setConstraint(SerialPortDto.class);
        if (renderer == null) {
            return;
        }
        SerialPortRenderer renderer = (SerialPortRenderer) this.renderer;

        String ifName = renderer.getValue(SerialPortRenderer.Attr.IFNAME);
        SerialPort serial = renderer.getModel();
        if (serial instanceof SerialPortImpl) {
            SerialPortImpl impl = (SerialPortImpl) serial;
            builder.setPortName(String.valueOf(impl.getPortIndex()));
        } else {
            builder.setPortName(serial.getIfName());
        }
        builder.setIfName(ifName);
        String configName = renderer.getValue(SerialPortRenderer.Attr.CONFIGNAME);
        if (configName != null) {
            builder.setConfigName(configName);
        }
        String ipIfName = renderer.getValue(SerialPortRenderer.Attr.IPIFNAME);
        builder.setIpIfName(ipIfName);
        builder.setIndependentIP(ipIfName != null);
        builder.setPortType(PortType.SERIAL);
        setBandwidth(builder, SerialPortRenderer.Attr.PORT_SPEED);
        String ip = renderer.getValue(SerialPortRenderer.Attr.IP_ADDRESS);
        String mask = renderer.getValue(SerialPortRenderer.Attr.SUBNETMASK);
        setIpAddress(builder, renderer.getModel(), ip, mask);
        String description = renderer.getValue(SerialPortRenderer.Attr.DESCRIPTION);
        builder.setPortDescription(description);
        builder.setOspfAreaID(renderer.getValue(SerialPortRenderer.Attr.OSPF_AREA_ID));
        Integer cost = Integer.valueOf(renderer.getValue(SerialPortRenderer.Attr.IGP_COST));
        builder.setIgpCost(cost);
    }

    private void setPosPortAttributes(PhysicalPortCommandBuilder builder) {
        builder.setConstraint(PosPortDto.class);
        if (renderer == null) {
            return;
        }
        PosPortRenderer renderer = (PosPortRenderer) this.renderer;

        builder.setPortName(String.valueOf(renderer.getModel().getPortIndex()));
        String ifName = renderer.getValue(PosPortRenderer.Attr.IFNAME);
        builder.setIfName(ifName);
        String configName = renderer.getValue(PosPortRenderer.Attr.CONFIGNAME);
        if (configName != null) {
            builder.setConfigName(configName);
        }
        String ipIfName = renderer.getValue(PosPortRenderer.Attr.IPIFNAME);
        builder.setIpIfName(ipIfName);
        builder.setIndependentIP(ipIfName != null);
        builder.setPortType(PortType.POS);
        setBandwidth(builder, PosPortRenderer.Attr.BANDWIDTH);
        String ip = renderer.getValue(PosPortRenderer.Attr.IP_ADDRESS);
        String mask = renderer.getValue(PosPortRenderer.Attr.SUBNETMASK);
        setIpAddress(builder, renderer.getModel(), ip, mask);
        String description = renderer.getValue(PosPortRenderer.Attr.DESCRIPTION);
        builder.setPortDescription(description);
        builder.setOspfAreaID(renderer.getValue(PosPortRenderer.Attr.OSPF_AREA_ID));
        Integer cost = Integer.valueOf(renderer.getValue(PosPortRenderer.Attr.IGP_COST));
        builder.setIgpCost(cost);
    }

    private void setAtmPortAttributes(PhysicalPortCommandBuilder builder) {
        builder.setConstraint(AtmPortDto.class);
        if (renderer == null) {
            return;
        }
        AtmPortRenderer renderer = (AtmPortRenderer) this.renderer;
        builder.setPortName(String.valueOf(renderer.getModel().getPortIndex()));
        String ifName = renderer.getValue(AtmPortRenderer.Attr.IFNAME);
        builder.setIfName(ifName);
        String configName = renderer.getValue(AtmPortRenderer.Attr.CONFIGNAME);
        if (configName != null) {
            builder.setConfigName(configName);
        }
        String ipIfName = renderer.getValue(AtmPortRenderer.Attr.IPIFNAME);
        builder.setIpIfName(ipIfName);
        builder.setIndependentIP(ipIfName != null);
        builder.setPortType(PortType.ATM);
        setBandwidth(builder, AtmPortRenderer.Attr.PORT_SPEED);
        String ip = renderer.getValue(AtmPortRenderer.Attr.IP_ADDRESS);
        String mask = renderer.getValue(AtmPortRenderer.Attr.SUBNETMASK);
        setIpAddress(builder, renderer.getModel(), ip, mask);
        String description = renderer.getValue(AtmPortRenderer.Attr.DESCRIPTION);
        builder.setPortDescription(description);
        builder.setOspfAreaID(renderer.getValue(AtmPortRenderer.Attr.OSPF_AREA_ID));
        Integer cost = Integer.valueOf(renderer.getValue(AtmPortRenderer.Attr.IGP_COST));
        builder.setIgpCost(cost);
    }

    private void setBandwidth(PhysicalPortCommandBuilder builder, Enum<?> attr) {
        Logger log = LoggerFactory.getLogger(PhysicalPortBuilderFactory.class);
        String speed = renderer.getValue(attr);
        if (speed == null) {
            return;
        }
        try {
            long bandwidth = Long.parseLong(speed);
            builder.setBandwidth(bandwidth);
        } catch (NumberFormatException e) {
            log.error("cannot parse speed:[" + speed + "]", e);
        }
    }

    private PortType getPortType() {
        if (target != null) {
            if (target instanceof EthPortDto) {
                return PortType.ETHERNET;
            } else if (target instanceof AtmPortDto) {
                return PortType.ATM;
            } else if (target instanceof SerialPortDto) {
                return PortType.SERIAL;
            } else if (target instanceof PosPortDto) {
                return PortType.POS;
            }
            throw new IllegalArgumentException("unsupported type: " + target.getAbsoluteName());
        } else if (renderer != null) {
            if (renderer instanceof EthernetPortRenderer) {
                return PortType.ETHERNET;
            } else if (renderer instanceof SerialPortRenderer) {
                return PortType.SERIAL;
            } else if (renderer instanceof PosPortRenderer) {
                return PortType.POS;
            } else if (renderer instanceof AtmPortRenderer) {
                return PortType.ATM;
            } else {
                throw new IllegalStateException("unknown renderer:" + renderer.getClass().getName());
            }
        }
        throw new IllegalArgumentException("no target and renderer.");
    }
}