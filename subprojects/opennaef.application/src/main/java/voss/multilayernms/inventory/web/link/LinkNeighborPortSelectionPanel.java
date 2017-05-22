package voss.multilayernms.inventory.web.link;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.PortFilter;
import voss.core.server.util.Util;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.web.parts.SimplePortFilter;
import voss.nms.inventory.util.Comparators;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.NodeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LinkNeighborPortSelectionPanel extends Panel {
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(LinkNeighborPortSelectionPanel.class);
    private PortDto selected = null;
    private final WebPage parentPage;

    public LinkNeighborPortSelectionPanel(String id, WebPage parent, String nodeName, PortDto selectedInterface) {
        this(id, parent, nodeName, selectedInterface, new SimplePortFilter());
    }

    public LinkNeighborPortSelectionPanel(String id, WebPage parent, String nodeName, PortDto selectedInterface,
                                          PortFilter filter) {
        this(id, parent, nodeName, selectedInterface, filter, new SimplePortFilter());
    }

    public LinkNeighborPortSelectionPanel(final String id, final WebPage parent, final String nodeName,
                                          final PortDto selectedInterface, final PortFilter filter, final PortFilter checkFilter) {
        super(id);
        if (Util.isNull(parent)) {
            throw new IllegalArgumentException();
        }
        this.parentPage = parent;
        try {
            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            NodeDto node = conn.getNodeDto(nodeName);
            List<PortDto> ports;
            if (node == null) {
                ports = new ArrayList<PortDto>();
            } else {
                ports = NodeUtil.getWholePorts(node, filter);
            }
            for (PortDto port : ports) {
                if (selectedInterface != null) {
                    if (DtoUtil.mvoEquals(selectedInterface, port)) {
                        this.selected = port;
                        break;
                    }
                }
            }

            final RadioGroup<PortDto> radios = new RadioGroup<PortDto>(
                    "radios", new PropertyModel<PortDto>(this, "selected"));

            Collections.sort(ports, Comparators.getIfNameBasedPortComparator());
            final ListView<PortDto> interfaceListView = new ListView<PortDto>("interfaces", ports) {
                private static final long serialVersionUID = 1L;

                @Override
                public void populateItem(ListItem<PortDto> item) {
                    final PortDto port = item.getModelObject();

                    Radio<PortDto> radio = new Radio<PortDto>("selector", new Model<PortDto>(port));
                    if (checkFilter.match(port)) {
                        radios.add(radio);
                    } else {
                        radio.setVisible(false);
                    }
                    item.add(radio);

                    String ifName = NameUtil.getIfName(port);
                    item.add(new Label("ifName", new Model<String>(ifName)));
                    item.add(new Label("ifName2", new Model<String>(ifName)));
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

                }
            };
            radios.add(interfaceListView);
            add(radios);
        } catch (Exception e) {
            throw new IllegalStateException("unexpected exception", e);
        }
    }

    public WebPage getParentPage() {
        return this.parentPage;
    }

    public void setSelected(PortDto port) {
        this.selected = port;
    }

    public PortDto getSelected() {
        return this.selected;
    }
}