package voss.multilayernms.inventory.nmscore.inventory.accessor;

import naef.dto.vrf.VrfIfDto;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.Vrf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.builder.GenericAttributeCommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.nmscore.inventory.InventoryIdUtil;
import voss.multilayernms.inventory.nmscore.model.creator.VrfModelCreator;
import voss.multilayernms.inventory.nmscore.rendering.VrfRenderingUtil;
import voss.multilayernms.inventory.renderer.VrfRenderer;
import voss.multilayernms.inventory.util.VrfExtUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.VrfUtil;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VrfHandler {
    private final static Logger log = LoggerFactory.getLogger(VrfHandler.class);

    public static List<Vrf> get(String inventoryId) throws NotBoundException, AuthenticationException, IOException, ExternalServiceException {
        List<Vrf> vrfList = new ArrayList<Vrf>();

        for (VrfIfDto vrf : VrfExtUtil.getVrfIfDtos()) {
            if (InventoryIdUtil.getInventoryId(vrf).equals(inventoryId)) {
                vrfList.add(VrfModelCreator.createModel(vrf, InventoryIdUtil.getInventoryId(vrf)));
            }
        }

        return vrfList;
    }

    public static VrfIfDto getVrfIfDto(String inventoryId) throws IOException, ExternalServiceException {
        for (VrfIfDto vrf : VrfExtUtil.getVrfIfDtos()) {
            if (InventoryIdUtil.getInventoryId(vrf).equals(inventoryId)) {
                return vrf;
            }
        }
        return null;
    }

    public static List<Vrf> getListOnNode(String inventoryId) throws IOException, InventoryException, ExternalServiceException, ParseException {
        List<Vrf> vrfList = new ArrayList<Vrf>();
        appendList(vrfList, voss.multilayernms.inventory.web.vrf.VrfWebUtil.getVrfIfs(NodeHandler.getNodeDto(inventoryId)));
        return vrfList;
    }

    public static List<Vrf> getListOnPort(String inventoryId) throws RemoteException, IOException, ExternalServiceException {
        List<Vrf> vrfList = new ArrayList<Vrf>();
        appendList(vrfList, VrfUtil.getVplsIfsOn(PortHandler.getPortDto(inventoryId)));
        return vrfList;
    }

    private static void appendList(List<Vrf> vrfList, List<VrfIfDto> vrfIfs) throws IOException {
        for (VrfIfDto vrf : vrfIfs) {
            vrfList.add(VrfModelCreator.createModel(vrf, InventoryIdUtil.getInventoryId(vrf)));
        }
    }


    public static List<Vrf> getList(ObjectFilterQuery query) throws NotBoundException, AuthenticationException, IOException, ExternalServiceException {
        List<Vrf> vrfList = new ArrayList<Vrf>();

        for (VrfIfDto vrf : filterVrfs(query, VrfExtUtil.getVrfIfDtos())) {
            vrfList.add(VrfModelCreator.createModel(vrf, InventoryIdUtil.getInventoryId(vrf)));
        }

        return vrfList;
    }

    private static List<VrfIfDto> filterVrfs(ObjectFilterQuery query, Collection<VrfIfDto> vrfs) {
        List<VrfIfDto> result = new ArrayList<VrfIfDto>();

        for (VrfIfDto vrf : vrfs) {
            boolean unmatched = false;
            for (String field : query.keySet()) {
                String value = VrfRenderingUtil.rendering(vrf, field);

                if (!query.get(field).matches(value)) {
                    unmatched = true;
                    break;
                }
            }

            if (!unmatched) {
                result.add(vrf);
            }
        }
        return result;
    }

    public static void executeSetFacilityStatus(VrfIfDto vrf,
                                                FacilityStatus fs, String userName) throws IOException, InventoryException, ExternalServiceException {
        String fsValue = (fs == null ? null : fs.getDisplayString());
        GenericAttributeCommandBuilder builder = new GenericAttributeCommandBuilder(vrf, userName);
        builder.setAttribute(MPLSNMS_ATTR.FACILITY_STATUS, fsValue);
        builder.buildCommand();
        ShellConnector.getInstance().execute(builder);
        log.debug("update completed: [" + VrfRenderer.getVpnId(vrf) + "/" + fs + "]");
    }

}