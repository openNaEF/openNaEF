package voss.nms.inventory.builder;

import naef.dto.LinkDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vlan.VlanSegmentDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.nms.inventory.util.Comparators;
import voss.nms.inventory.util.NameUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("serial")
public class VlanLinkCommandBuilder extends AbstractCommandBuilder {
    private final VlanSegmentDto vlanLink;
    private final VlanDto vlan;
    private final VlanIfDto vif1;
    private final VlanIfDto vif2;
    private final String vifName1;
    private final String vifName2;
    private final LinkDto ethLink;

    public VlanLinkCommandBuilder(VlanSegmentDto target, String editorName) {
        super(VlanSegmentDto.class, target, editorName);
        if (target == null) {
            throw new IllegalStateException("target is null.");
        }
        setConstraint(VlanSegmentDto.class);
        this.vlanLink = target;
        this.vlan = (VlanDto) vlanLink.getOwner();
        this.vif1 = null;
        this.vif2 = null;
        this.vifName1 = null;
        this.vifName2 = null;
        if (this.vlanLink.getLowerLayerLinks().size() > 1) {
            throw new IllegalArgumentException("Unexpected lower-layer status: " + DtoUtil.toDebugString(vlanLink));
        } else if (this.vlanLink.getLowerLayerLinks().size() == 1) {
            this.ethLink = DtoUtil.getInstanceOf(LinkDto.class, vlanLink.getLowerLayerLinks().iterator().next());
        } else {
            this.ethLink = null;
        }
    }

    public VlanLinkCommandBuilder(VlanIfDto vif1, VlanIfDto vif2, LinkDto lower, String editorName) {
        super(VlanSegmentDto.class, null, editorName);
        if (vif1 == null || vif2 == null) {
            throw new IllegalStateException("vif1/2 is null. vif1=" + vif1 + ", vif2=" + vif2);
        } else if (DtoUtil.mvoEquals(vif1.getTrafficDomain(), vif2.getTrafficDomain())) {
            throw new IllegalStateException("vlan mismatch: vif1.vlan="
                    + DtoUtil.getAbsoluteName(vif1.getTrafficDomain())
                    + ", vif2=" + DtoUtil.getAbsoluteName(vif2.getTrafficDomain()));
        }
        setConstraint(VlanSegmentDto.class);
        this.vlanLink = null;
        this.vlan = vif1.getTrafficDomain();
        List<VlanIfDto> vifs = new ArrayList<VlanIfDto>();
        vifs.add(vif1);
        vifs.add(vif2);
        Collections.sort(vifs, Comparators.getIfNameBasedPortComparator());
        this.vif1 = vifs.get(0);
        this.vif2 = vifs.get(1);
        String caption = NameUtil.getNodeIfName(vif1) + " - " + NameUtil.getNodeIfName(vif2);
        this.vifName1 = null;
        this.vifName2 = null;
        this.ethLink = lower;
        recordChange("VlanLink", null, caption);
    }

    public VlanLinkCommandBuilder(VlanDto vlan, String vifName1, String vifName2, LinkDto lower, String editorName) {
        super(VlanSegmentDto.class, null, editorName);
        if (vifName1 == null || vifName2 == null) {
            throw new IllegalStateException("vifName1/2 is null. vif1=" + vifName1 + ", vif2=" + vifName2);
        }
        setConstraint(VlanSegmentDto.class);
        this.vlanLink = null;
        this.vlan = vlan;
        List<String> vifs = new ArrayList<String>();
        vifs.add(vifName1);
        vifs.add(vifName2);
        Collections.sort(vifs);
        this.vif1 = null;
        this.vif2 = null;
        String caption = "[" + vifName1 + "]-[" + vifName2 + "]";
        this.vifName1 = vifs.get(0);
        this.vifName2 = vifs.get(1);
        this.ethLink = lower;
        recordChange("VlanLink", null, caption);
    }

    public void initialize() {
        if (this.vlanLink == null) {
            return;
        }
    }

    public VlanIfDto getVlanIf1() {
        return this.vif1;
    }

    public VlanIfDto getVlanIf2() {
        return this.vif2;
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException {
        if (!hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        if (this.vif1 != null) {
            VlanBuilderUtil.createLink(this.cmd, this.vif1, this.vif2);
            this.cmd.addLastEditCommands();
            InventoryBuilder.translate(this.cmd, CMD.STACK_LOWER_NETWORK,
                    CMD.ARG_LOWER, this.ethLink.getAbsoluteName());
            VlanBuilderUtil.bindVlanLinkToVlan(this.cmd, this.vlan, this.vif1, this.vif2);
        } else if (this.vifName1 != null) {
            VlanBuilderUtil.createLink(this.cmd, this.vifName1, this.vifName2);
            this.cmd.addLastEditCommands();
            InventoryBuilder.translate(this.cmd, CMD.STACK_LOWER_NETWORK,
                    CMD.ARG_LOWER, this.ethLink.getAbsoluteName());
            VlanBuilderUtil.bindVlanLinkToVlan(cmd, this.vlan, vifName1, vifName2);
        } else {
            throw new IllegalStateException("no vif info.");
        }
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, InventoryException {
        if (this.vlanLink == null) {
            throw new IllegalStateException("no vlan-link.");
        }
        VlanBuilderUtil.removeLink(this.cmd, this.vlanLink);
        recordChange("vlan-link", NameUtil.getName(this.vlanLink), null);
        return BuildResult.SUCCESS;
    }

    @Override
    public String getObjectType() {
        return DiffObjectType.VLAN_LINK.getCaption();
    }
}