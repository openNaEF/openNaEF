package voss.multilayernms.inventory.web.parts;


import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.web.node.SimpleNodeDetailPage;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.WicketUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NodeSelectionPanel extends Panel {
	private static final long serialVersionUID = 1L;
	private String nodeName;
	
	public NodeSelectionPanel(String id, String nodeName) {
		super(id);
		this.nodeName = nodeName;
		try{
			final MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
			List<String> nodes = new ArrayList<String>();
			conn.getActiveNodes().forEach(nodeDto -> nodes.add(nodeDto.getName()));
			DropDownChoice<String> field = new DropDownChoice<String>("nodeName",
					new PropertyModel<String>(this, "nodeName"), nodes);
			field.setRequired(true);
			add(field);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ExternalServiceException e) {
			e.printStackTrace();
		}


		Link<Void> link = new Link<Void>("nodeLink") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onClick() {
				String nodeName = getNodeName();
				if (nodeName == null) {
					return;
				}
				setResponsePage(SimpleNodeDetailPage.class, NodeUtil.getNodeParameters(nodeName));
			}
		};
		WicketUtil.toPopup(link);
		link.setEnabled(nodeName != null);
		add(link);
	}

	public String getNodeName() {
		return this.nodeName;
	}
	
	public void setNodeName(String name) {
		this.nodeName = name;
	}

}
