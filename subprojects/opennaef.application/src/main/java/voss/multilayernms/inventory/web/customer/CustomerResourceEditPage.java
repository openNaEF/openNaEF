package voss.multilayernms.inventory.web.customer;

import naef.dto.NaefDto;
import naef.dto.NetworkDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.vlan.VlanDto;
import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.datetime.PatternDateConverter;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import pasaran.naef.dto.CustomerInfo2dDto;
import tef.DateTime;
import tef.DateTimeFormat;
import tef.skelton.dto.EntityDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.*;
import voss.multilayernms.inventory.web.node.SimpleNodeDetailPage;
import voss.multilayernms.inventory.web.parts.NodeSelectionPanel;
import voss.nms.inventory.builder.CustomerInfo2dReferencesBuilder;
import voss.nms.inventory.builder.IpSubnetCommandBuilder;
import voss.nms.inventory.util.*;
import voss.nms.inventory.util.Comparators;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CustomerResourceEditPage extends WebPage {
    public static final String OPERATION_NAME = "CustomerInfoEdit";
    public static final String DATE_PATTERN = "yyyy/MM/dd";
    private final WebPage backPage;
    private final CustomerInfo2dDto user;
    private final String editorName;

    private String name = null;
    private String companyID = null;
    private boolean active = true;

    private final NodeSelectionPanel nodepanel;
    private final DateTextField nodeDateTextField;
    private String nodeName = null;
    private Date nodeTime;

    private final NodeSelectionPanel nodepanel2;
    private final PortSelectionPanel portPanel;
    private final DateTextField portDateTextField;
    private Date portTime;

    private final VlanSelectionPanel vlanPanel;
    private final DateTextField vlanDateTextField;
    private Date vlanTime;

    private final SubnetSelectionPanel subnetPanel;
    private final DateTextField subnetDateTextField;
    private Date subnetTime;

    private final Calendar TODAY;

    public CustomerResourceEditPage(WebPage backPage, CustomerInfo2dDto user, String nodeName) {
        TODAY = Calendar.getInstance();
        TODAY.set(Calendar.HOUR_OF_DAY, 0);
        TODAY.set(Calendar.MINUTE, 0);
        TODAY.set(Calendar.SECOND, 0);
        TODAY.set(Calendar.MILLISECOND, 0);

        this.nodeTime = TODAY.getTime();
        this.portTime = TODAY.getTime();
        this.vlanTime = TODAY.getTime();
        this.subnetTime = TODAY.getTime();

        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);

            this.nodeName = nodeName;
            if (backPage == null) {
                throw new IllegalArgumentException();
            }
            if (user != null) {
                user.renew();
            }
            this.backPage = backPage;
            this.user = user;

            final List<EntityDto> references = getOccupancyObjects(this.user);
            final Map<String, String> occupancies = getOccupancyString(this.user);
            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            BookmarkablePageLink<Void> refresh = new BookmarkablePageLink<Void>("refresh", CustomerResourceEditPage.class);
            add(refresh);
            BookmarkablePageLink<Void> back = new BookmarkablePageLink<Void>("back", CustomerInfoListPage.class);
            add(back);
            add(new FeedbackPanel("feedback"));

            Label userNameLabel = new Label("customerInfo", user.getName());
            add(userNameLabel);

            Form<Void> form = new Form<Void>("form") {};
            add(form);
            final CustomerInfoRenderer renderer = new CustomerInfoRenderer(user);
            Label nameField = new Label("name",renderer.getName());
            form.add(nameField);
            Label companyIDField = new Label("companyID",renderer.getCompanyID());
            form.add(companyIDField);

            Form<Void> nodeform = new Form<Void>("nodeSelectionForm");
            List<NodeDto> nodes = filter(references, NodeDto.class);
            ListView<NodeDto> nodeList = new ListView<NodeDto>("nodes", nodes) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<NodeDto> item) {
                    NodeDto node = item.getModelObject();
                    Link<Void> link = getLink(node);
                    item.add(link);
                    Label label = new Label("nodeName", NodeRenderer.getNodeName(node));
                    link.add(label);
                    item.add(new Label("applianceType", NodeRenderer.getApplianceType(node)));
                    item.add(new Label("resourcePermission", NodeRenderer.getResourcePermission(node)));
                    item.add(new Label("purpose", NodeRenderer.getPurpose(node)));
                    item.add(new Label("nodeType", NodeRenderer.getNodeType(node)));
                    item.add(new Label("vendorName", NodeRenderer.getVendorName(node)));
                    item.add(new Label("osType", NodeRenderer.getOsType(node)));
                    item.add(new Label("osVersion", NodeRenderer.getOsVersion(node)));
                    item.add(new Label("managementIpAddress", NodeRenderer.getManagementIpAddress(node)));

                    item.add(new MultiLineLabel("occupancy", Objects.toString(
                            occupancies.get(node.getOid().toString()),
                            "")));

                    RemoveTime removeTimeModel = new RemoveTime(TODAY);
                    item.add(createDateTextField(removeTimeModel, "removeTime", "time"));
                    Link<Void> removeNodeResource = new Link<Void>("removeNodeResource") {
                        private static final long serialVersionUID = 1L;
                        public void onClick() {
                            removeNaefResourceFromCustomer(removeTimeModel.time, NaefDto.class.cast(node));
                            setResponsePage(new CustomerResourceEditPage(this.getWebPage(), user, null));
                        }
                    };
                    item.add(removeNodeResource);
                }

                private BookmarkablePageLink<Void> getLink(NodeDto node) {
                    if (node == null) {
                        throw new IllegalStateException("node is null.");
                    }
                    PageParameters param = NodeUtil.getParameters(node);
                    BookmarkablePageLink<Void> link = new BookmarkablePageLink<Void>("nodeNameLink", SimpleNodeDetailPage.class, param);
                    return link;
                }
            };
            nodeform.add(nodeList);
            this.nodepanel = new NodeSelectionPanel("nodeSelectionPanel", null);
            nodeform.add(this.nodepanel);

            this.nodeDateTextField = createDateTextField(this, "nodeDateTextField", "nodeTime");
            nodeform.add(this.nodeDateTextField);

            Button selectNodeButton = new Button("addNodeResource") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        if (nodepanel.getNodeName() == null) {
                            return;
                        } else if (NodeUtil.getNode(nodepanel.getNodeName()) == null) {
                            throw new InventoryException("No node found with specified name: "
                                    + nodepanel.getNodeName());
                        }
                        addNaefResourceToCustomer(nodeTime, NaefDto.class.cast(NodeUtil.getNode(nodepanel.getNodeName())));
                        setResponsePage(new CustomerResourceEditPage(this.getWebPage(), user, null));
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            nodeform.add(selectNodeButton);
            add(nodeform);

            Form<Void> portResourceForm = new Form<Void>("portResourceForm");
            add(portResourceForm);

            List<PortDto> ports = filter(references, PortDto.class);
            Collections.sort(ports, Comparators.getIfNameBasedPortComparator());
            final ListView<PortDto> interfaceListView = new ListView<PortDto>("interfaces", ports) {
                private static final long serialVersionUID = 1L;

                @Override
                public void populateItem(ListItem<PortDto> item) {
                    final PortDto port = item.getModelObject();
                    item.add(new Label("nodeName", new Model<String>(port.getNode().getName())));
                    String ifName = NameUtil.getIfName(port);
                    item.add(new Label("ifName", new Model<String>(ifName)));
                    item.add(new Label("resourcePermission", new Model<String>(PortRenderer.getResourcePermission(port))));
                    item.add(new Label("interfaceType", new Model<String>(PortRenderer.getPortType(port))));
                    item.add(new Label("bandwidth", new Model<String>(PortRenderer.getBandwidth(port))));
                    item.add(new Label("connected", new Model<String>(PortRenderer.getConnected(port))));
                    item.add(new Label("interimConnected", new Model<String>(PortRenderer.getInterimConnected(port))));
                    Label lastEditor = new Label("lastEditor", new Model<String>(PortRenderer.getLastEditor(port)));
                    item.add(lastEditor);
                    Label lastEditTime = new Label("lastEditTime", new Model<String>(PortRenderer.getLastEditTime(port)));
                    item.add(lastEditTime);
                    Label version = new Label("version", Model.of(PortRenderer.getVersion(port)));
                    item.add(version);
                    item.add(new MultiLineLabel("occupancy", Objects.toString(
                            occupancies.get(port.getOid().toString()),
                            "")));

                    RemoveTime removeTimeModel = new RemoveTime(TODAY);
                    item.add(createDateTextField(removeTimeModel, "removeTime", "time"));
                    Link<Void> removePortResource = new Link<Void>("removePortResource") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            removeNaefResourceFromCustomer(removeTimeModel.time, NaefDto.class.cast(port));
                            setResponsePage(new CustomerResourceEditPage(this.getWebPage(), user, null));
                        }
                    };
                    item.add(removePortResource);
                }
            };
            portResourceForm.add(interfaceListView);

            Form<Void> portSelectionForm = new Form<Void>("portSelectionForm");
            add(portSelectionForm);
            Button selectNodeButton2 = new Button("selectNode") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        if (nodepanel2.getNodeName() == null) {
                            return;
                        } else if (NodeUtil.getNode(nodepanel2.getNodeName()) == null) {
                            throw new InventoryException("No node found with specified name: "
                                    + nodepanel2.getNodeName());
                        }
                        setResponsePage(new CustomerResourceEditPage(this.getWebPage(), user, nodepanel2.getNodeName()));
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            portSelectionForm.add(selectNodeButton2);

            Form<Void> portSelectionForm2 = new Form<Void>("portSelectionForm2");
            add(portSelectionForm2);

            this.nodepanel2 = new NodeSelectionPanel("nodeSelectionPanel2", nodeName);
            portSelectionForm.add(this.nodepanel2);

            this.portDateTextField = createDateTextField(this, "portDateTextField", "portTime");
            portSelectionForm2.add(this.portDateTextField);

            Button proceedButton = new Button("addPortResource") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    if (getSelectedPort() == null) {
                        return;
                    }
                    try {
                        addNaefResourceToCustomer(portTime, NaefDto.class.cast(getSelectedPort()));
                        setResponsePage(new CustomerResourceEditPage(this.getWebPage(), user, null));
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            portSelectionForm2.add(proceedButton);

            this.portPanel = new PortSelectionPanel(
                    "portSelectionPanel2", CustomerResourceEditPage.this, nodeName, getSelectedPort(), null);
            portSelectionForm2.add(this.portPanel);

            Form<Void> vlanResourceForm = new Form<Void>("vlanResourceForm");
            add(vlanResourceForm);
            List<VlanDto> vlans = filter(references, VlanDto.class);
            ListView<VlanDto> vlanList = new ListView<VlanDto>("vlans", vlans) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<VlanDto> item) {
                    VlanDto vlan = item.getModelObject();
                    item.add(new Label("vlanId", VlanRenderer.getVlanId(vlan)));
                    item.add(new Label("operStatus", VlanRenderer.getOperStatus(vlan)));
                    item.add(new Label("note", VlanRenderer.getNote(vlan)));
                    item.add(new Label("lastEditor", VlanRenderer.getLastEditor(vlan)));
                    item.add(new Label("lastEditTime", VlanRenderer.getLastEditTime(vlan)));
                    item.add(new Label("history", VlanRenderer.getVersion(vlan)));
                    item.add(new MultiLineLabel("occupancy", Objects.toString(
                            occupancies.get(vlan.getOid().toString()),
                            "")));

                    RemoveTime removeTimeModel = new RemoveTime(TODAY);
                    item.add(createDateTextField(removeTimeModel, "removeTime", "time"));
                    Link<Void> removeVlanResource = new Link<Void>("removeVlanResource") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            removeNaefResourceFromCustomer(removeTimeModel.time, NaefDto.class.cast(vlan));
                            setResponsePage(new CustomerResourceEditPage(this.getWebPage(), user, null));
                        }
                    };
                    item.add(removeVlanResource);
                }
            };
            vlanResourceForm.add(vlanList);

            this.vlanPanel = new VlanSelectionPanel("vlanSelectionPanel", null);
            vlanResourceForm.add(this.vlanPanel);

            this.vlanDateTextField = createDateTextField(this, "vlanDateTextField", "vlanTime");
            vlanResourceForm.add(this.vlanDateTextField);

            Button selectVlanButton = new Button("addVlanResource") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        if (vlanPanel.getVlanName() == null) {
                            return;
                        }
                        final MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
                        addNaefResourceToCustomer(vlanTime, NaefDto.class.cast(VlanUtil.getVlan(conn.getVlanPool("vlans"),Integer.valueOf(vlanPanel.getVlanName()))));
                        setResponsePage(new CustomerResourceEditPage(this.getWebPage(), user, null));
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            vlanResourceForm.add(selectVlanButton);
            add(vlanResourceForm);


            Form<Void> subnetResourceForm = new Form<Void>("subnetResourceForm");
            add(subnetResourceForm);
            List<IpSubnetDto> subnets = filter(references, IpSubnetDto.class);
            ListView<IpSubnetDto> subList = new ListView<IpSubnetDto>("subnets", subnets) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<IpSubnetDto> item) {
                    IpSubnetDto subnet = item.getModelObject();
                    VlanDto vlan = null;
                    for(NetworkDto net : subnet.getLowerLayerLinks()){
                        if (net instanceof VlanDto) { vlan = VlanDto.class.cast(net); }
                    }
                    if(subnet.getSubnetAddress() != null && subnet.getSubnetAddress().getName() != null) {
                        String vpnPrefix = SubnetRenderer.getVpnPrefix(subnet);
                        String address = SubnetRenderer.getIpAddress(subnet);
                        String mask = SubnetRenderer.getSubnetMask(subnet);
                        if(vpnPrefix != null){
                            item.add(new Label("subnet", vpnPrefix + "/" + address + "/" + mask));

                        } else {
                            item.add(new Label("subnet", address + "/" + mask));
                        }
                    }
                    item.add(new Label("vlanId", vlan == null ? null : VlanRenderer.getVlanId(vlan)));
                    item.add(new Label("lastEditor", SubnetRenderer.getLastEditor(subnet)));
                    item.add(new Label("lastEditTime", SubnetRenderer.getLastEditTime(subnet)));
                    item.add(new Label("history", SubnetRenderer.getVersion(subnet)));
                    item.add(new MultiLineLabel("occupancy", Objects.toString(
                            occupancies.get(subnet.getOid().toString()),
                            "")));

                    RemoveTime removeTimeModel = new RemoveTime(TODAY);
                    item.add(createDateTextField(removeTimeModel, "removeTime", "time"));
                    Link<Void> removeSubnetResource = new Link<Void>("removeSubnetResource") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            removeNaefResourceFromCustomer(removeTimeModel.time, NaefDto.class.cast(subnet));
                            setResponsePage(new CustomerResourceEditPage(this.getWebPage(), user, null));
                        }
                    };
                    item.add(removeSubnetResource);
                }
            };
            subnetResourceForm.add(subList);

            List<VlanDto> vlanDtos = filter(references, VlanDto.class);
            this.subnetPanel = new SubnetSelectionPanel("subnetSelectionPanel", vlanDtos);
            subnetResourceForm.add(this.subnetPanel);

            this.subnetDateTextField = createDateTextField(this, "subnetDateTextField", "subnetTime");
            subnetResourceForm.add(this.subnetDateTextField);

            Button selectSubnetButton = new Button("addSubnetResource") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        if (subnetPanel.getSubnetName() == null) {
                            return;
                        }
                        final MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
                        addLowerLayerNetwork(VlanUtil.getVlan(conn.getVlanPool("vlans"),Integer.valueOf(subnetPanel.getVlanName())),subnetPanel.getSubnet(subnetPanel.getSubnetName()));
                        addNaefResourceToCustomer(subnetTime, NaefDto.class.cast(subnetPanel.getSubnet(subnetPanel.getSubnetName())));
                        setResponsePage(new CustomerResourceEditPage(this.getWebPage(), user, null));
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            subnetResourceForm.add(selectSubnetButton);
            add(subnetResourceForm);

        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static DateTextField createDateTextField(Object self, String wicketId, String fieldName) {
        DateTextField textField = new DateTextField(
                wicketId,
                new PropertyModel<>(self, fieldName),
                new PatternDateConverter("yyyy/MM/dd", true));

        DatePicker datePicker = new DatePicker() {
            @Override
            protected String getAdditionalJavascript() {
                return "${calendar}.cfg.setProperty(\"navigator\",true,false); ${calendar}.render();";
            }
        };
        datePicker.setShowOnFieldClick(true);
        textField.add(datePicker);

        textField.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                Component component = this.getComponent();
                if (component instanceof DateTextField) {
                    System.out.printf("input changed: %s \n", this.getComponent().getDefaultModelObject());
                }
            }
        });

        return textField;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompanyID() {
        return companyID;
    }

    public void setCompanyID(String companyID) {
        this.companyID = companyID;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getNodeName() {
        return this.nodeName;
    }

    public void setNodeName(String name) {
        this.nodeName = name;
    }

    public PortDto getSelectedPort() {
        if(this.portPanel != null){
            return this.portPanel.getSelected();
        }else{
            return null;
        }
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    @Override
    protected void onModelChanged() {
        this.user.renew();
        super.onModelChanged();
    }

    private static List<EntityDto> getOccupancyObjects(CustomerInfo2dDto ci) {
        return Collections.unmodifiableList(
                ci.getReferences2dChanges().values().stream()
                .collect(
                        HashSet<EntityDto.Desc<NaefDto>>::new,
                        Set::addAll,
                        Set::addAll
                )
                .stream()
                .map(ci::toDto)
                .collect(Collectors.toList()));
    }

    private static <T  extends EntityDto> List<T> filter(List<EntityDto> occupancies, Class<T> target){
        if (target == null) return Collections.emptyList();
        return occupancies.stream()
                .filter(target::isInstance)
                .map(target::cast)
                .collect(Collectors.toList());
    }

    private static Map<String, String> getOccupancyString(CustomerInfo2dDto ci) {
        Map<Long, List<EntityDto.Desc<NaefDto>>> changes = new TreeMap<>(ci.getReferences2dChanges());

        Map<String, StringBuilder> occupancy = new HashMap<>();

        List<EntityDto.Desc<NaefDto>> prev = Collections.emptyList();
        for (Map.Entry<Long, List<EntityDto.Desc<NaefDto>>> entry : changes.entrySet()) {
            final Long time = entry.getKey();
            final List<EntityDto.Desc<NaefDto>> current = Collections.unmodifiableList(entry.getValue());

            for (EntityDto.Desc<NaefDto> desc : current) {
                occupancy.putIfAbsent(desc.oid().toString(), new StringBuilder());
                if (!prev.contains(desc)) {
                    occupancy.get(desc.oid().toString())
                            .append(DateTimeFormat.YMDHMS_SLASH.format(time))
                            .append(" - ");
                }
            }

            final List<EntityDto.Desc<NaefDto>> expect = new ArrayList<>(prev);
            expect.removeAll(current);
            expect.stream()
                    .filter(desc -> !current.contains(desc))
                    .forEach(desc -> {
                        occupancy.get(desc.oid().toString())
                                .append(DateTimeFormat.YMDHMS_SLASH.format(time))
                                .append("\n");
                    });
            prev = current;
        }

        return occupancy.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().toString().trim()));
    }

    private void addNaefResourceToCustomer(Date time, NaefDto dto) {
        try {
            DateTime targetTime = time != null ? new DateTime(time.getTime()) : new DateTime(TODAY.getTimeInMillis());
            CustomerInfo2dReferencesBuilder builder;
            builder = new CustomerInfo2dReferencesBuilder(this.user, this.editorName);
            builder.addReference(targetTime, dto);
            build(builder);
        }catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void removeNaefResourceFromCustomer(Date time, NaefDto dto) {
        try {
            DateTime targetTime = time != null ? new DateTime(time.getTime()) : new DateTime(TODAY.getTimeInMillis());
            CustomerInfo2dReferencesBuilder builder;
            builder = new CustomerInfo2dReferencesBuilder(this.user, this.editorName);
            builder.removeReference(targetTime, dto.getAbsoluteName());
            build(builder);
        }catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private static void build(CustomerInfo2dReferencesBuilder builder) throws ExternalServiceException, IOException, InventoryException {
        BuildResult result = builder.buildCommand();
        switch (result) {
            case SUCCESS:
                ShellConnector.getInstance().execute(builder);
                return;
            case NO_CHANGES:
                return;
            case FAIL:
            default:
                throw new IllegalStateException("unexpected build result: " + result);
        }
    }

    public static class RemoveTime {
        public Date time;

        public RemoveTime(Calendar defaultValue) {
            time = defaultValue.getTime();
        }
    }

    private void addLowerLayerNetwork(VlanDto vlan, IpSubnetDto subnet) {
        if(vlan == null || subnet == null){
            throw new IllegalArgumentException("pls select both vlan and subnet.");
        }
        try {
            IpSubnetCommandBuilder builder = new IpSubnetCommandBuilder(subnet, this.editorName);
            builder.addLowerLayerNetwork(vlan);
            BuildResult result = builder.buildCommand();
            switch (result) {
                case SUCCESS:
                    ShellConnector.getInstance().execute(builder);
                    return;
                case NO_CHANGES:
                    return;
                case FAIL:
                default:
                    throw new IllegalStateException("unexpected build result: " + result);
            }
        }catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

}
