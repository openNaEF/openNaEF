package voss.core.server.constant;

import voss.core.server.database.ATTR;

public class ModelConstant {
    public static final String LOCATION_TRASH_NAME = "Trash";
    public static final String VLAN_POOL_TRASH_NAME = "vlanPoolTrash";
    public static final String VPLS_POOL_TRASH_NAME = "vplsStringPoolTrash";
    public static final String VRF_POOL_TRASH_NAME = "vrfStringPoolTrash";
    public static final String PSEUDOWIRE_POOL_TRASH_NAME = "pwStringPoolTrash";
    public static final String IP_SUBNET_ADDRESS_TRASH_NAME = "IpSubnetAddressTrash";
    public static final String IP_SUBNET_NAMESPACE_TRASH_NAME = "IpSubnetNamespaceTrash";

    public static final String LOCATION_TRASH = ATTR.TYPE_LOCATION + ";" + LOCATION_TRASH_NAME;
    public static final String VLAN_POOL_TRASH = ATTR.POOL_TYPE_VLAN_DOT1Q + ";" + VLAN_POOL_TRASH_NAME;
    public static final String VPLS_POOL_TRASH = ATTR.POOL_TYPE_VPLS_STRING_TYPE + ";" + VPLS_POOL_TRASH_NAME;
    public static final String VRF_POOL_TRASH = ATTR.POOL_TYPE_VRF_STRING_TYPE + ";" + VRF_POOL_TRASH_NAME;
    public static final String PSEUDOWIRE_POOL_TRASH = ATTR.POOL_TYPE_PSEUDOWIRE_STRING_TYPE + ";" + PSEUDOWIRE_POOL_TRASH_NAME;
    public static final String IP_SUBNET_ADDRESS_TRASH = ATTR.POOL_TYPE_IPSUBNET_ADDRESS + ";" + IP_SUBNET_ADDRESS_TRASH_NAME;
    public static final String IP_SUBNET_NAMESPACE_TRASH = ATTR.POOL_TYPE_IPSUBNET + ";" + IP_SUBNET_NAMESPACE_TRASH_NAME;
}