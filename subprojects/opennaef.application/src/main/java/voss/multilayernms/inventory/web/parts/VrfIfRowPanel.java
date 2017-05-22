package voss.multilayernms.inventory.web.parts;

import naef.dto.PortDto;
import naef.dto.vrf.VrfDto;
import naef.dto.vrf.VrfIfDto;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import voss.core.server.config.CoreConfiguration;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.PerfLog;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.renderer.VrfRenderer;
import voss.multilayernms.inventory.web.node.SimpleNodeDetailPage;
import voss.multilayernms.inventory.web.vrf.VrfEditPage;
import voss.nms.inventory.util.HistoryUtil;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.NodeUtil;

import java.util.ArrayList;
import java.util.List;

public class VrfIfRowPanel extends Panel {
    private static final long serialVersionUID = 1L;

    public VrfIfRowPanel(String id, final WebPage parentPage, final VrfIfDto vrfIf, final String editorName) {
        super(id);
        try {
            long time = System.currentTimeMillis();
            PerfLog.info(time, time, "populateInterface.populateItem", "begin.");
            long prev = time;
            time = System.currentTimeMillis();
            vrfIf.renew();
            final VrfDto vrf = vrfIf.getTrafficDomain();
            prev = time;
            time = System.currentTimeMillis();
            String endOfUseClass = null;
            if (NodeUtil.isEndOfUse(vrfIf)) {
                endOfUseClass = "end-of-use";
            }
            AttributeAppender appender = new AttributeAppender("class", new Model<String>(endOfUseClass), " ");
            add(appender);

            String vrfId;
            if (vrfIf.getTrafficDomain() != null) {
                vrfId = vrfIf.getTrafficDomain().getStringId().toString();
            } else {
                vrfId = null;
            }
            Link<Void> editLink = new Link<Void>("edit") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    vrfIf.renew();
                    if (vrfIf instanceof VrfIfDto) {
                        VrfEditPage page = new VrfEditPage(getWebPage(), vrf);
                        setResponsePage(page);
                    } else {
                        throw new IllegalStateException("unexpected type: " + vrfIf.getAbsoluteName());
                    }
                }
            };
            editLink.add(new Label("id", new Model<String>(vrfId)));
            add(editLink);
            Label vrfIfLabel = new Label("routingInstanceName", DtoUtil.getStringOrNull(vrf, "RoutingInstance名"));
            add(vrfIfLabel);
            Label nodeNameLabel = new Label("nodeName", vrfIf.getNode().getName());
            add(nodeNameLabel);

            List<PortDto> attachedPorts = new ArrayList<PortDto>(vrfIf.getAttachedPorts());
            ListView<PortDto> attachedPortList = new ListView<PortDto>("attachmentPorts", attachedPorts) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<PortDto> item) {
                    final PortDto port = item.getModelObject();
                    Link<Void> nodeRef = new Link<Void>("nodeReference") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick() {
                            setResponsePage(SimpleNodeDetailPage.class, NodeUtil.getNodeParameters(port));
                        }
                    };
                    item.add(nodeRef);
                    String fqn = NameUtil.getIfName(port);
                    Label label = new Label("attachmentPort", new Model<String>(fqn));
                    nodeRef.add(label);
                }
            };
            add(attachedPortList);
            add(new Label("purpose", new Model<String>(PortRenderer.getPurpose(vrf))));
            add(new Label("note", new Model<String>(PortRenderer.getNote(vrf))));

            String portIpAddress = PortRenderer.getIpAddress(vrfIf);
            String portIpMask1 = PortRenderer.getSubnetMask(vrfIf);
            if (portIpAddress != null && portIpMask1 != null) {
                portIpAddress = portIpAddress + "/" + portIpMask1;
            }

            Label lastEditor = new Label("lastEditor", new Model<String>(VrfRenderer.getLastEditor(vrf)));
            add(lastEditor);
            Label lastEditTime = new Label("lastEditTime", new Model<String>(VrfRenderer.getLastEditor(vrf)));
            add(lastEditTime);
            Label version = new Label("version", Model.of(VrfRenderer.getVersion(vrf)));
            add(version);
            ExternalLink history = HistoryUtil.createHistoryLink(vrfIf, "History");
            history.setEnabled(CoreConfiguration.getInstance().isDebug());
            history.setVisible(CoreConfiguration.getInstance().isDebug());
            add(history);

            prev = time;
            time = System.currentTimeMillis();
            PerfLog.info(prev, time, "populateInterface.populateItem", "end.");
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public WebPage createDestinationSelector(PortDto port) {
        DestinationSelectionPage page = new DestinationSelectionPage(getWebPage(), port);
        return page;
    }

}