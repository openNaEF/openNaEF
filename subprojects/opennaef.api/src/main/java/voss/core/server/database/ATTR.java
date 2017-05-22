package voss.core.server.database;

import naef.dto.CustomerInfoDto;
import naef.dto.SystemUserDto;
import naef.mvo.mpls.RsvpLspHopSeries;

public class ATTR {
    public static final String NAME_DELIMITER_PRIMARY = ",";
    public static final String NAME_DELIMITER_SECONDARY = ";";

    public static final String IFNAME = naef.mvo.AbstractPort.Attr.IFNAME.getName();

    public static final String IFINDEX = naef.mvo.AbstractPort.Attr.IFINDEX.getName();

    public static final String POOL_TYPE_VRF_STRING_TYPE = "vrf.id-pool.string-type";
    public static final String POOL_TYPE_VRF_INTEGER_TYPE = "vrf.id-pool.integer-type";

    public static final String POOL_TYPE_VPLS_STRING_TYPE = "vpls.id-pool.string-type";
    public static final String POOL_TYPE_VPLS_INTEGER_TYPE = "vpls.id-pool.integer-type";

    public static final String POOL_TYPE_PSEUDOWIRE_STRING_TYPE = "pseudowire.id-pool.string-type";
    public static final String POOL_TYPE_PSEUDOWIRE_LONG_TYPE = "pseudowire.id-pool.long-type";

    public static final String POOL_TYPE_VLAN_DOT1Q = "vlan.id-pool.dot1q";
    public static final String POOL_TYPE_IPSUBNET = "ip.subnet-namespace";
    public static final String POOL_TYPE_RSVP_LSP = "rsvp-lsp.id-pool";
    public static final String POOL_TYPE_RSVP_LSP_HOPS = "rsvp-lsp-hop-series.id-pool";
    public static final String POOL_TYPE_IPSUBNET_ADDRESS = "ip-subnet-address";

    public static final String NETWORK_TYPE_IPSUBNET = "ip-subnet";
    public static final String NETWORK_TYPE_IPSUBNET_ADDRESS = "ip-subnet-address";
    public static final String NETWORK_TYPE_RSVPLSP = "rsvp-lsp";
    public static final String NETWORK_TYPE_RSVPLSP_HOP_SERIES = "rsvp-lsp-hop-series";
    public static final String NETWORK_TYPE_PSEUDOWIRE = "pseudowire";
    public static final String NETWORK_TYPE_VLAN = "vlan";
    public static final String NETWORK_TYPE_VPLS = "vpls";
    public static final String NETWORK_TYPE_VRF = "vrf";
    public static final String NETWORK_TYPE_ID = "id";

    public static final String TYPE_ID = "id";

    public static final String TYPE_LOCATION = "location";

    public static final String TYPE_VLAN_IF = "vlan-if";
    public static final String TYPE_VLAN_SEGMENT_GATEWAY_IF = "vlan-segment-gateway-if";
    public static final String TYPE_VPLS_IF = "vpls-if";
    public static final String TYPE_VRF_IF = "vrf-if";
    public static final String TYPE_ATM_PVP_IF = "atm-pvp-if";
    public static final String TYPE_ATM_PVC_IF = "atm-pvc-if";
    public static final String TYPE_FR_PVC_IF = "fr-pvc-if";
    public static final String TYPE_ISDN_CHANNEL_IF = "isdn-channel-port";
    public static final String TYPE_ATM_APS = "atm-aps-if";

    public static final String SUFFIX = "suffix";

    public static final String TYPE_MVO_ID = "mvo-id";

    public static final String TYPE_NODE = "node";
    public static final String TYPE_CHASSIS = "chassis";
    public static final String TYPE_SLOT = "slot";
    public static final String TYPE_MODULE = "module";
    public static final String TYPE_JACK = "jack";

    public static final String TYPE_ETH_PORT = "eth-port";
    public static final String TYPE_ATM_PORT = "atm-port";
    public static final String TYPE_POS_PORT = "pos-port";
    public static final String TYPE_SERIAL_PORT = "serial-port";

    public static final String TYPE_TDM_SERIAL_PORT = "tdm-serial-if";
    public static final String TYPE_ISDN_PORT = "isdn-port";
    public static final String TYPE_WDM_PORT = "wdm-port";

