package voss.multilayernms.inventory.web.node;

import naef.dto.*;
import org.apache.wicket.PageParameters;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.config.CoreConfiguration;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.NodeElementComparator;
import voss.core.server.util.NodeElementFilter;
import voss.core.server.util.PerfLog;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.HardwareRenderer;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.web.location.LocationUtil;
import voss.multilayernms.inventory.web.parts.PortPanel;
import voss.multilayernms.inventory.web.vm.VirtualNodeEditPage;
import voss.nms.inventory.model.PortsModel;
import voss.nms.inventory.util.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleNodeDetailPage extends WebPage {
    public static final String OPERATION_NAME = "SimpleNodeDetail";
    public static final String KEY_NODE_NAME = "node";
    private NodeDto node;
    private final String editorName;
    private final SlotsModel slotsModel;
    private final PortsModel portsModel;
    private long startTime = System.currentTimeMillis();

    public SimpleNodeDetailPage() {
        this(new PageParameters());
    }

    public SimpleNodeDetailPage(PageParameters param) {
        try {
            long prev = System.currentTimeMillis();
            String nodeName_ = NodeUtil.getNodeName(param);
            if (nodeName_ == null) {
                throw new IllegalStateException("A node name is not specified.");
            }
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            this.node = conn.getNodeDto(nodeName_);
            if (this.node == null) {
                throw new IllegalStateException("No node with this name found: " + nodeName_);
            }

            Label nodeName = new Label("nodeName", new Model<String>(node.getName()));
            add(nodeName);
            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            ExternalLink searchLink = UrlUtil.getLink("search", "search");
            add(searchLink);
            BookmarkablePageLink<Void> reloadLink = NodePageUtil.createSimpleNodeLink("reload", node, null);
            add(reloadLink);

            long time = System.currentTimeMillis();
            PerfLog.info(prev, time, "build header component");
            prev = time;

            this.slotsModel = new SlotsModel(node);
            time = System.currentTimeMillis();
            PerfLog.info(prev, time, "build slotsModel");
            prev = time;

            this.portsModel = new PortsModel(node);
            time = System.currentTimeMillis();
            PerfLog.info(prev, time, "build portsModel");
            prev = time;

            populateNodeInfo(node);
            time = System.currentTimeMillis();
            PerfLog.info(prev, time, "populateNodeInfo");
            prev = time;
            populateSlotInfo(node);
            time = System.currentTimeMillis();
            PerfLog.info(prev, time, "populateSlotInfo");
            prev = time;
            populateInterface(node);
            time = System.currentTimeMillis();
            PerfLog.info(prev, time, "populateInterface");
            prev = time;
            time = System.currentTimeMillis();
            PerfLog.info(prev, time, "NodeDetailPage() - end");
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static NodeElementFilter createFilter(NodeDto node) {
        NodeElementFilter filter = new NodeElementFilter(node);
        filter.setEnablePseudoWire(true);
        filter.setEnableVlan(true);
        filter.setEnableVpls(true);
        filter.setEnableVrf(true);
        filter.setEnableAtmPvc(true);
        filter.setEnableFrPvc(true);
        filter.setEnableRouterVlanIf(true);
        filter.setEnableHardPort(true);
        filter.setEnableIpIf(true);
        return filter;
    }

    private void populateNodeInfo(final NodeDto node) {
        node.renew();
        Label role = new Label("purpose", new Model<String>(NodeRenderer.getPurpose(node)));
        add(role);
        Label ip = new Label("managementIpAddress", new Model<String>(NodeRenderer.getManagementIpAddress(node)));
        add(ip);
        Label vendor = new Label("vendor", new Model<String>(NodeRenderer.getVendorName(node)));
        add(vendor);
        Label nodeType = new Label("nodeType", new Model<String>(NodeRenderer.getNodeType(node)));
        add(nodeType);
        Label applicanceType = new Label("applianceType", new Model<String>(NodeRenderer.getApplianceType(node)));
        add(applicanceType);
        Label resourcePermission = new Label("resourcePermission", new Model<String>(NodeRenderer.getResourcePermission(node)));
        add(resourcePermission);
        Label osType = new Label("osType", new Model<String>(NodeRenderer.getOsType(node)));
        add(osType);
        Label osVersion = new Label("osVersion", new Model<String>(NodeRenderer.getOsVersion(node)));
        add(osVersion);
        Label feature = new Label("feature", Model.of(NodeRenderer.getFeature(node)));
        add(feature);
        Label power = new Label("power", Model.of(NodeRenderer.getPower(node)));
        add(power);
        Label cpus = new Label("cpus", Model.of(NodeRenderer.getCPUs(node)));
        add(cpus);
        Label cpuProduct = new Label("cpuProduct", Model.of(NodeRenderer.getCPUProduct(node)));
        add(cpuProduct);
        Label memory = new Label("memory", Model.of(NodeRenderer.getMemory(node)));
        add(memory);
        Label storage = new Label("storage", Model.of(NodeRenderer.getStorage(node)));
        add(storage);
        Label portSummary = new Label("portSummary", Model.of(NodeRenderer.getPortSummary(node)));
        add(portSummary);
        BookmarkablePageLink<Void> locationLink = LocationUtil.getLocationLink("locationLink", NodeRenderer.getArea(node));
        add(locationLink);
        Label locationName = new Label("caption", NodeRenderer.getLocationName(node));
        locationLink.add(locationName);
        Label lastEditor = new Label("lastEditor", new Model<String>(NodeRenderer.getLastEditor(node)));
        add(lastEditor);
        Label lastEditTime = new Label("lastEditTime", new Model<String>(NodeRenderer.getLastEditTime(node)));
        add(lastEditTime);
        Label version = new Label("version", Model.of(NodeRenderer.getVersion(node)));
        add(version);

        Label note = new Label("note", new Model<String>(NodeRenderer.getNote(node)));
        add(note);
        Label nicProduct = new Label("nicProduct", new Model<String>(NodeRenderer.getNICProduct(node)));
        add(nicProduct);
        Label sfpPlusProduct = new Label("sfpPlusProduct", new Model<String>(NodeRenderer.getSFPPlusProduct(node)));
        add(sfpPlusProduct);

        NodeDto host = this.node.getVirtualizationHostNode();
        PageParameters params;
        if (host != null) {
            params = NodeUtil.getParameters(host);
        } else {
            params = new PageParameters();
        }
        BookmarkablePageLink<Void> parentLink = new BookmarkablePageLink<Void>("parentLink", SimpleNodeDetailPage.class, params);
        parentLink.setEnabled(host != null);
        parentLink.setVisible(host != null);
        Label parentCaption = new Label("caption", NodeRenderer.getNodeName(host));
        parentLink.add(parentCaption);
        add(parentLink);

        Link<Void> editNodeInfo = new Link<Void>("editNodeInfo") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                WebPage backPage = SimpleNodeDetailPage.this;
                if (NodeUtil.isVirtualNode(node)) {
                    NodeDto host = node.getVirtualizationHostNode();
                    setResponsePage(new VirtualNodeEditPage(backPage, node, host));
                } else {
                    setResponsePage(new NodeEditPage(backPage, node, null));
                }
            }
        };
        add(editNodeInfo);
        ExternalLink history = HistoryUtil.createHistoryLink(node, "History");
        history.setEnabled(CoreConfiguration.getInstance().isDebug());
        history.setVisible(CoreConfiguration.getInstance().isDebug());
        add(history);
    }

    private void populateSlotInfo(NodeDto node) {
        ListView<SlotDto> slotTable = new ListView<SlotDto>("slotTable", slotsModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<SlotDto> item) {
                final SlotDto slot = item.getModelObject();
                slot.renew();
                final ModuleDto module = slot.getModule();
                String endOfUseClass = null;
                if (NodeUtil.isEndOfUse(slot)) {
                    endOfUseClass = "end-of-use";
                }
                AttributeAppender appender = new AttributeAppender("class", new Model<String>(endOfUseClass), " ");
                item.add(appender);

                Link<Void> editSlot = new Link<Void>("editSlot") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick() {
                        setResponsePage(new SlotEditPage(SimpleNodeDetailPage.this, slot));
                    }
                };
                item.add(editSlot);

                String slotId = NameUtil.getIfName(slot);
                editSlot.add(new Label("slotId", new Model<String>(slotId)));

                String slotType = HardwareRenderer.getSlotType(slot);
                item.add(new Label("slotType", new Model<String>(slotType)));

                String moduleName = HardwareRenderer.getModuleType(module);
                item.add(new Label("moduleName", new Model<String>(moduleName)));

                String operStatus = HardwareRenderer.getOperStatus(slot);
                item.add(new Label("operStatus", new Model<String>(operStatus)));

                String note = HardwareRenderer.getNote(slot);
                item.add(new Label("note", new Model<String>(note)));

                Label lastEditor = new Label("lastEditor", new Model<String>(HardwareRenderer.getLastEditor(slot)));
                item.add(lastEditor);
                Label lastEditTime = new Label("lastEditTime", new Model<String>(HardwareRenderer.getLastEditTime(slot)));
                item.add(lastEditTime);
                Label version = new Label("version", Model.of(HardwareRenderer.getVersion(slot)));
                item.add(version);

                Form<Void> deleteForm = new Form<Void>("removeModule");
                item.add(deleteForm);
                SubmitLink removeModuleLink = new SubmitLink("apply") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit() {
                        ModuleRemovePage page = new ModuleRemovePage(SimpleNodeDetailPage.this, slot);
                        setResponsePage(page);
                    }
                };
                removeModuleLink.setEnabled(slot.getModule() != null);
                removeModuleLink.setVisible(slot.getModule() != null);
                deleteForm.add(removeModuleLink);

                try {
                    ExternalLink history = HistoryUtil.createHistoryLink(slot, "History");
                    history.setEnabled(CoreConfiguration.getInstance().isDebug());
                    history.setVisible(CoreConfiguration.getInstance().isDebug());
                    item.add(history);
                } catch (Exception e) {
                    throw ExceptionUtils.throwAsRuntime(e);
                }
            }
        };
        add(slotTable);
    }

    private void populateInterface(final NodeDto node) throws InventoryException {
        Link<Void> addLoopbackLink = new Link<Void>("addLoopback") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                LoopbackPortEditPage page = new LoopbackPortEditPage(SimpleNodeDetailPage.this, node, null);
                setResponsePage(page);
            }
        };
        add(addLoopbackLink);
        Link<Void> addPortLink = new Link<Void>("addVirtualPort") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                VirtualPortCreationPage page = new VirtualPortCreationPage(SimpleNodeDetailPage.this, node);
                setResponsePage(page);
            }
        };
        add(addPortLink);
        addPortLink.setEnabled(NodeRenderer.isVirtualNode(this.node));
        addPortLink.setVisible(NodeRenderer.isVirtualNode(this.node));
        Link<Void> addAliasLink = new Link<Void>("addAlias") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                AliasPortEditPage page = new AliasPortEditPage(SimpleNodeDetailPage.this, node, null);
                setResponsePage(page);
            }
        };
        add(addAliasLink);
        addAliasLink.setEnabled(NodeRenderer.isVirtualNode(this.node));
        addAliasLink.setVisible(NodeRenderer.isVirtualNode(this.node));

        PortPanel portPanel = new PortPanel("interfacesPanel", SimpleNodeDetailPage.this, portsModel, editorName);
        add(portPanel);

    }

    @Override
    public void onPageAttached() {
        this.startTime = System.currentTimeMillis();
        log().debug("onPageAttached() called.");
        super.onPageAttached();
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        log().info("page rendering time: " + (System.currentTimeMillis() - startTime));
        PerfLog.info(startTime, System.currentTimeMillis(), "SimpleNodeDetailPage:onPageAttach->onDetach");
    }

    @Override
    protected void onModelChanged() {
        try {
            log().debug("model changed.");
            this.node.renew();
            this.slotsModel.renew();
            this.portsModel.renew();
            super.onModelChanged();
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public NodeDto getNode() {
        return this.node;
    }

    public String getNodeLastEditTime() {
        String lastEditTime = NodeRenderer.getLastEditTime(node);
        return lastEditTime;
    }

    private Logger log() {
        return LoggerFactory.getLogger(SimpleNodeDetailPage.class);
    }

    private class SlotsModel extends AbstractReadOnlyModel<List<SlotDto>> {
        private static final long serialVersionUID = 1L;
        private final NodeDto node;
        private final List<SlotDto> slots = new ArrayList<SlotDto>();

        public SlotsModel(NodeDto node) {
            this.node = node;
            renew();
        }

        public synchronized List<SlotDto> getObject() {
            return slots;
        }

        public synchronized void renew() {
            List<SlotDto> newSlots = new ArrayList<SlotDto>();
            if (node.getChassises() != null) {
                for (ChassisDto chassis : node.getChassises()) {
                    if (chassis.getSlots() != null) {
                        for (SlotDto slot : chassis.getSlots()) {
                            addSlot(newSlots, slot);
                        }
                    }
                }
            }
            Collections.sort(newSlots, new NodeElementComparator());
            this.slots.clear();
            this.slots.addAll(newSlots);
        }

        private void addSlot(List<SlotDto> result, SlotDto slot) {
            if (slot == null) {
                return;
            }
            result.add(slot);
            if (slot.getModule() == null) {
                return;
            }
            ModuleDto module = slot.getModule();
            for (NodeElementDto elem : module.getSubElements()) {
                if (elem instanceof SlotDto) {
                    addSlot(result, (SlotDto) elem);
                }
            }
        }
    }

    ;
}