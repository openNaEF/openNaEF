package voss.multilayernms.inventory.nmscore.inventory.accessor;

import naef.dto.vpls.VplsIfDto;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.Vpls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.builder.GenericAttributeCommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.nmscore.inventory.InventoryIdUtil;
import voss.multilayernms.inventory.nmscore.model.creator.VplsModelCreator;
import voss.multilayernms.inventory.nmscore.rendering.VplsRenderingUtil;
import voss.multilayernms.inventory.renderer.VplsRenderer;
import voss.multilayernms.inventory.util.VplsExtUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.VplsUtil;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VplsHandler {
    private final static Logger log = LoggerFactory.getLogger(VplsHandler.class);

    public static List<Vpls> get(String inventoryId) throws NotBoundException, AuthenticationException, IOException, ExternalServiceException {
        List<Vpls> vplsList = new ArrayList<Vpls>();

        for (VplsIfDto vpls : VplsExtUtil.getVplsIfDtos()) {
            if (InventoryIdUtil.getInventoryId(vpls).equals(inventoryId)) {
                vplsList.add(VplsModelCreator.createModel(vpls, InventoryIdUtil.getInventoryId(vpls)));
            }
        }

        return vplsList;
    }

    public static VplsIfDto getVplsIfDto(String inventoryId) throws IOException, ExternalServiceException {
        for (VplsIfDto vpls : VplsExtUtil.getVplsIfDtos()) {
            if (InventoryIdUtil.getInventoryId(vpls).equals(inventoryId)) {
                return vpls;
            }
        }
        return null;
    }

    public static List<Vpls> getListOnNode(String inventoryId) throws IOException, InventoryException, ExternalServiceException, ParseException {
        List<Vpls> vplsList = new ArrayList<Vpls>();
        appendList(vplsList, voss.multilayernms.inventory.web.vpls.VplsWebUtil.getVplsIfs(NodeHandler.getNodeDto(inventoryId)));
        return vplsList;
    }

    public static List<Vpls> getListOnPort(String inventoryId) throws RemoteException, IOException, ExternalServiceException {
        List<Vpls> vplsList = new ArrayList<Vpls>();
        appendList(vplsList, VplsUtil.getVplsIfsOn(PortHandler.getPortDto(inventoryId)));
        return vplsList;
    }

    private static void appendList(List<Vpls> vplsList, List<VplsIfDto> vplsIfs) throws IOException {
        for (VplsIfDto vpls : vplsIfs) {
            vplsList.add(VplsModelCreator.createModel(vpls, InventoryIdUtil.getInventoryId(vpls)));
        }
    }

    public static List<Vpls> getList(ObjectFilterQuery query) throws NotBoundException, AuthenticationException, IOException, ExternalServiceException {
        List<Vpls> vplsList = new ArrayList<Vpls>();

        for (VplsIfDto vpls : filterVplses(query, VplsExtUtil.getVplsIfDtos())) {
            vplsList.add(VplsModelCreator.createModel(vpls, InventoryIdUtil.getInventoryId(vpls)));
        }

        return vplsList;
    }

    private static List<VplsIfDto> filterVplses(ObjectFilterQuery query, Collection<VplsIfDto> vplses) {
        List<VplsIfDto> result = new ArrayList<VplsIfDto>();

        for (VplsIfDto vpls : vplses) {
            boolean unmatched = false;
            for (String field : query.keySet()) {
                String value = VplsRenderingUtil.rendering(vpls, field);

                if (!query.get(field).matches(value)) {
                    unmatched = true;
                    break;
                }
            }

            if (!unmatched) {
                result.add(vpls);
            }
        }
        return result;
    }

    public static void executeSetFacilityStatus(VplsIfDto vpls,
                                                FacilityStatus fs, String userName) throws IOException, InventoryException, ExternalServiceException {
        String fsValue = (fs == null ? null : fs.getDisplayString());
        GenericAttributeCommandBuilder builder = new GenericAttributeCommandBuilder(vpls, userName);
        builder.setAttribute(MPLSNMS_ATTR.FACILITY_STATUS, fsValue);
        builder.buildCommand();
        ShellConnector.getInstance().execute(builder);
        log.debug("update completed: [" + VplsRenderer.getVplsId(vpls) + "/" + fs + "]");
    }

}