    public static final String TYPE_IP_PORT = "ip-if";
    public static final String TYPE_INTERCONNECTION_IF = "i13n-if";
    public static final String TYPE_LAG_PORT = "eth-lag-if";
    public static final String TYPE_ATM_APS_PORT = "atm-aps-if";
    public static final String TYPE_POS_APS_PORT = "pos-aps-if";

    public static final String TYPE_L1_LINK = "l1-link";
    public static final String TYPE_L2_LINK = "l2-link";
    public static final String TYPE_IP_LINK = "ip-link";
    public static final String TYPE_ETH_LINK = "eth-link";
    public static final String TYPE_LAG_LINK = "eth-lag";
    public static final String TYPE_VLAN_LINK = "vlan-link";
    public static final String TYPE_ATM_LINK = "atm-link";
    public static final String TYPE_POS_LINK = "pos-link";
    public static final String TYPE_ISDN_LINK = "isdn-link";
    public static final String TYPE_SERIAL_LINK = "serial-link";
    public static final String TYPE_ATM_APS_LINK = "atm-aps-link";
    public static final String TYPE_WDM_LINK = "wdm-link";

    public static final String TYPE_IFNAME = "if-name";
    public static final String TYPE_IFINDEX = "if-index";

    public static final String TYPE_SERVICE_MENU_ITEM = "service-menu-item";
    public static final String TYPE_CUSTOMER_INFO = "customer-info";

    public static final String FEATURE_PSEUDOWIRE = "naef.enabled-networking-function.pseudowire";
    public static final String FEATURE_RSVPLSP = "naef.enabled-networking-function.rsvp-lsp";
    public static final String FEATURE_VPLS = "naef.enabled-networking-function.vpls";
    public static final String FEATURE_VRF = "naef.enabled-networking-function.vrf";

    public static final String FEATURE_VLAN = "naef.enabled-networking-function.vlan";
    public static final String FEATURE_VLAN_DOT1Q = "dot1q";
    public static final String ATTR_VLAN_IF_ID = "naef.vlan.vlan-if.vlan-id";

    public static final String FEATURE_FR_PVC = "naef.enabled-networking-function.fr.pvc";
    public static final String ATTR_FR_ENCAPSULATION = FEATURE_FR_PVC;
    public static final String FR_ENCAPSULATION_ANSI = "ANSI";
    public static final String ATTR_FR_DLCI = "naef.fr.fr-if.dlci";

    public static final String FEATURE_ATM_PVC = "naef.enabled-networking-function.atm";
    public static final String ATTR_ATM_PVP_VPI = "naef.atm.pvp-if.vpi";
    public static final String ATTR_ATM_PVC_VCI = "naef.atm.pvc-if.vci";

    public static final String TAGCHANGER_ID = naef.mvo.vlan.VlanSegmentGatewayIf.Attr.ID.getName();

    public static final String TAGCHANGER_INNER_VLAN_ID = naef.mvo.vlan.VlanSegmentGatewayIf.Attr.VLAN_IF.getName();

    public static final String TAGCHANGER_OUTER_VLAN_ID = "OuterVlanID";

    public static final String TAGCHANGER_BOUND_VLAN_LINK = naef.mvo.vlan.VlanSegmentGatewayIf.Attr.VLAN_SEGMENT.getName();

    public static final String ATTR_IPSUBNET_ID = "naef.ip.subnet-name";

    public static final String ATTR_IPSUBNET_POOL = "naef.ip.subnet-namespace";

    public static final String ATTR_IPSUBNET_SUBNET_ADDRESS = naef.mvo.ip.IpSubnet.Attr.SUBNET_ADDRESS.getName();

    public static final String ATTR_PW_ID_LONG = "naef.pseudowire.id.long-type";

    public static final String ATTR_PW_ID_STRING = "naef.pseudowire.id.string-type";

    public static final String ATTR_PW_POOL_LONG = "naef.pseudowire.id-pool.long-type";

    public static final String ATTR_PW_POOL_STRING = "naef.pseudowire.id-pool.string-type";

    public static final String ATTR_VLAN_ID = naef.mvo.vlan.Vlan.Attr.ID.getName();

    public static final String ATTR_VLAN_POOL = naef.mvo.vlan.Vlan.Attr.ID_POOL.getName();

    public static final String ATTR_VPLS_ID_INTEGER = "naef.vpls.id.integer-type";

