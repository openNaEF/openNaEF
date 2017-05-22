package voss.nms.inventory.diff.network.builder;

import naef.dto.NodeDto;
import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.vlan.VlanIfDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.util.Util;
import voss.model.RouterVlanIf;
import voss.nms.inventory.builder.VlanIfCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.NetworkDiffUtil;
import voss.nms.inventory.diff.network.analyzer.VlanIfRenderer;

public class VlanIfBuilderFactory extends AbstractPortBuilderFactory {
    private final VlanIfDto target;
    private final VlanIfRenderer renderer;
    private final String editorName;

    public VlanIfBuilderFactory(VlanIfDto port, VlanIfRenderer renderer, String editorName) {
        if (Util.isAllNull(port, renderer)) {
            throw new IllegalArgumentException();
        }
        this.target = port;
        this.renderer = renderer;
        this.editorName = editorName;
    }

    public VlanIfBuilderFactory(VlanIfRenderer renderer, String editorName) {
        this(null, renderer, editorName);
    }

    @Override
    public CommandBuilder getBuilder() {
        VlanIfCommandBuilder builder;
        if (this.target == null) {
            builder = new VlanIfCommandBuilder(this.renderer.getParentAbsoluteName(), editorName);
            builder.setSource(DiffCategory.DISCOVERY.name());
        } else {
            NodeElementDto owner = this.target.getOwner();
            if (owner instanceof EthPortDto || owner instanceof EthLagIfDto) {
                builder = new VlanIfCommandBuilder((PortDto) owner, this.target, editorName);
            } else if (owner instanceof NodeDto) {
                builder = new VlanIfCommandBuilder((NodeDto) owner, this.target, editorName);
            } else {
                throw new IllegalStateException("illegal vlan-if owner: " + owner.getAbsoluteName());
            }
        }
        builder.setPreCheckEnable(false);
        builder.setKeepBinding(true);
        if (renderer != null) {
            String vlanIDName = this.renderer.getValue(VlanIfRenderer.Attr.VLAN_ID);
            Integer vlanID = Integer.valueOf(vlanIDName);
            builder.setVlanId(vlanID);
            String defaultVlanPoolName = NetworkDiffUtil.getPolicy().getDefaultVlanPoolName();
            if (defaultVlanPoolName != null) {
                builder.setVlan(defaultVlanPoolName, vlanIDName);
            }
            try {
                Long bandwidth = Long.valueOf(this.renderer.getValue(VlanIfRenderer.Attr.BANDWIDTH));
                builder.setBandwidth(bandwidth);
            } catch (Exception e) {
            }
            if (this.target instanceof RouterVlanIf) {
            } else {
                builder.setVlanName(renderer.getValue(VlanIfRenderer.Attr.PORT_ID));
            }
            builder.setIfName(renderer.getValue(VlanIfRenderer.Attr.IFNAME));
            String configName = renderer.getValue(VlanIfRenderer.Attr.CONFIGNAME);
            if (configName != null) {
                builder.setConfigName(configName);
            }
            String ip = renderer.getValue(VlanIfRenderer.Attr.IP_ADDRESS);
            String mask = renderer.getValue(VlanIfRenderer.Attr.SUBNETMASK);
            setIpAddress(builder, renderer.getModel(), ip, mask);
            String description = renderer.getValue(VlanIfRenderer.Attr.DESCRIPTION);
            builder.setPortDescription(description);
            builder.setOspfAreaID(renderer.getValue(VlanIfRenderer.Attr.OSPF_AREA_ID));
            Integer cost = Integer.valueOf(renderer.getValue(VlanIfRenderer.Attr.IGP_COST));
            builder.setIgpCost(cost);
            String sviEnabled = renderer.getValue(VlanIfRenderer.Attr.SVI_ENABLED);
            if (sviEnabled != null) {
                builder.setSviEnable(true);
            } else {
                builder.setSviEnable(false);
            }
        }
        return builder;
    }

}