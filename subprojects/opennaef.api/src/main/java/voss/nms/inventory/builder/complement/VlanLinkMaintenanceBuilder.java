package voss.nms.inventory.builder.complement;

import naef.dto.LinkDto;
import naef.dto.NetworkDto;
import naef.dto.PortDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vlan.VlanSegmentDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.*;
import voss.nms.inventory.builder.VlanBuilderUtil;
import voss.nms.inventory.util.Comparators;

import java.io.IOException;
import java.util.*;

@SuppressWarnings("serial")
public class VlanLinkMaintenanceBuilder extends AbstractCommandBuilder {
    private final VlanDto vlan;
    private final MvoDtoSet<VlanIfDto> vlanIfs = new MvoDtoSet<VlanIfDto>();
    private final MvoDtoSet<VlanIfDto> original = new MvoDtoSet<VlanIfDto>();

    public VlanLinkMaintenanceBuilder(VlanDto target, String editorName) {
        super(VlanDto.class, target, editorName);
        if (target == null) {
            throw new IllegalStateException("target is null.");
        }
        setConstraint(VlanDto.class);
        this.vlan = target;
    }

    public void initialize() {
        if (this.vlan == null) {
            return;
        }
        for (VlanIfDto member : this.vlan.getMemberVlanifs()) {
            this.vlanIfs.add(member);
            this.original.add(member);
        }
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException {
        checkBuilt();
        Map<VlanLinkKey, VlanLinkEntry> links = new HashMap<VlanLinkKey, VlanLinkEntry>();
        for (VlanIfDto vif : this.vlan.getMemberVlanifs()) {
            for (VlanSegmentDto link : vif.getVlanLinks()) {
                VlanLinkKey key = new VlanLinkKey(link);
                log().debug("vlanLinkEntry current found: " + key);
                VlanLinkEntry entry = links.get(key);
                if (entry == null) {
                    entry = new VlanLinkEntry(link);
                    links.put(key, entry);
                }
            }
        }
        MvoDtoMap map = new MvoDtoMap();
        for (VlanIfDto vif : this.vlan.getMemberVlanifs()) {
            for (PortDto tagged : vif.getTaggedVlans()) {
                map.put(tagged, vif);
            }
        }
        log().info("vlanLinkEntry discovery begin.");
        for (VlanIfDto vif : this.vlan.getMemberVlanifs()) {
            log().debug("- target vif: " + vif.getAbsoluteName());
            if (VlanUtil.isSecondaryVlanIf(vif)) {
                log().debug("- tag stack secondary found: ignore.");
                continue;
            }
            for (PortDto tagged : vif.getTaggedVlans()) {
                log().debug("- tagged=" + tagged.getAbsoluteName());
                PortDto neighbor = NodeUtil.getLayer2Neighbor(tagged);
                if (neighbor == null) {
                    log().debug("- no neighbor.");
                    continue;
                }
                log().debug("- neighbor=" + neighbor.getAbsoluteName());
                LinkDto l2Link = NodeUtil.getLayer2Link(tagged);
                if (l2Link == null) {
                    throw new IllegalStateException("port has neighbor, but has no link: bug? " +
                            "tagged-port is " + tagged.getAbsoluteName());
                }
                VlanIfDto vif2 = (VlanIfDto) map.getObject(neighbor);
                if (vif2 == null) {
                    log().debug("- no neighbor vlan-if.");
                    continue;
                }
                VlanLinkKey key = new VlanLinkKey(vif, vif2);
                VlanLinkEntry entry = links.get(key);
                if (entry == null) {
                    entry = new VlanLinkEntry(null);
                    links.put(key, entry);
                    log().debug("- new vlanLinkEntry created: " + key);
                }
                entry.setLowerLayerLink(l2Link);
                entry.setFound();
                log().debug("- vlanLinkEntry found: " + key);
            }
        }

        for (Map.Entry<VlanLinkKey, VlanLinkEntry> entry : links.entrySet()) {
            VlanLinkKey key = entry.getKey();
            VlanLinkEntry value = entry.getValue();
            log().debug("vlanLinkEntry: " + value);
            if (value.isNew()) {
                VlanIfDto vif1 = key.getVif1();
                VlanIfDto vif2 = key.getVif2();
                VlanBuilderUtil.createLink(cmd, vif1, vif2);
                recordChange("vlan-link", null, "*");
                for (LinkDto link : value.getLowerLayerLinks()) {
                    VlanBuilderUtil.stackLowerLayerLink(cmd, link);
                }
                VlanBuilderUtil.bindVlanLinkToVlan(cmd, vlan, vif1, vif2);
            } else if (value.isDelete()) {
                VlanSegmentDto deleteTarget = value.getLink();
                VlanBuilderUtil.removeLink(cmd, deleteTarget);
                recordChange("vlan-link", "*", null);
            } else {
            }
        }
        built();
        return setResult(BuildResult.SUCCESS);
    }

    private static class VlanLinkKey {
        private final VlanIfDto vif1;
        private final VlanIfDto vif2;

        public VlanLinkKey(VlanSegmentDto link) {
            if (link == null) {
                throw new IllegalArgumentException("link is null.");
            } else if (link.getMemberPorts().size() != 2) {
                throw new IllegalArgumentException("illegal link: "
                        + link.getMemberPorts().size() + " ports on " + DtoUtil.toDebugString(link));
            }
            List<VlanIfDto> keys = new ArrayList<VlanIfDto>();
            for (PortDto member : link.getMemberPorts()) {
                keys.add((VlanIfDto) member);
            }
            Collections.sort(keys, Comparators.getIfNameBasedPortComparator());
            this.vif1 = keys.get(0);
            this.vif2 = keys.get(1);
        }

        public VlanLinkKey(VlanIfDto vif1, VlanIfDto vif2) {
            if (Util.isNull(vif1, vif2)) {
                throw new IllegalArgumentException("null argument.");
            }
            List<VlanIfDto> keys = new ArrayList<VlanIfDto>();
            keys.add(vif1);
            keys.add(vif2);
            Collections.sort(keys, Comparators.getIfNameBasedPortComparator());
            this.vif1 = keys.get(0);
            this.vif2 = keys.get(1);
        }

        public VlanIfDto getVif1() {
            return this.vif1;
        }

        public VlanIfDto getVif2() {
            return this.vif2;
        }

        @Override
        public String toString() {
            return this.vif1.getAbsoluteName() + "|" + this.vif2.getAbsoluteName();
        }

        @Override
        public int hashCode() {
            return DtoUtil.getMvoId(this.vif1).hashCode() + DtoUtil.getMvoId(this.vif2).hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            } else if (this == o) {
                return true;
            } else if (!VlanLinkKey.class.isInstance(o)) {
                return false;
            }
            VlanLinkKey other = (VlanLinkKey) o;
            return DtoUtil.mvoEquals(this.vif1, other.vif1) && DtoUtil.mvoEquals(this.vif2, other.vif2);
        }
    }

