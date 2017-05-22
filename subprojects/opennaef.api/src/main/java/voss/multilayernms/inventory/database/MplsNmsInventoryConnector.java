package voss.multilayernms.inventory.database;

import naef.dto.LocationDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import naef.dto.mpls.PseudowireLongIdPoolDto;
import naef.dto.mpls.RsvpLspHopSeriesIdPoolDto;
import naef.dto.mpls.RsvpLspIdPoolDto;
import naef.dto.vpls.VplsIntegerIdPoolDto;
import naef.dto.vpls.VplsStringIdPoolDto;
import naef.dto.vrf.VrfIntegerIdPoolDto;
import naef.dto.vrf.VrfStringIdPoolDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.LocationUtil;
import voss.multilayernms.inventory.MplsNmsLogCategory;
import voss.multilayernms.inventory.constants.MplsNmsPoolConstants;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MplsNmsInventoryConnector extends InventoryConnector {
    private static final Logger log = LoggerFactory.getLogger(MplsNmsLogCategory.LOG_DEBUG);
    private static MplsNmsInventoryConnector instance = null;

    public synchronized static MplsNmsInventoryConnector getInstance() throws IOException {
        if (instance == null) {
            instance = new MplsNmsInventoryConnector();
        }
        return instance;
    }

    public synchronized static void renew() {
        log.info("renew requested.");
        instance = null;
    }

    protected MplsNmsInventoryConnector() throws IOException {
    }

    public PortDto getPortByInventoryID(String target) throws IOException, ExternalServiceException {
        for (NodeDto node : getActiveNodes()) {
            for (PortDto port : node.getPorts()) {
                String id = InventoryIdCalculator.getId(port);
                if (id != null && id.equals(target)) {
                    return port;
                }
            }
        }
        return null;
    }

    public Set<LocationDto> getActiveLocationDtos() throws ExternalServiceException {
        try {
            Set<LocationDto> result = new HashSet<LocationDto>();
            for (LocationDto location : getDtoFacade().getLocations()) {
                if (!LocationUtil.isAlive(location)) {
                    continue;
                } else if (location.getName().contains(ATTR.DELETED)) {
                    continue;
                } else if (LocationUtil.getLocationType(location) == null) {
                    continue;
                }
                result.add(location);
            }
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public List<IpSubnetDto> getActiveIpSubnets() throws ExternalServiceException {
        try {
            List<IpSubnetDto> result = new ArrayList<IpSubnetDto>();
            IpSubnetNamespaceDto subnetRoot = getActiveRootIpSubnetNamespace(MplsNmsPoolConstants.DEFAULT_IPSUBNET_POOL);
            for (IpSubnetDto subnet : subnetRoot.getUsers()) {
                if (subnet == null) {
                    continue;
                } else if (subnet.getMemberIpifs().size() != 2) {
                    continue;
                }
                result.add(subnet);
            }
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public IpSubnetNamespaceDto getIpSubnetRootPool() throws ExternalServiceException, IOException {
        IpSubnetNamespaceDto subnetRoot = getActiveRootIpSubnetNamespace(MplsNmsPoolConstants.DEFAULT_IPSUBNET_POOL);
        return subnetRoot;
    }

    public RsvpLspHopSeriesIdPoolDto getRegularRsvpLspPathPool() throws ExternalServiceException, IOException {
        return super.getRsvpLspHopSeriesIdPool(MplsNmsPoolConstants.DEFAULT_RSVPLSP_PATH_POOL);
    }

    public RsvpLspHopSeriesIdPoolDto getRegulationRoutePathPool() throws ExternalServiceException, IOException {
        return super.getRsvpLspHopSeriesIdPool(MplsNmsPoolConstants.LSP_PATH_POOL_FOR_REGULATION_ROUTE);
    }

    public RsvpLspIdPoolDto getRegularRsvpLspPool() throws ExternalServiceException, IOException {
        return super.getRsvpLspIdPool(MplsNmsPoolConstants.DEFAULT_RSVPLSP_POOL);
    }

    public RsvpLspIdPoolDto getRegulationRoutePool() throws ExternalServiceException, IOException {
        return super.getRsvpLspIdPool(MplsNmsPoolConstants.LSP_POOL_FOR_REGULATION_ROUTE);
    }

    public PseudowireLongIdPoolDto getPseudoWirePool() throws ExternalServiceException, IOException {
        return super.getPseudoWireLongIdPool(MplsNmsPoolConstants.DEFAULT_PSEUDOWIRE_POOL);
    }

    public VrfIntegerIdPoolDto getVrfIntergerPool() throws ExternalServiceException, IOException {
        return super.getVrfIntegerIpPool("vrf_pool");
    }

    public VrfStringIdPoolDto getVrfStringPool() throws ExternalServiceException, IOException {
        return super.getVrfStringIpPool("vrf_pool");
    }

    public VplsIntegerIdPoolDto getVplsIntergerPool() throws ExternalServiceException, IOException {
        return super.getVplsIntegerIpPool("vpls_pool");
    }

    public VplsStringIdPoolDto getVplsStringPool() throws ExternalServiceException, IOException {
        return super.getVplsStringIpPool("vpls_pool");
    }

    public List<String> getFacilityStatusList() throws ExternalServiceException {
        try {
            List<String> result = getConstants(MPLSNMS_ATTR.FACILITY_STATUS);
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public List<String> getPseudoWireTypeList() throws ExternalServiceException {
        try {
            List<String> result = getConstants(MPLSNMS_ATTR.PSEUDOWIRE_TYPE);
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public RsvpLspHopSeriesIdPoolDto getPathPoolForReservation() throws IOException,
            ExternalServiceException {
        return getRsvpLspHopSeriesIdPool(MplsNmsPoolConstants.LSP_PATH_POOL_FOR_RESERVATION);
    }

    public RsvpLspHopSeriesIdPoolDto getCandidatePathPool() throws IOException,
            ExternalServiceException {
        return getRsvpLspHopSeriesIdPool(MplsNmsPoolConstants.LSP_PATH_POOL_FOR_CANDIDATE);
    }

}