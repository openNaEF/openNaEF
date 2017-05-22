package voss.multilayernms.inventory.builder;

import naef.dto.NodeDto;
import naef.dto.PathHopDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import tef.MVO.MvoId;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.Util;
import voss.multilayernms.inventory.util.RsvpLspExtUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.RsvpLspUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LspCouplingCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final RsvpLspDto lsp1;
    private final RsvpLspDto lsp2;

    public LspCouplingCommandBuilder(RsvpLspDto lsp1, RsvpLspDto lsp2, String editorName) {
        super(RsvpLspDto.class, lsp1, editorName);
        setConstraint(RsvpLspDto.class);
        if (Util.isNull(lsp1, lsp2)) {
            throw new IllegalArgumentException();
        }
        checkCouping(lsp1, lsp2);
        checkCouping(lsp2, lsp1);
        this.lsp1 = lsp1;
        this.lsp2 = lsp2;
    }

    private void checkCouping(RsvpLspDto lsp, RsvpLspDto toBePair) {
        RsvpLspDto opposit = RsvpLspExtUtil.getOppositLsp(lsp);
        if (opposit != null && !DtoUtil.isSameMvoEntity(toBePair, opposit)) {
            throw new IllegalArgumentException("lsp[" + opposit.getName() + "] has pairing LSP.");
        }
        NodeDto ingress1 = RsvpLspUtil.getIngressNode(lsp);
        NodeDto ingress2 = RsvpLspUtil.getIngressNode(toBePair);
        NodeDto egress1 = RsvpLspUtil.getEgressNode(lsp);
        NodeDto egress2 = RsvpLspUtil.getEgressNode(toBePair);
        if (!DtoUtil.isSameMvoEntity(ingress1, egress2)) {
            throw new IllegalArgumentException("ingress1-egress2 mismatch.");
        }
        if (!DtoUtil.isSameMvoEntity(ingress2, egress1)) {
            throw new IllegalArgumentException("ingress2-egress1 mismatch.");
        }
        if (!Util.isAllOrNothing(lsp.getHopSeries1(), toBePair.getHopSeries1())) {
            throw new IllegalArgumentException("primary path mismatch.");
        }
        if (!Util.isAllOrNothing(lsp.getHopSeries2(), toBePair.getHopSeries2())) {
            throw new IllegalArgumentException("secondary path mismatch.");
        }
        if (!hasSameHops(lsp.getHopSeries1(), toBePair.getHopSeries1())) {
            throw new IllegalArgumentException("primary path hop mismatch.");
        }
        if (!hasSameHops(lsp.getHopSeries2(), toBePair.getHopSeries2())) {
            throw new IllegalArgumentException("secondary path hop mismatch.");
        }
    }

    private boolean hasSameHops(RsvpLspHopSeriesDto hops1, RsvpLspHopSeriesDto hops2) {
        if (hops1 == null && hops2 == null) {
            return true;
        } else if (hops1 != null && hops2 != null) {
            List<MvoId> hopIDs1 = getHopIDs(hops1);
            List<MvoId> hopIDs2 = getHopIDs(hops2);
            Collections.reverse(hopIDs2);
            if (hopIDs1.size() != hopIDs2.size()) {
                return false;
            }
            for (int i = 0; i < hopIDs1.size(); i++) {
                if (!hopIDs1.get(i).equals(hopIDs2.get(i))) {
                    return false;
                }
            }
            return true;
        } else {
            throw new IllegalStateException("path configuration mismatch.");
        }
    }

    private List<MvoId> getHopIDs(RsvpLspHopSeriesDto path) {
        List<MvoId> result = new ArrayList<MvoId>();
        for (PathHopDto hop : path.getHops()) {
            result.add(DtoUtil.getMvoId(hop.getSrcPort()));
            result.add(DtoUtil.getMvoId(hop.getDstPort()));
        }
        return result;
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException {
        cmd.addVersionCheckTarget(this.lsp1, this.lsp2);
        InventoryBuilder.changeContext(cmd, lsp1);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.LSP_PAIR, this.lsp2.getAbsoluteName());
        InventoryBuilder.changeContext(cmd, lsp2);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.LSP_PAIR, this.lsp1.getAbsoluteName());
        recordChange("create", "", "");
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        cmd.addVersionCheckTarget(this.lsp1, this.lsp2);
        InventoryBuilder.changeContext(cmd, this.lsp1);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.LSP_PAIR, null);
        InventoryBuilder.changeContext(cmd, this.lsp2);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.LSP_PAIR, null);
        recordChange("delete", "", "");
        return BuildResult.SUCCESS;
    }

    public String getObjectType() {
        return DiffObjectType.RSVPLSP.getCaption();
    }
}