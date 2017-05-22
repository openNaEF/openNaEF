package voss.multilayernms.inventory.nmscore.inventory.accessor;

import naef.dto.CustomerInfoDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIdPoolDto;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.Vlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import tef.skelton.dto.EntityDto;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.model.creator.VlanModelCreator;
import voss.multilayernms.inventory.nmscore.rendering.VlanRenderingUtil;
import voss.multilayernms.inventory.renderer.CustomerInfoRenderer;
import voss.multilayernms.inventory.renderer.VlanRenderer;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VlanHandler {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(VlanHandler.class);

    public static List<Vlan> getList(ObjectFilterQuery query) throws NotBoundException,
            AuthenticationException, IOException, InventoryException, ExternalServiceException {
        List<Vlan> vlanList = new ArrayList<Vlan>();
        for (VlanIdPoolDto vlanPoolDto : VlanRenderer.getVlanIdPools()) {
            if (vlanPoolDto != null) {
                Set<VlanDto> vlans = vlanPoolDto.getUsers();
                for (VlanDto vlan : filterVlans(query, vlans)) {
                    vlanList.add(VlanModelCreator.createModel(vlan));
                }
            }
        }
        return vlanList;
    }

    public static List<VlanDto> filterVlans(ObjectFilterQuery query, Set<VlanDto> vlans) {
        List<VlanDto> result = new ArrayList<VlanDto>();

        for (VlanDto vlan : vlans) {
            boolean unmatched = false;
            for (String field : query.keySet()) {
                String value = VlanRenderingUtil.rendering(vlan, field);

                if (!query.get(field).matches(value)) {
                    unmatched = true;
                    break;
                }
            }

            if (!unmatched) {
                result.add(vlan);
            }
        }
        return result;
    }

    public static List<Vlan> getList(String mvoid) throws IOException, ExternalServiceException {
        EntityDto dto = MplsNmsInventoryConnector.getInstance().getMvoDtoByMvoId(mvoid);
        if (dto instanceof CustomerInfoDto) {
            return getList((CustomerInfoDto) dto);
        } else {
            return new ArrayList<Vlan>();
        }
    }

    private static List<Vlan> getList(CustomerInfoDto dto) throws IOException {
        List<Vlan> list = new ArrayList<Vlan>();
        for (VlanDto vlan : CustomerInfoRenderer.getMemberVlans(dto)) {
            list.add(VlanModelCreator.createModel(vlan));
        }
        return list;
    }
}