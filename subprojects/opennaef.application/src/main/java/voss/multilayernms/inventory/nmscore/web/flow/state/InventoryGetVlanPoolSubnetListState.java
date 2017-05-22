package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.resistvlansubnet.VlanPoolsAndMasterSubnetList;
import naef.dto.LocationDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import naef.dto.vlan.VlanIdPoolDto;
import voss.core.server.constant.ModelConstant;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.renderer.LocationRenderer;
import voss.multilayernms.inventory.renderer.SubnetRenderer;
import voss.multilayernms.inventory.renderer.VlanRenderer;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;


public class InventoryGetVlanPoolSubnetListState extends UnificUIViewState {

    public InventoryGetVlanPoolSubnetListState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            VlanPoolsAndMasterSubnetList list = new VlanPoolsAndMasterSubnetList();
            List<VlanIdPoolDto> vlanpools = VlanRenderer.getVlanIdPools();
            List<IpSubnetNamespaceDto> ipSubnetNamespaces = SubnetRenderer.getAllIpSubnetNamespace();
            for (VlanIdPoolDto vlanpool : vlanpools) {
                addVlanPools(list, vlanpool);
            }
            for (IpSubnetNamespaceDto ipSubnetNamespace : ipSubnetNamespaces) {
                addIpSubnetNamespace(list, ipSubnetNamespace);
            }
            List<String> locations = new ArrayList<String>();
            for (LocationDto location : MplsNmsInventoryConnector.getInstance().getActiveLocationDtos()) {
                if (!LocationRenderer.isTrash(location)) {
                    locations.add(LocationRenderer.getName(location));
                }
            }
            list.setLocations(locations);
            setXmlObject(list);
            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }


    private void addVlanPools(VlanPoolsAndMasterSubnetList list, VlanIdPoolDto vlanpool) {
        list.addVlanPools(vlanpool.getName(), DtoUtil.getMvoId(vlanpool).toString());
    }

    private void addIpSubnetNamespace(VlanPoolsAndMasterSubnetList list, IpSubnetNamespaceDto ipSubnetNamespace) {
        String name = ipSubnetNamespace.getName();
        if (!name.equals(ModelConstant.IP_SUBNET_NAMESPACE_TRASH_NAME)) {
            list.addSubnetNameSpacelist(name, DtoUtil.getStringOrNull(ipSubnetNamespace, ATTR.VPN_PREFIX), DtoUtil.getMvoId(ipSubnetNamespace).toString(), isRoot(ipSubnetNamespace));
        }
    }

    private boolean isRoot(IpSubnetNamespaceDto ipSubnetNamespace) {
        return ipSubnetNamespace.getParent() == null ? true : false;
    }
}