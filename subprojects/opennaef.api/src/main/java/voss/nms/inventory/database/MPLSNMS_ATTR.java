package voss.nms.inventory.database;

import voss.core.server.database.ATTR;
import voss.mplsnms.MplsnmsAttrs;

public class MPLSNMS_ATTR extends ATTR {

    public static final String ATTR_PORT_MODE = naef.mvo.AbstractPort.Attr.PORT_MODE.getName();

    public static final String ATTR_PORT_MODE_VLAN = naef.mvo.PortMode.VLAN.name();
    public static final String ATTR_PORT_MODE_IP = naef.mvo.PortMode.IP.name();

    public static final String ATTR_SWITCHPORT_MODE = naef.mvo.vlan.VlanAttrs.SWITCH_PORT_MODE.getName();

    public static final String ATTR_SWITCHPORT_MODE_ACCESS = naef.mvo.vlan.SwitchPortMode.ACCESS.name();

    public static final String ATTR_SWITCHPORT_MODE_DOT1Q_TUNNEL = naef.mvo.vlan.SwitchPortMode.DOT1Q_TUNNEL.name();

    public static final String ATTR_SWITCHPORT_MODE_TRUNK = naef.mvo.vlan.SwitchPortMode.TRUNK.name();

    public static final String ADMIN_ACCOUNT = "特権ユーザ名";

    public static final String ADMIN_PASSWORD = "特権パスワード";

    public static final String ADMIN_STATUS = "管理状態";

    public static final String AUTO_NEGO = "AutoNegotiation";

    public static final String BANDWIDTH = "帯域";

    public static final String CAPTION = "表示名";

    public static final String CHANNEL_GROUP = "ChannelGroup";

    public static final String CHASSIS_TYPE = "シャーシ種別";

    public static final String CLI_MODE = "コンソール種別";

    public static final String DESCRIPTION = "description";

    public static final String DUPLEX_ADMIN = "AdministrativeDuplex";

    public static final String DUPLEX_OPER = "OperationalDuplex";

    public static final String END_USER = "利用者";

    public static final String EPS_FLAG = "eps";

    public static final String FACILITY_STATUS = "設備ステータス";

    public static final String IGP_COST = "IGPコスト";

    public static final String LOCATION_TYPE = "種別";

    public static final String LSP_ACTIVE_PATH = "naef.mpls.rsvp-lsp.active-hop-series";

    public static final String LSP_BACKUP_PATH = "naef.mpls.rsvp-lsp.hop-series-2";

    public static final String LSP_PRIMARY_PATH = "naef.mpls.rsvp-lsp.hop-series-1";

    public static final String LSP_TERM = "term";

    public static final String MANAGEMENT_IP = "Management IP Address";

    public static final String MODULE_TYPE = "モジュール種別";

    public static final String NAME = "name";

    public static final String NODE_LOCATION = MplsnmsAttrs.NodeAttr.SHUUYOU_FLOOR.getName();

    public static final String NODE_TYPE = "機種";

    public static final String NODE_TYPE_VM = "VMware VM";

    public static final String NODE_TYPE_VSWITCH = "VMware vSwitch";

    public static final String OPER_STATUS = "動作状態";

    public static final String OPER_STATUS_DEFAULT = "現用";

    public static final String OS_TYPE = "OS種別";

    public static final String OS_VERSION = "OSバージョン";

    public static final String OSPF_AREA_ID = "OSPFエリアID";

    public static final String PERMANENT = "permanent";

    public static final String PORTSPEED_ADMIN = "AdministrativeSpeed";

    public static final String PORTSPEED_OPER = "OperationalSpeed";

    public static final String PREFIX = "Prefix";

    public static final String PSEUDOWIRE_NAME = "PW名";

    public static final String PSEUDOWIRE_TRANSPORT_TYPE = "naef.pseudowire.transport-type";

    public static final String PSEUDOWIRE_TYPE = "PseudoWire種別";

    public static final String SLOT_TYPE = "スロット種別";

    public static final String SNMP_COMMUNITY = "snmp_community (read-only)";

    public static final String SNMP_MODE = "SNMP取得方式";

    public static final String SORT_ORDER = "ソート順";

