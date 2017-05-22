package voss.multilayernms.inventory.web.mpls;

import naef.dto.NodeDto;
import naef.dto.PathHopDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import naef.dto.mpls.RsvpLspHopSeriesIdPoolDto;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import tef.MVO.MvoId;
import voss.core.server.builder.BuildResult;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.Util;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.GenericRenderer;
import voss.multilayernms.inventory.renderer.LinkRenderer;
import voss.multilayernms.inventory.renderer.PathRenderer;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.web.parts.OperationStatusChoiceRenderer;
import voss.multilayernms.inventory.web.subnet.PtoPIpSubnetListPage;
import voss.nms.inventory.builder.PathCommandBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.model.RenewableAbstractReadOnlyModel;
import voss.nms.inventory.model.RsvpLspPathHopPoolModel;
import voss.nms.inventory.util.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RsvpLspPathHopEditPage extends WebPage {
    public static final String OPERATION_NAME = "RsvpLspPathHopEdit";

    private Form<Void> editForm;
    private final WebPage backPage;
    private final RsvpLspHopSeriesIdPoolDto pool;
    private final RsvpLspHopSeriesDto path;
    private NodeDto ingress;
    private String pathName;
    private Long bandwidth;
    private String operStatus;
    private String note;
    private IpSubnetDto selectedLink;

    private final RsvpLspPathHopPoolModel model;
    private final HopModel hopModel;
    private final HopSelectionModel hopSelectionModel;

    private final DropDownChoice<NodeDto> ingressSelection;
    private final SubmitLink setIngressLink;
    private final DropDownChoice<IpSubnetDto> linkSelection;
    private final SubmitLink addLastHopLink;
    private final SubmitLink removeLastHopLink;

    private final String editorName;

    public void renew() {
        if (this.path != null) {
            this.path.renew();
        }
    }

    public RsvpLspPathHopEditPage(WebPage backPage, final RsvpLspHopSeriesDto path) {
        this(backPage, path, null);
    }

    public RsvpLspPathHopEditPage(WebPage backPage, final RsvpLspHopSeriesDto path, final NodeDto ingress) {
        if (path == null) {
            throw new IllegalArgumentException();
        }
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;
            this.pool = path.getIdPool();
            if (pool == null) {
                throw new IllegalStateException("pool not found.");
            }
            this.model = new RsvpLspPathHopPoolModel(pool);
            this.path = path;
            if (this.path != null) {
                this.pathName = path.getName();
                this.bandwidth = PathRenderer.getBandwidthAsLong(path);
                this.operStatus = PathRenderer.getOperStatus(path);
                this.note = PathRenderer.getNote(path);
            } else {
                this.pathName = null;
                this.bandwidth = null;
                this.operStatus = MPLSNMS_ATTR.UP;
                this.note = null;
            }
            this.hopModel = new HopModel(this.path);
            if (ingress != null) {
                this.ingress = ingress;
            } else {
                this.ingress = this.hopModel.getIngress();
            }
            this.hopSelectionModel = new HopSelectionModel(this.hopModel);
            this.hopSelectionModel.renew();

            add(new FeedbackPanel("feedback"));
            Label poolName = new Label("poolName", new PropertyModel<String>(model, "poolName"));
            add(poolName);
            Label range = new Label("range", new PropertyModel<String>(model, "range"));
            add(range);
            Label used = new Label("used", new PropertyModel<String>(model, "used"));
            add(used);
            Label purpose = new Label("purpose", new PropertyModel<String>(model, "usage"));
            add(purpose);
            Label lastEditor = new Label("lastEditor", new Model<String>(GenericRenderer.getLastEditor(pool)));
            add(lastEditor);
            Label lastEditTime = new Label("lastEditTime", new Model<String>(GenericRenderer.getLastEditTime(pool)));
            add(lastEditTime);
            Label version = new Label("version", new PropertyModel<String>(model, "version"));
            add(version);

            Form<Void> backForm = new Form<Void>("backForm");
            add(backForm);
            Button backButton = new Button("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    setResponsePage(getBackPage());
                }
            };
            backForm.add(backButton);

            editForm = new Form<Void>("editForm");
            add(editForm);

            Button proceedButton = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        processUpdate();
                        PageUtil.setModelChanged(getBackPage());
                        setResponsePage(getBackPage());
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            editForm.add(proceedButton);

            Button createPtoPLink = new Button("createPtoPLink") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        setResponsePage(new PtoPIpSubnetListPage(getBackPage()));
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            editForm.add(createPtoPLink);

            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();

            TextField<String> pathNameField = new TextField<String>("pathName", new PropertyModel<String>(this, "pathName"));
            pathNameField.setEnabled(this.path == null);
            editForm.add(pathNameField);

            TextField<Long> bandwidthField = new TextField<Long>("bandwidth", new PropertyModel<Long>(this, "bandwidth"));
            editForm.add(bandwidthField);

            RadioChoice<String> statusSelection = new RadioChoice<String>("operStatus",
                    new PropertyModel<String>(this, "operStatus"),
                    conn.getStatusList(),
                    new OperationStatusChoiceRenderer());
            editForm.add(statusSelection);

            ListView<HopUnit> pathHops = new ListView<HopUnit>("pathHops", this.hopModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<HopUnit> item) {
                    final HopUnit unit = item.getModelObject();
                    item.add(new Label("linkSourcePort", NameUtil.getNodeIfName(unit.src)));
                    item.add(new Label("linkSourcePortIp", PortRenderer.getAddressMask(unit.src)));
                    item.add(new Label("linkDestinationPort", NameUtil.getNodeIfName(unit.dst)));
                    item.add(new Label("linkDestinationPortIp", PortRenderer.getAddressMask(unit.dst)));
                    item.add(new Label("linkName", LinkRenderer.getName(unit.link)));
                }
            };
            editForm.add(pathHops);

            this.ingressSelection = new DropDownChoice<NodeDto>(
                    "ingressNodes",
                    new PropertyModel<NodeDto>(this, "ingress"),
                    conn.getActiveNodes(),
                    new ChoiceRenderer<NodeDto>("name")
            );
            this.editForm.add(ingressSelection);

            this.setIngressLink = new SubmitLink("setIngress") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    NodeDto selected = getIngress();
                    if (selected == null) {
                        throw new IllegalArgumentException("please select ingress node.");
                    }
                    hopModel.setIngress(selected);
                    hopSelectionModel.renew();
                    updateFormVisibility();
                }
            };
            this.editForm.add(setIngressLink);

            this.linkSelection = new DropDownChoice<IpSubnetDto>(
                    "links",
                    new PropertyModel<IpSubnetDto>(this, "selectedLink"),
                    hopSelectionModel,
                    new ChoiceRenderer<IpSubnetDto>() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public Object getDisplayValue(IpSubnetDto subnet) {
                            if (subnet == null) {
                                return null;
                            }
                            final NodeDto lastNode = hopModel.getLastHopNode();
                            if (lastNode == null) {
                                return null;
                            }
                            PortDto src = null;
                            PortDto dst = null;
                            for (PortDto member : subnet.getMemberIpifs()) {
                                if (DtoUtil.isSameMvoEntity(lastNode, member.getNode())) {
                                    src = member;
                                } else {
                                    dst = member;
                                }
                            }
                            String srcCaption = getCaption(src);
                            String dstCaption = getCaption(dst);
                            StringBuilder sb = new StringBuilder();
                            sb.append(srcCaption);
                            sb.append(" => ");
                            sb.append(dstCaption);
                            return sb.toString();
                        }

                        private String getCaption(PortDto member) {
                            PortDto owner = null;
                            if (member != null) {
                                if (member instanceof IpIfDto) {
                                    IpIfDto ipif = IpIfDto.class.cast(member);
                                    NodeDto node = ipif.getNode();
                                    for (PortDto p : node.getPorts()) {
                                        if (DtoUtil.isSameMvoEntity(p.getPrimaryIpIf(), ipif)) {
                                            owner = p;
                                            String ipAddress = PortRenderer.getIpAddress(ipif);
                                            return NameUtil.getNodeIfName(owner) + "(" + ipAddress + ")";
                                        }
                                    }
                                }
                            } else if(member == null){
                                throw new IllegalStateException("port not found");
                            }
                            String ipAddress = PortRenderer.getIpAddress(member);
                            return NameUtil.getNodeIfName(owner) + "(" + ipAddress + ")";
                        }

                    });
            this.editForm.add(linkSelection);

            this.addLastHopLink = new SubmitLink("addLast") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    IpSubnetDto selected = getSelectedLink();
                    if (selected == null) {
                        throw new IllegalArgumentException("please select next hop link.");
                    }
                    PortDto sourceSide = null;
                    PortDto destinationSide = null;
                    for (PortDto ip : selected.getMemberIpifs()) {
                        if (DtoUtil.isSameMvoEntity(ip.getNode(), hopModel.getLastHopNode())) {
                            sourceSide = ip;
                        } else {
                            destinationSide = ip;
                        }
                    }
                    if (Util.isNull(sourceSide, destinationSide)) {
                        throw new IllegalArgumentException(
                                "illegal subnet: " + selected.getAbsoluteName());
                    }
                    HopUnit unit = new HopUnit();
                    unit.link = selected;
                    unit.src = sourceSide;
                    unit.dst = destinationSide;
                    hopModel.addLastHop(unit);
                    hopSelectionModel.renew();
                    updateFormVisibility();
                }
            };
            this.editForm.add(addLastHopLink);

            this.removeLastHopLink = new SubmitLink("removeLast") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    hopModel.removeLastHop();
                    hopSelectionModel.renew();
                    updateFormVisibility();
                }
            };
            this.editForm.add(removeLastHopLink);

            TextArea<String> noteArea = new TextArea<String>("noteArea", new PropertyModel<String>(this, "note"));
            editForm.add(noteArea);

            updateFormVisibility();
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void processUpdate() throws InventoryException, IOException, ExternalServiceException {
        if (path == null) {
            throw new IllegalStateException("path is null.");
        }
        NodeDto ingress = RsvpLspUtil.getIngressNode(path);
        PathCommandBuilder builder;
        if (ingress != null) {
            builder = new PathCommandBuilder(path, editorName);
        } else {
            if (this.ingress == null) {
                throw new IllegalArgumentException("Missing ingress node. Please select ingress node.");
            }
            builder = new PathCommandBuilder(path, this.ingress, editorName);
        }
        if (hopModel.isChanged()) {
            builder.setPathHops(hopModel.getHops());
        }
        builder.setBandwidth(bandwidth);
        builder.setOperationStatus(operStatus);
        builder.setNote(note);
        BuildResult result = builder.buildCommand();
        if (result == BuildResult.SUCCESS) {
            ShellConnector.getInstance().execute(builder);
        }
    }

    public RsvpLspHopSeriesDto getRsvpLspPath() {
        return this.path;
    }

    @Override
    protected void onModelChanged() {
        this.path.renew();
        this.hopSelectionModel.renew();
        updateFormVisibility();
        super.onModelChanged();
    }

    private void updateFormVisibility() {
        boolean noLastHop = hopModel.getLastHopNode() == null;
        boolean hasLastHop = !noLastHop;
        setVisibility(this.ingressSelection, noLastHop);
        setVisibility(this.setIngressLink, noLastHop);
        setVisibility(this.linkSelection, hasLastHop);
        setVisibility(this.addLastHopLink, hasLastHop);
        setVisibility(this.removeLastHopLink, hasLastHop);
    }

    private void setVisibility(MarkupContainer markup, boolean visible) {
        markup.setEnabled(visible);
        markup.setVisible(visible);
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public String getNote() {
        return this.note;
    }

    public void setNote(String s) {
        this.note = s;
    }

    public String getPathName() {
        return pathName;
    }

    public void setPathName(String pathName) {
        this.pathName = pathName;
    }

    public IpSubnetDto getSelectedLink() {
        return selectedLink;
    }

    public void setSelectedLink(IpSubnetDto selectedLink) {
        this.selectedLink = selectedLink;
    }

    public HopModel getHopModel() {
        return hopModel;
    }

    public HopSelectionModel getHopSelectionModel() {
        return hopSelectionModel;
    }

    public RsvpLspHopSeriesDto getTargetPath() {
        return path;
    }

    public void setIngress(NodeDto node) {
        this.ingress = node;
    }

    public NodeDto getIngress() {
        return ingress;
    }

    public Long getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(Long bandwidth) {
        this.bandwidth = bandwidth;
    }

    private class HopSelectionModel extends RenewableAbstractReadOnlyModel<List<IpSubnetDto>> {
        private static final long serialVersionUID = 1L;
        private final List<IpSubnetDto> subnets = new ArrayList<IpSubnetDto>();
        private final HopModel hopModel;

        public HopSelectionModel(HopModel hopModel) {
            this.hopModel = hopModel;
        }

        @Override
        public List<IpSubnetDto> getObject() {
            return this.subnets;
        }

        public void renew() {
            NodeDto lastHopNode = hopModel.getLastHopNode();
            if (lastHopNode == null) {
                lastHopNode = RsvpLspPathHopEditPage.this.getIngress();
            }
            if (lastHopNode == null) {
                this.subnets.clear();
                return;
            }
            List<IpSubnetDto> newSubnets = new ArrayList<IpSubnetDto>();
            for (PortDto port : lastHopNode.getPorts()) {
                if (!(port instanceof IpIfDto)) {
                    continue;
                }
                IpSubnetDto subnet = NodeUtil.getLayer3Link(port);
                if (subnet == null) {
                    continue;
                }
                if (!isSelectable(subnet)) {
                    continue;
                }
                newSubnets.add(subnet);
            }
            this.subnets.clear();
            this.subnets.addAll(newSubnets);
        }

        private boolean isSelectable(IpSubnetDto subnet) {
            if (subnet.getMemberIpifs().size() != 2) {
                return false;
            }
            Set<MvoId> nodeIds = new HashSet<MvoId>();
            for (PortDto ip : subnet.getMemberIpifs()) {
                String ipAddress = PortRenderer.getIpAddress(ip);
                if (ipAddress == null) {
                    return false;
                }
                nodeIds.add(DtoUtil.getMvoId(ip.getNode()));
            }
            if (nodeIds.size() == 1) {
                return false;
            }
            return true;
        }
    }

    private static class HopModel extends AbstractReadOnlyModel<List<HopUnit>> {
        private static final long serialVersionUID = 1L;
        private final ArrayList<HopUnit> hops = new ArrayList<HopUnit>();
        private NodeDto ingress = null;
        private boolean changed = false;

        public HopModel(RsvpLspHopSeriesDto path) {
            for (PathHopDto hop : path.getHops()) {
                IpSubnetDto subnet = NodeUtil.getLayer3Link(hop.getSrcPort());
                HopUnit unit = new HopUnit();
                unit.src = hop.getSrcPort();
                unit.dst = hop.getDstPort();
                unit.link = subnet;
                hops.add(unit);
                if (this.ingress == null) {
                    this.ingress = hop.getSrcPort().getNode();
                }
            }
        }

        public boolean isChanged() {
            return changed;
        }

        public List<HopUnit> getObject() {
            return hops;
        }

        public NodeDto getIngress() {
            return this.ingress;
        }

        public void setIngress(NodeDto node) {
            this.ingress = node;
        }

        public NodeDto getLastHopNode() {
            if (this.hops.isEmpty()) {
                return this.ingress;
            }
            HopUnit lastHop = this.hops.get(this.hops.size() - 1);
            return lastHop.dst.getNode();
        }

        public void addLastHop(HopUnit newHop) {
            for (HopUnit hop : this.hops) {
                if (DtoUtil.isSameMvoEntity(newHop.link, hop.link)) {
                    throw new IllegalArgumentException("already selected.");
                }
            }
            this.hops.add(newHop);
            if (this.ingress == null) {
                this.ingress = newHop.src.getNode();
            }
            changed = true;
        }

        public void removeLastHop() {
            this.hops.remove(hops.size() - 1);
            changed = true;
        }

        public List<PortDto> getHops() {
            List<PortDto> result = new ArrayList<PortDto>();
            for (HopUnit unit : this.hops) {
                result.add(unit.src);
            }
            return result;
        }
    }

    private static class HopUnit implements Serializable {
        private static final long serialVersionUID = 1L;
        public PortDto src;
        public PortDto dst;
        public IpSubnetDto link;
    }
}