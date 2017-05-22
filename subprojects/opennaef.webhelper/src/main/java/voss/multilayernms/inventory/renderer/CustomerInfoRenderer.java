package voss.multilayernms.inventory.renderer;

import naef.dto.CustomerInfoDto;
import naef.dto.NaefDto;
import naef.dto.NodeDto;
import naef.dto.SystemUserDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIfDto;
import naef.ui.NaefDtoFacade;
import naef.ui.NaefDtoFacade.SearchMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.constants.CustomerConstants;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;

import java.io.IOException;
import java.util.*;

public class CustomerInfoRenderer extends GenericRenderer {

    static Logger log = LoggerFactory.getLogger(CustomerInfoRenderer.class);

    private final CustomerInfoDto user;
    public CustomerInfoRenderer(CustomerInfoDto dto) {
        this.user = dto;
    }
    public String getName() {
        return getCustomerInfoId(this.user);
    }
    public static String getCustomerInfoId(CustomerInfoDto customerInfo) {
        return DtoUtil.getString(customerInfo, "ID", "");
    }

    public static String getCustomerName(CustomerInfoDto dto) {
        return getCustomerInfoId(dto);
    }

    public String getCompanyID() {
        return getCompanyID(this.user);
    }

    public static String getCompanyID(CustomerInfoDto customerInfo) {
        return DtoUtil.getString(customerInfo, "企業ID", "");
    }

    public boolean isActive() {
        return isActive(this.user);
    }

    public static boolean isActive(CustomerInfoDto dto) {
        return DtoUtil.getBoolean(dto, "active");
    }

    public String getStatus() {
        return getStatus(this.user);
    }

    public static String getStatus(CustomerInfoDto dto) {
        boolean b = isActive(dto);
        if (b) {
            return "O";
        } else {
            return "x";
        }
    }

    public static List<VlanDto> getMemberVlans(CustomerInfoDto dto) {
        List<VlanDto> list = new ArrayList<VlanDto>();
        for (NaefDto ref : getReferences(dto)) {
            if (ref instanceof VlanDto) {
                list.add((VlanDto) ref);
            }
        }
        return list;
    }

    public static String getMemberVlansString(CustomerInfoDto dto) {
        StringBuilder sb = new StringBuilder();
        for (VlanDto ref : getMemberVlans(dto)) {
            sb.append(VlanRenderer.getVlanIdPoolName(ref));
            sb.append(":");
            sb.append(VlanRenderer.getVlanId(ref));
            sb.append(" ");
        }
        return sb.toString();
    }

    public static List<IpSubnetDto> getMemberIpSubnets(CustomerInfoDto dto) {
        List<IpSubnetDto> list = new ArrayList<IpSubnetDto>();
        for (NaefDto ref : getReferences(dto)) {
            if (ref instanceof IpSubnetDto) {
                list.add((IpSubnetDto) ref);
            }
        }
        return list;
    }

    public static String getMemberIpSubnetsString(CustomerInfoDto dto) {
        StringBuilder sb = new StringBuilder();
        for (IpSubnetDto ref : getMemberIpSubnets(dto)) {
            IpSubnetDto ipSubnet = (IpSubnetDto) ref;
            sb.append(SubnetRenderer.getSubnetName(ipSubnet));
            sb.append(":");
            sb.append(SubnetRenderer.getIpAddress(ipSubnet));
            sb.append("/");
            sb.append(SubnetRenderer.getSubnetMask(ipSubnet));
            sb.append(" ");
        }
        return sb.toString();
    }

    private static Collection<NaefDto> getReferences(CustomerInfoDto dto) {
        Collection<NaefDto> refs = dto.getReferences();
        if (refs == null) return new ArrayList<NaefDto>();
        return dto.getReferences();
    }

    public static Set<CustomerInfoDto> getCustomerInfoExactMatchByName(String customerInfoName) {
        Set<CustomerInfoDto> css = null;
        try {
            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            NaefDtoFacade facade;
            facade = conn.getDtoFacade();
            css = facade.selectCustomerInfos(SearchMethod.EXACT_MATCH, ATTR.CUSTOMER_INFO_ID, customerInfoName);
        } catch (IOException e) {
            log.debug("IOException", e);
        } catch (ExternalServiceException e) {
            log.debug("ExternalServiceException", e);
        } catch (AuthenticationException e) {
            log.debug("AuthenticationException", e);
        }
        return css;
    }

    public static ArrayList<NodeDto> getNodeDtoByCustomerInfoDto(CustomerInfoDto customer) {
        ArrayList<NodeDto> nodes = new ArrayList<NodeDto>();
        Collection<NaefDto> dtos = getReferences(customer);
        if (dtos.size() > 0) {
            for (NaefDto dto : dtos) {
                if (dto instanceof VlanDto) {
                    VlanDto vlan = VlanDto.class.cast(dto);
                    if (vlan.getMemberVlanifs().size() > 0) {
                        for (VlanIfDto vif : vlan.getMemberVlanifs()) {
                            if (!nodes.contains(vif.getNode())) {
                                nodes.add(vif.getNode());
                            }
                        }
                    }
                }
            }
        }
        return nodes;
    }

    public static String getNodesStringByCustomerInfoDto(CustomerInfoDto customer) {
        Collection<NaefDto> dtos = getReferences(customer);
        ArrayList<String> nodes = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        if (dtos.size() > 0) {
            for (NaefDto dto : dtos) {
                if (dto instanceof NodeDto) {
                    nodes.add(((NodeDto) dto).getName());
                }
            }
        }
        Collections.sort(nodes);
        if (nodes != null) {
            if (nodes.size() > 0) {
                for (String node : nodes) {
                    if (node != null) {
                        sb.append(node);
                        sb.append(" ");
                    }
                }
            }
        }
        return sb.toString();
    }

    public static List<String> getFWPoliciesZoneMatrix(CustomerInfoDto customerInfo) {
        if (customerInfo == null) {
            return new ArrayList<String>();
        }
        return DtoUtil.getStringList(customerInfo, CustomerConstants.FW_POLICY_ZONE_MATRIX);
    }

    public String getPortalUser() {
        return getFMPortalUser(this.user);
    }
    public static String getFMPortalUser(CustomerInfoDto dto) {
        return DtoUtil.getString(dto, "FMPortalUser") != null ? DtoUtil.getString(dto, "FMPortalUser") : "";
    }

    public String getPortalPass() {
        return getFMPortalPass(this.user);
    }
    public static String getFMPortalPass(CustomerInfoDto dto) {
        return DtoUtil.getString(dto, "FMPortalPass") != null ? DtoUtil.getString(dto, "FMPortalPass") : "";
    }

    public static Long getMaxSsl(CustomerInfoDto dto) {
        return DtoUtil.getLong(dto, CustomerConstants.MAX_SSL) == null ? 0 : DtoUtil.getLong(dto, CustomerConstants.MAX_SSL);
    }

    public static Long getMaxMember(CustomerInfoDto dto) {
        return DtoUtil.getLong(dto, CustomerConstants.MAX_MEMBER) == null ? 0 : DtoUtil.getLong(dto, CustomerConstants.MAX_MEMBER);
    }

    public static String getSystemUserName(CustomerInfoDto dto) {
        SystemUserDto su = dto.getSystemUser();
        if (su == null) return "";
        return su.getName();
    }
}