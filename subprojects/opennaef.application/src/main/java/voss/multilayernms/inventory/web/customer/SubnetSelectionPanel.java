package voss.multilayernms.inventory.web.customer;


import naef.dto.ip.IpSubnetDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import naef.dto.vlan.VlanDto;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.renderer.SubnetRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubnetSelectionPanel extends Panel {
	private static final long serialVersionUID = 1L;
	private String vlanName;
	private String subnetName;
	private List<String> vlans = new ArrayList<>();
	private List<String> subnets;
	private Map<String,IpSubnetDto> subnetsMap;

	public SubnetSelectionPanel(String id, List<VlanDto> vlanDtos) {
		super(id);
		this.subnets = new ArrayList<>();
		this.subnetsMap = new HashMap<>();
		try{
			DropDownChoice<String> vlanfield;
			if(vlanDtos == null || vlanDtos.size() == 0){
				vlanfield = new DropDownChoice<String>("vlanName",
						new PropertyModel<>(this, "vlanName"), vlans);
			}else {
				vlanDtos.forEach(vlanDto -> vlans.add(vlanDto.getVlanId().toString()));
				vlanfield = new DropDownChoice<String>("vlanName",
						new PropertyModel<>(this, "vlanName"), vlans);
			}
			vlanfield.setRequired(true);
			add(vlanfield);

			addAllIpSubnet(SubnetRenderer.getAllIpSubnetNamespace(),subnets,subnetsMap);
			DropDownChoice<String> subnetfield = new DropDownChoice<String>("subnetName",
					new PropertyModel<>(this, "subnetName"), subnets);
			subnetfield.setRequired(true);
			add(subnetfield);
		} catch (ExternalServiceException e) {
			e.printStackTrace();
		}

	}

	public void addAllIpSubnet(List<IpSubnetNamespaceDto> namespaces, List<String> subnets, Map<String,IpSubnetDto> subnetsMap)
	{
		List<IpSubnetDto> allIpSubnetDto = new ArrayList<>();
		allIpSubnetDto.addAll(getAllIpSubnets(namespaces));
		for(IpSubnetDto subnet : allIpSubnetDto){
			if(subnet.getSubnetAddress() != null && subnet.getSubnetAddress().getName() != null) {
				String vpnPrefix = SubnetRenderer.getVpnPrefix(subnet);
				String address = SubnetRenderer.getIpAddress(subnet);
				String mask = SubnetRenderer.getSubnetMask(subnet);
				if(vpnPrefix != null){
					subnets.add(vpnPrefix + "/" + address + "/" + mask);
					subnetsMap.putIfAbsent(vpnPrefix + "/" + address + "/" + mask, subnet);
				} else {
					subnets.add(address + "/" + mask);
					subnetsMap.putIfAbsent(address + "/" + mask, subnet);
				}
			}
		}
	}

	private List<IpSubnetDto> getAllIpSubnets(List<IpSubnetNamespaceDto> namespaces){
		List<IpSubnetDto> allIpSubnetDto = new ArrayList<>();
		namespaces.forEach(namespaceDto -> allIpSubnetDto.addAll(namespaceDto.getUsers()));
		if(namespaces.size() > 1){
			for(IpSubnetNamespaceDto name : namespaces){
				if(name.getChildren() != null && name.getChildren().size() >0){
					List<IpSubnetNamespaceDto> names = new ArrayList<>();
					names.addAll(name.getChildren());
					getAllIpSubnets(names);
				}
			}
		}
		return allIpSubnetDto;
	}

	public String getVlanName() {
		return this.vlanName;
	}

	public void setVlanName(String name) {
		this.vlanName = name;
	}

	public String getSubnetName() {
		return this.subnetName;
	}

	public IpSubnetDto getSubnet(String subnetName) {
		return subnetsMap.get(this.subnetName);
	}
}