    public static final String ATTR_VPLS_ID_STRING = "naef.vpls.id.string-type";

    public static final String ATTR_VPLS_POOL_INTEGER = "naef.vpls.id-pool.integer-type";

    public static final String ATTR_VPLS_POOL_STRING = "naef.vpls.id-pool.string-type";

    public static final String ATTR_VRF_ID_INTEGER = "naef.vrf.id.integer-type";

    public static final String ATTR_VRF_ID_STRING = "naef.vrf.id.string-type";

    public static final String ATTR_VRF_POOL_INTEGER = "naef.vrf.id-pool.integer-type";

    public static final String ATTR_VRF_POOL_STRING = "naef.vrf.id-pool.string-type";

    public static final String ATTR_RSVPLSP_ID = "naef.mpls.rsvp-lsp.id";
    public static final String ATTR_RSVPLSP_POOL = "naef.mpls.rsvp-lsp.id-pool";

    public static final String ATTR_PATH_ID = "naef.rsvp-lsp-hop-series.id";
    public static final String ATTR_PATH_POOL = "naef.rsvp-lsp-hop-series.id-pool";

    public static final String ATTR_PATH_INGRESS = RsvpLspHopSeries.Attr.INGRESS_NODE.getName();

    public static final String ATTR_VPLS_ID = "VPLS ID";
    public static final String ATTR_VRF_ID = "VRF ID";

    public static final String ATTR_PRIMARY_IP = "naef.port.primary-ipif";
    public static final String ATTR_SECONDARY_IP = "naef.port.secondary-ipif";

    public static final String ATTR_IPIF_SUBNET_ADDRESS = naef.mvo.ip.IpIf.Attr.IP_SUBNET_ADDRESS.getName();

    public static final String ATTR_IPIF_IP_ADDRESS = naef.mvo.ip.IpIf.Attr.IP_ADDRESS.getName();

    public static final String ATTR_IPIF_SUBNETMASK_LENGTH = naef.mvo.ip.IpIf.Attr.SUBNET_MASK_LENGTH.getName();

    public static final String ATTR_IPSUBNETNAMESPACE_ADDRESS = naef.mvo.ip.IpSubnetNamespace.Attr.IP_SUBNET_ADDRESS.getName();

    public static final String ATTR_IPSUBNETADDRESS_SUBNETNAMESPACE = naef.mvo.ip.IpSubnetAddress.Attr.IP_SUBNET_NAMESPACE.getName();

    public static final String ATTR_IPSUBNETADDRESS_SUBNET = naef.mvo.ip.IpSubnetAddress.Attr.IP_SUBNET.getName();

    public static final String ATTR_NAEF_PORT_MODE = naef.mvo.AbstractPort.Attr.PORT_MODE.getName();

    public static final String ATTR_NAEF_PORT_MODE_VLAN = naef.mvo.PortMode.VLAN.name();
    public static final String ATTR_NAEF_PORT_MODE_IP = naef.mvo.PortMode.IP.name();

    public static final String ATTR_NAEF_SWITCHPORT_MODE = naef.mvo.vlan.VlanAttrs.SWITCH_PORT_MODE.getName();

    public static final String ATTR_NAEF_SWITCHPORT_MODE_TRUNK = naef.mvo.vlan.SwitchPortMode.TRUNK.name();

    public static final String ATTR_NAEF_SWITCHPORT_MODE_ACCESS = naef.mvo.vlan.SwitchPortMode.ACCESS.name();

    public static final String ATTR_NAEF_SWITCHPORT_MODE_DOT1Q_TUNNEL = naef.mvo.vlan.SwitchPortMode.DOT1Q_TUNNEL.name();

    public static final String ATTR_VIRTUALIZATION_HOSTED_TYPE = naef.mvo.Node.Attr.VIRTUALIZATION_HOSTED_TYPE.getName();

    public static final String ATTR_VIRTUALIZATION_HOST_NODES = naef.mvo.Node.Attr.VIRTUALIZATION_HOST_NODES.getName();

    public static final String ATTR_VIRTUALIZATION_GUEST_NODES = naef.mvo.Node.Attr.VIRTUALIZATION_GUEST_NODES.getName();

    public static final String ATTR_VIRTUALIZATION_HOSTING_ENABLED = naef.mvo.Node.Attr.VIRTUALIZED_HOSTING_ENABLED.getName();

