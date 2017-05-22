package voss.multilayernms.inventory.web.parts;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.PortFilter;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.web.node.NodePageUtil;
import voss.nms.inventory.util.Comparators;
import voss.nms.inventory.util.LinkUtil;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.NodeUtil;

import java.util.*;

public class MultiPortSelectionPanel extends Panel {
    private static final long serialVersionUID = 1L;
    private final List<InterfaceContainer> containers = new ArrayList<InterfaceContainer>();
    private final WebPage parentPage;

    public MultiPortSelectionPanel(String id, WebPage parent, String nodeName, List<PortDto> selectedInterfaces) {
        this(id, parent, getNode(nodeName), selectedInterfaces, new SimplePortFilter());
    }

    private static NodeDto getNode(String nodeName) {
        try {
            return MplsNmsInventoryConnector.getInstance().getNodeDto(nodeName);
        } catch (Exception e) {
            throw new IllegalStateException("illegal node name: " + nodeName);
        }
    }

    public MultiPortSelectionPanel(String id, WebPage parent, NodeDto node, List<PortDto> selectedInterfaces) {
        this(id, parent, node, selectedInterfaces, new SimplePortFilter());
    }

    public MultiPortSelectionPanel(String id, WebPage parent, NodeDto node, List<PortDto> selectedInterfaces, PortFilter listFilter) {
        this(id, parent, node, selectedInterfaces, listFilter, new SimplePortFilter());
    }

    public MultiPortSelectionPanel(String id, WebPage parent, NodeDto node, List<PortDto> selectedInterfaces,
                                   PortFilter listFilter, PortFilter checkFilter) {
        super(id);
        if (parent == null) {
            throw new IllegalArgumentException();
        }
        this.parentPage = parent;
        try {
            List<PortDto> ports;
            if (node == null) {
                ports = new ArrayList<PortDto>();
            } else {
                ports = NodeUtil.getWholePorts(node, listFilter);
            }
            final Map<PortDto, InterfaceContainer> map = new HashMap<PortDto, InterfaceContainer>();
            for (PortDto port : ports) {
                log().trace("selectedPorts:" + port.getAbsoluteName());
                boolean checked = selectedInterfaces.contains(port);
                if (!checked) {
                    PortDto bridge = NodeUtil.getBridgePort(port);
                    checked = selectedInterfaces.contains(bridge);
                }
                InterfaceContainer ct = new InterfaceContainer(port);
                ct.setChecked(checked);
                ct.setExist(checked);
                log().debug(port.getAbsoluteName() + "=>" + checkFilter.match(port));
                ct.setEnabled(checkFilter.match(port));
                this.containers.add(ct);
                map.put(port, ct);
            }
            Collections.sort(ports, Comparators.getIfNameBasedPortComparator());
            final ListView<PortDto> interfaceListView = new ListView<PortDto>("interfaces", ports) {
                private static final long serialVersionUID = 1L;

                @Override
                public void populateItem(ListItem<PortDto> item) {
                    final PortDto port = item.getModelObject();
                    final InterfaceContainer ct = map.get(port);
                    CheckBox cbox = new CheckBox("selector", new PropertyModel<Boolean>(ct, "checked"));
                    cbox.setEnabled(ct.isEnabled());
                    item.add(cbox);

                    String ifName = NameUtil.getIfName(port);
                    item.add(new Label("ifName", new Model<String>(ifName)));
                    item.add(new Label("ifName2", new Model<String>(ifName)));
                    item.add(new Label("operStatus", new Model<String>(PortRenderer.getOperStatus(port))));
                    item.add(new Label("interfaceType", new Model<String>(PortRenderer.getPortType(port))));
                    item.add(new Label("bandwidth", new Model<String>(PortRenderer.getBandwidth(port))));
                    item.add(new Label("purpose", new Model<String>(PortRenderer.getPurpose(port))));
                    item.add(new Label("note", new Model<String>(PortRenderer.getNote(port))));
                    item.add(new Label("channelTimeslot", new Model<String>(PortRenderer.getTimeSlot(port))));
                    item.add(new Label("channelGroupNumber", new Model<String>(PortRenderer.getChannelGroup(port))));

                    String portIpAddress = PortRenderer.getIpAddress(port);
                    String portIpMask1 = PortRenderer.getSubnetMask(port);
                    if (portIpAddress != null && portIpMask1 != null) {
                        portIpAddress = portIpAddress + "/" + portIpMask1;
                    }
                    item.add(new Label("portIpAddress", new Model<String>(portIpAddress)));
                    item.add(new Label("ospfAreaID", new Model<String>(PortRenderer.getOspfAreaID(port))));
                    item.add(new Label("vpi", new Model<String>(PortRenderer.getVpi(port))));
                    item.add(new Label("vci", new Model<String>(PortRenderer.getVci(port))));
                    item.add(new Label("routerVlanID", new Model<String>(PortRenderer.getRouterVlanID(port))));

                    Label lastEditor = new Label("lastEditor", new Model<String>(PortRenderer.getLastEditor(port)));
                    item.add(lastEditor);
                    Label lastEditTime = new Label("lastEditTime", new Model<String>(PortRenderer.getLastEditTime(port)));
                    item.add(lastEditTime);
                    Label version = new Label("version", Model.of(PortRenderer.getVersion(port)));
                    item.add(version);

                    PortDto l2Neighbor = NodeUtil.getLayer2Neighbor(port);
                    Link<Void> l2Link = NodePageUtil.createPortLink("layer3Neighbor", l2Neighbor);
                    item.add(l2Link);
                    Link<Void> editNeighbor2 = NodePageUtil.createNeighborEditLink(parentPage, "editNeighbor3", port);
                    editNeighbor2.setEnabled(LinkUtil.isL2LinkCapablePort(port));
                    editNeighbor2.setVisible(LinkUtil.isL2LinkCapablePort(port));
                    Label editNeighbor2Caption = new Label("caption", Model.of("Edit"));
                    editNeighbor2.add(editNeighbor2Caption);
                    item.add(editNeighbor2);
                }
            };
            add(interfaceListView);
        } catch (Exception e) {
            throw new IllegalStateException("unexpected exception", e);
        }
    }

    public List<PortDto> getSelected() {
        List<PortDto> interfaces = new ArrayList<PortDto>();
        for (InterfaceContainer ct : this.containers) {
            if (ct.isChecked()) {
                log().debug("selected: " + ct.getPort().getAbsoluteName());
                interfaces.add(ct.getPort());
            }
        }
        return interfaces;
    }

    public List<InterfaceContainer> getListed() {
        return this.containers;
    }

    public WebPage getParentPage() {
        return this.parentPage;
    }

    private Logger log() {
        return LoggerFactory.getLogger(MultiPortSelectionPanel.class);
    }
}