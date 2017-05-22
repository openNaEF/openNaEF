package voss.multilayernms.inventory.web.customer;


import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VlanSelectionPanel extends Panel {
	private static final long serialVersionUID = 1L;
	private String vlanName;

	public VlanSelectionPanel(String id, String vlanName) {
		super(id);
		this.vlanName = vlanName;
		try{
			final MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
			List<String> vlans = new ArrayList<String>();
			conn.getVlanPool("vlans").getUsers().forEach(vlanDto -> vlans.add(vlanDto.getVlanId().toString()));
			DropDownChoice<String> field = new DropDownChoice<String>("vlanName",
					new PropertyModel<String>(this, "vlanName"), vlans);
			field.setRequired(true);
			add(field);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ExternalServiceException e) {
			e.printStackTrace();
		}

	}

	public String getVlanName() {
		return this.vlanName;
	}

	public void setVlanName(String name) {
		this.vlanName = name;
	}

}
