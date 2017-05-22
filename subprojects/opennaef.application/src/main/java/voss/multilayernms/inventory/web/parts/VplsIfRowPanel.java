package voss.multilayernms.inventory.web.parts;

import naef.dto.PortDto;
import naef.dto.vpls.VplsDto;
import naef.dto.vpls.VplsIfDto;
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
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.PerfLog;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.renderer.VplsRenderer;
import voss.multilayernms.inventory.web.node.SimpleNodeDetailPage;
import voss.multilayernms.inventory.web.vpls.VplsEditPage;
import voss.nms.inventory.util.HistoryUtil;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.NodeUtil;

import java.util.ArrayList;
import java.util.List;

public class VplsIfRowPanel extends Panel {
    private static final long serialVersionUID = 1L;

    public VplsIfRowPanel(String id, final WebPage parentPage, final VplsIfDto vplsIf, final String editorName) {
        super(id);
        try {
            long time = System.currentTimeMillis();
            PerfLog.info(time, time, "populateInterface.populateItem", "begin.");

            long prev = time;
            time = System.currentTimeMillis();
            vplsIf.renew();
            final VplsDto vpls = vplsIf.getTrafficDomain();
            prev = time;
            time = System.currentTimeMillis();
            String endOfUseClass = null;
            if (NodeUtil.isEndOfUse(vplsIf)) {
                endOfUseClass = "end-of-use";
            }
            AttributeAppender appender = new AttributeAppender("class", new Model<String>(endOfUseClass), " ");
            add(appender);

            String vplsId;
            if (vplsIf.getVplsId() != null) {
                vplsId = vplsIf.getVplsId().toString();
            } else {
                vplsId = null;
            }
            Link<Void> editLink = new Link<Void>("editInterface") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    vplsIf.renew();
                    if (vplsIf instanceof VplsIfDto) {
                        VplsEditPage page = new VplsEditPage(getWebPage(),
                                vpls);
                        setResponsePage(page);
                    } else {
                        throw new IllegalStateException("unexpected type: " + vplsIf.getAbsoluteName());
                    }
                }
            };
            editLink.add(new Label("vplsId", new Model<String>(vplsId)));
            add(editLink);

            List<PortDto> attachedPorts = new ArrayList<PortDto>(vplsIf.getAttachedPorts());
            ListView<PortDto> attachedPortList = new ListView<PortDto>("attachedPorts", attachedPorts) {
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
                    String fqn = NameUtil.getNodeIfName(port);
                    Label label = new Label("fqn", new Model<String>(fqn));
                    nodeRef.add(label);
                }
            };
            add(attachedPortList);

            add(new Label("note", new Model<String>(PortRenderer.getNote(vpls))));

            String portIpAddress = PortRenderer.getIpAddress(vplsIf);
            String portIpMask1 = PortRenderer.getSubnetMask(vplsIf);
            if (portIpAddress != null && portIpMask1 != null) {
                portIpAddress = portIpAddress + "/" + portIpMask1;
            }

            Label lastEditor = new Label("lastEditor", new Model<String>(VplsRenderer.getLastEditor(vpls)));
            add(lastEditor);
            Label lastEditTime = new Label("lastEditTime", new Model<String>(VplsRenderer.getLastEditTime(vpls)));
            add(lastEditTime);
            Label version = new Label("version", Model.of(VplsRenderer.getVersion(vpls)));
            add(version);
            ExternalLink history = HistoryUtil.createHistoryLink(vplsIf, "History");
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