    private static class VlanLinkEntry {
        private final VlanSegmentDto link;
        private boolean found = false;
        private final MvoDtoSet<LinkDto> lowerLayerLinks = new MvoDtoSet<LinkDto>();

        public VlanLinkEntry(VlanSegmentDto link) {
            this.link = link;
            if (link != null) {
                for (NetworkDto lower : link.getLowerLayerLinks()) {
                    if (!LinkDto.class.isInstance(lower)) {
                        continue;
                    }
                    this.lowerLayerLinks.add((LinkDto) lower);
                }
            }
        }

        public void setFound() {
            this.found = true;
        }

        public VlanSegmentDto getLink() {
            return this.link;
        }

        public boolean isNew() {
            return this.link == null && this.found;
        }

        public boolean isDelete() {
            return this.link != null && !this.found;
        }

        public void setLowerLayerLink(LinkDto link) {
            this.lowerLayerLinks.add(link);
        }

        public MvoDtoSet<LinkDto> getLowerLayerLinks() {
            return this.lowerLayerLinks;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (this.link != null) {
                sb.append("vlan-link:").append(this.link.getAbsoluteName()).append("\r\n");
            } else {
                sb.append("vlan-link: <not created>\r\n");
            }
            if (isNew()) {
                sb.append("- status: new");
            } else if (isDelete()) {
                sb.append("- status: delete");
            } else {
                sb.append("- status: update");
            }
            sb.append("\r\n");
            for (LinkDto lower : this.lowerLayerLinks) {
                sb.append("- lower layer link: ").append(lower.getAbsoluteName()).append("\r\n");
            }
            return sb.toString();
        }
    }

    @Override
    public BuildResult buildDeleteCommandInner() throws IOException, InventoryException {
        if (this.vlan == null) {
            return BuildResult.NO_CHANGES;
        }
        for (NetworkDto network : this.vlan.getLowerLayerLinks()) {
            if (!VlanSegmentDto.class.isInstance(network)) {
                throw new IllegalStateException("A lower layer link other than vlan-segment was found: "
                        + network.getAbsoluteName());
            }
            VlanSegmentDto link = (VlanSegmentDto) network;
            VlanBuilderUtil.removeLink(cmd, link);
        }
        return BuildResult.SUCCESS;
    }

    @Override
    public String getObjectType() {
        return DiffObjectType.VLAN_LINK.getCaption();
    }
}