    public static final String ATTR_ALIAS_SOURCEABLE = naef.mvo.AbstractPort.Attr.ALIAS_SOURCEABLE.getName();

    public static final String ATTR_ALIASES = naef.mvo.AbstractPort.Attr.ALIASES.getName();

    public static final String ATTR_ALIAS_SOURCE = naef.mvo.AbstractPort.Attr.ALIAS_SOURCE.getName();

    public static final String ATTR_ALIAS_ROOT_SOURCE = naef.dto.PortDto.ExtAttr.ALIAS_ROOT_SOURCE.getName();

    public static final String MEMBER_PORT_MAX = "naef.network.max-configurable-ports";

    public static final String LSP_PRIMARY_PATH = "naef.mpls.rsvp-lsp.hop-series-1";
    public static final String LSP_BACKUP_PATH = "naef.mpls.rsvp-lsp.hop-series-2";

    public static final String PW_AC1 = "ac1";
    public static final String PW_AC2 = "ac2";

    public static final String TASK_ENABLED = "naef.enabled";

    public static final String TASK_ENABLED_TIME = "naef.enabled-time";

    public static final String TASK_DISABLED_TIME = "naef.disabled-time";

    public static final String LINK_MAX_PORTS = "naef.network.max-configurable-ports";

    public static final String DELETE_FLAG = "削除フラグ";

    public static final String NODEGROUP_MEMBER = naef.dto.NodeGroupDto.ExtAttr.MEMBERS.getName();

    public static final String NODE_GROUPS = naef.dto.NodeDto.ExtAttr.NODE_GROUPS.getName();

    public static final String CUSTOMER_INFO_REFERENCES = naef.mvo.CustomerInfo.Attr.REFERENCES.getName();

    public static final String CUSTOMER_INFO_ON_MODEL = naef.mvo.NaefAttributes.CUSTOMER_INFOS.getName();

    public static final String TYPE_SYSTEM_USER = "system-user";

    public static final String CONFIG_NAME = "ConfigName";

    public static final String VPN_PREFIX = "vpn-prefix";

    public static final String VPN_DELIMITER = "/";

    public static final String IP_ADDRESS_PREFIX = "IP:";

    public static final String PORT_TYPE = "ポート種別";

    public static final String DIFF_TARGET = "DiffTarget";

    public static final String IMPLICIT = "implicit";

    public static final String DELETED = "$$DELETED$$";

    public static final String IP_ADDRESS = "ip-address";

    public static final String MASK_LENGTH = "mask-length";

    public static final String VISIBLE_FLAG = "表示フラグ";

    public static final String LSP_NAME = "LSP名";

    public static final String PATH_NAME = "パス名";

    public static final String LAST_EDITOR = "_LastEditor";

    public static final String LAST_EDIT_TIME = "_LastEditTime";

    public static final String ENABLED = "enabled";

    public static final String DISABLED = "disabled";

    public static final String ADMIN_STATUS_ENABLE = "有効";

    public static final String ADMIN_STATUS_DISABLE = "無効";

    public static final String UP = "UP";

    public static final String DOWN = "down";

    public static final String KEY_FLAGS_COLUMN = "参考リンク";

    public static final String NOTE = "備考";

    public static final String PURPOSE = "用途";

    public static final String SOURCE = "情報ソース";

    public static final String CUSTOMER_INFO_ID = "ID";

    public static final String CUSTOMER_INFO_ID_TYPE = "ID-TYPE";

    public static final String ATTR_CUSTOMER_INFO_REF_TO_SYSTEM_USER = CustomerInfoDto.ExtAttr.SYSTEM_USER.getName();

    public static final String ATTR_SYSTEM_USER_REF_TO_CUSTOMER_INFO = SystemUserDto.ExtAttr.CUSTOMER_INFOS.getName();

    public static final String LINK_TYPE = "リンク種別";

    public static final String POOL_TYPE_OF_PATCH_LINK_PATCH_ID = "of-patch-link.patch-id-pool";

    public static final String TYPE_OF_PATCH_LINK = "of-patch-link";
    public static final String ATTR_OF_PATCH_LINK_PATCH_ID = "naef.of-patch-link.id";

    public static final String ATTR_OF_PATCH_LINK_PATCH_ID_POOL = "naef.patch-id-pool";
}