    public static final String SOURCE = "情報ソース";

    public static final String SUFFIX = "suffix";

    public static final String SVI_ENABLED = "svi_port";

    public static final String TELNET_ACCOUNT = "コンソールログインユーザ名";

    public static final String TELNET_PASSWORD = "コンソールログインパスワード";

    public static final String TIMESLOT = "Timeslot";

    public static final String VENDOR_NAME = "ベンダー名";

    public static final String VIRTUAL_NODE = "仮想ノード";

    public static final String VPLS_NAME = "VPLS名";

    public static final String VRF_NAME = "VRF名";

    public static final String ZONE = "__Zone";

    public static final String ZONE_LIST = "__ZoneList";

    public static final String DIFF_DEBUG_ARCHIVE = "diff.debug-archive";

    public static final String OPER_STATUS_DEBUG_ARCHIVE = "oper-status.debug-archive";

    public static final String ABORTED = "中断中";

    public static final String ACCOMMODATION_SERVICE_TYPE = "収容サービスタイプ";

    public static final String BUILDING_CODE = "ビルコード";

    public static final String CONTRACT_BANDWIDTH = "契約帯域";

    public static final String FIXED_RTT = "固定RTT";

    public static final String EXTERNAL_INVENTORY_DB_ID = "EXTERNAL_INVENTORY_DB_ID";

    public static final String EXTERNAL_INVENTORY_DB_STATUS = "EXTERNAL_INVENTORY_DB_STATUS";

    public static final String LINK_NAME = "リンク名";

    public static final String LINK_ACCOMMODATION_LIMIT = "収容許容率";

    public static final String LINK_APPROVED = "承認済";

    public static final String LINK_CABLE_NAME = "ケーブル名";

    public static final String LINK_DETOUR_LINK = "迂回路";

    public static final String LINK_DETOUR_DIRECTION = "迂回路方向";

    public static final String LINK_FOUND_ON_NETWORK = "ネットワーク存在";

    public static final String LINK_FOUND_ON_EXTERNAL_INVENTORY_DB = "LINK_FOUND_ON_EXTERNAL_INVENTORY_DB";

    public static final String LINK_SRLG_VALUE = "SRLG値";

    public static final String LINK_TRAFFIC_DIFFUSION_AUTO = "ユーザトラフィック分配比率(自動)";

    public static final String LINK_TRAFFIC_DIFFUSION_MANUAL = "ユーザトラフィック分配比率(手動)";

    public static final String LSP_PAIR = "対向LSP";

    public static final String LSP_PRIMARY_PATH_OPER_STATUS = "primary-path動作状態";

    public static final String LSP_SDP_ID = "sdp-id";

    public static final String LSP_SECONDARY_PATH_OPER_STATUS = "secondary-path動作状態";

    public static final String LSP_TUNNEL_ID = "tunnel-id";

    public static final String NETWORK_ADDRESS = "ネットワークアドレス";

    public static final String OPERATION_BEGIN_DATE = "運用開始日時";

    public static final String BEST_EFFORT_GUARANTEED_BANDWIDTH = "BEST_EFFORT_GUARANTEED_BANDWIDTH";

    public static final String BEST_EFFORT_GUARANTEED_CAPTURE_DATE = "BEST_EFFORT_GUARANTEED_CAPTURE_DATE";

    public static final String POP_NAME = "POP名";

    public static final String REGISTERED_DATE = "登録日時";

    public static final String REGULATION_ROUTE_DEFAULT = "regulation-default";

    public static final String SERVICE_ID = "サービスID";

    public static final String SERVICE_TYPE = "サービス種別";

    public static final String SETUP_DATE = "開通日時";

    public static final String STORMCONTROL_ACTION = "storm-control action";

    public static final String STORMCONTROL_ACTION_VALUE_SHUTDOWN = "shutdown";

    public static final String STORMCONTROL_ACTION_VALUE_TRAP = "trap";

    public static final String STORMCONTROL_BROADCAST_LEVEL = "storm-control broadcast level";

    public static final String USERLINE_ID = "回線ID";

    public static final String VARIABLE_RTT = "可変RTT";

    public static final String VISIBLE_FLAG = "表示フラグ";

}