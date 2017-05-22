package voss.core.server.builder;

import voss.core.server.database.ATTR;

public class CMD {
    public static final String SET_PRIMARY_DELIMITER = "name-delimiter absolute.primary " + ATTR.NAME_DELIMITER_PRIMARY;
    public static final String SET_SECODNARY_DELIMITER = "name-delimiter absolute.secondary " + ATTR.NAME_DELIMITER_SECONDARY;

    public static final String CONTEXT_RELATIVE = "context \"_TYPE_;_NAME_\"";
    public static final String CONTEXT_BY_ABSOLUTE_NAME = "context \"_NAME_\"";
    public static final String CONTEXT_BY_MVO = "context mvo-id;_MVO_ID_";
    public static final String CONTEXT_RESET = "context";
    public static final String CONTEXT_DOWN = "context ..";

    public static final String ARG_ATTR = "_ATTR_";
    public static final String ARG_EXPR = "_EXPR_";
    public static final String ARG_FQN = "_FQN_";
    public static final String ARG_FQN1 = "_FQN1_";
    public static final String ARG_FQN2 = "_FQN2_";
    public static final String ARG_IFNAME = "_IFNAME_";
    public static final String ARG_INSTANCE = "_INSTANCE_";
    public static final String ARG_KEY = "_KEY_";
    public static final String ARG_LOWER = "_LOWER_";
    public static final String ARG_MVOID = "_MVO_ID_";
    public static final String ARG_NAME = "_NAME_";
    public static final String ARG_PORT = "_PORT_";
    public static final String ARG_TYPE = "_TYPE_";
    public static final String ARG_UPPER = "_UPPER_";
    public static final String ARG_VALUE = "_VALUE_";
    public static final String ARG_VAR = "_VAR_";

    public static final String NAVIGATE = "navigate network-upper-layer \"_UPPERLAYER_\"";

    public static final String ASSIGN = "assign \"_VAR_\"";

    public static final String ATTRIBUTE_SET = "attribute set \"_ATTR_\" \"_VALUE_\"";

    public static final String ATTRIBUTE_RESET = "attribute reset \"_ATTR_\"";

    public static final String ATTRIBUTE_ADD = "attribute add \"_ATTR_\" \"_VALUE_\"";

    public static final String ATTRIBUTE_REMOVE = "attribute remove \"_ATTR_\" \"_VALUE_\"";

    public static final String ATTRIBUTE_PUT_MAP = "attribute put-map \"_ATTR_\" \"_KEY_\" \"_VALUE_\"";

    public static final String ATTRIBUTE_REMOVE_MAP = "attribute remove-map \"_ATTR_\" \"_KEY_\"";

    public static final String ASSERT_ATTRIBUTE_IS = " assert-attribute \"_ATTR_\" is \"_VALUE_\"";
    public static final String ASSERT_ATTRIBUTE_CONTAINS = " assert-attribute \"_ATTR_\" contains \"_VALUE_\"";

    public static final String ASSERT_ATTRIBUTE_IS_NULL_OR_EMPTY = " assert-attribute \"_ATTR_\" ( is-null or is \"\" )";

    public static final String ASSERT_ATTRIBUTE_IS_NULL = " assert-attribute \"_ATTR_\" is-null";

    public static final String ASSERT_ATTRIBUTE_EXPR = " assert-attribute \"_ATTR_\" _EXPR_";

    public static final String NEW_NAMED_OBJECT = "new-named-model";

    public static final String RENAME = "alter-name \"_NAME_\"";
    public static final String RENAME_ARG1 = "_NAME_";

    public static final String ALTER_NAME = "alter-name";

    public static final String POOL_CREATE = "new-hierarchical-model \"_TYPE_\" \"_NAME_\"";
    public static final String POOL_CREATE_ARG1 = "_TYPE_";
    public static final String POOL_CREATE_ARG2 = "_NAME_";

    public static final String POOL_RANGE_ALLOCATE = "configure-id-pool allocate-range _RANGE_";

    public static final String POOL_RANGE_ALLOCATE_ARG1 = "_RANGE_";

    public static final String POOL_RANGE_RELEASE = "configure-id-pool release-range _RANGE_";

    public static final String POOL_RANGE_RELEASE_ARG1 = "_RANGE_";

    public static final String POOL_RANGE_ALTER = "configure-id-pool alter-range _OLD_ _NEW_";

    public static final String RANGE_ALTER_ARG1 = "_OLD_";

    public static final String RANGE_ALTER_ARG2 = "_NEW_";

    public static final String POOL_RANGE_MERGE = "configure-id-pool merge-range _RANGE1_ _RANGE2_";
    public static final String RANGE_MERGE_ARG1 = "_RANGE1_";
    public static final String RANGE_MERGE_ARG2 = "_RANGE2_";

    public static final String POOL_RANGE_SPLIT = "configure-id-pool split-range";

    public static final String NETWORK_CREATE = "new-network \"_TYPE_\"";
    public static final String NETWORK_CREATE_ARG = "_TYPE_";

    public static final String NETWORK_SET_POOL1 = "attribute set \"_POOL_TYPE_\" \"_POOL_NAME_\"";
    public static final String NETWORK_SET_POOL1_ARG1 = "_POOL_TYPE_";
    public static final String NETWORK_SET_POOL1_ARG2 = "_POOL_NAME_";

    public static final String NETWORK_SET_ID2 = "attribute set \"_ID_TYPE_\" \"_ID_\"";
    public static final String NETWORK_SET_ID2_ARG1 = "_ID_TYPE_";
    public static final String NETWORK_SET_ID2_ARG2 = "_ID_";

    public static final String NETWORK_RESET_ID1 = "attribute reset _ID_TYPE_";
    public static final String NETWORK_RESET_ID1_ARG = "_ID_TYPE_";

    public static final String NETWORK_RESET_POOL2 = "attribute reset _POOL_TYPE_";
    public static final String NETWORK_RESET_POOL2_ARG = "_POOL_TYPE_";

    public static final String BIND_NETWORK_INSTANCE = "configure-network-port add-member \"_INSTANCE_\"";

    public static final String UNBIND_NETWORK_INSTANCE = "configure-network-port remove-member \"_INSTANCE_\"";

    public static final String INCLUDE_ELEMENT_TO_NETWORK = "configure-network-ix include \"_FQN_\"";

    public static final String EXCLUDE_ELEMENT_FROM_NETWORK = "configure-network-ix exclude \"_FQN_\"";

    public static final String STACK_LOWER_NETWORK = "configure-network-ix stack \"" + ARG_LOWER + "\"";

    public static final String UNSTACK_LOWER_NETWORK = "configure-network-ix unstack \"" + ARG_LOWER + "\"";


    public static final String CONNECT_NETWORK_INSTANCE_PORT = "configure-port-ix x-connect \"_INSTANCE_\" \"_PORT_\"";

    public static final String DISCONNECT_NETWORK_INSTANCE_PORT = "configure-port-ix x-disconnect \"_INSTANCE_\" \"_PORT_\"";

    public static final String PORT_STACK = "configure-port-ix stack \"" + ARG_LOWER + "\" \"" + ARG_UPPER + "\"";

    public static final String PORT_UNSTACK = "configure-port-ix unstack \"" + ARG_LOWER + "\" \"" + ARG_UPPER + "\"";

    public static final String LINK_CONNECT = "configure-link connect \"" + ARG_TYPE + "\" \"" + ARG_FQN1 + "\" \"" + ARG_FQN2 + "\"";

    public static final String LINK_DISCONNECT = "configure-link disconnect \"" + ARG_NAME + "\"";

    public static final String LINK_DISCONNECT_BY_MVOID = "configure-link disconnect mvo-id;" + ARG_MVOID;

    public static final String CREATE_PSEUDOWIRE = "new-network pseudowire";

    public static final String PSEUDOWIRE_ADD_AC = "configure-pseudowire set-_AC_ \"_FQN_\"";
    public static final String PSEUDOWIRE_ADD_AC_ARG1 = "_AC_";
    public static final String PSEUDOWIRE_ADD_AC_ARG2 = "_FQN_";

    public static final String PSEUDOWIRE_REMOVE_AC = "configure-pseudowire reset-_AC_";
    public static final String PSEUDOWIRE_REMOVE_AC_ARG = "_AC_";

    public static final String ADD_STACK_LAG_LINK = "configure-network-ix stack eth-lag \"_LAGFQN1_\" \"_LAGFQN2_\"";
    public static final String ADD_STACK_LAG_LINK_ARG1 = "_LAGFQN1_";
    public static final String ADD_STACK_LAG_LINK_ARG2 = "_LAGFQN2_";

    public static final String ADD_UNSTACK_LAG_LINK = "configure-network-ix unstack eth-lag \"_FQN1_\" \"_FQN2_\"";

    public static final String CREATE_DEMARCATION_LINK = "configure-demarcation-link set-eth-link";

    public static final String CREATE_TRUNK_ALONE = "configure-demarcation-link set-trunk \"_IFNAME_\"";

    public static final String CREATE_VLAN_ALONE = "configure-demarcation-link set-vlan";

    public static final String DELETE_DEMARCATION_LINK = "configure-demarcation-link reset-eth-link";

    public static final String DELETE_TRUNK_ALONE = "configure-demarcation-link reset-trunk \"_IFNAME_\"";

    public static final String DELETE_VLAN_ALONE = "configure-demarcation-link reset-vlan";

    public static final String CREATE_VPLSIF = "new-port vpls-if \"_VPLSIFNAME_\"";

    public static final String CREATE_VRFIF = "new-port vrf-if \"_VRFIFNAME_\"";

    public static final String HOP_ADD = "configure-path-hop add-hop \"_TYPE_\" \"_NAME_\"";
    public static final String HOP_ADD_ARG1 = "_TYPE_";
    public static final String HOP_ADD_ARG2 = "_NAME_";

    public static final String HOP_REMOVE = "configure-path-hop reset-hops";

    public static final String BUNDLE_PORT_INCLUDE = "configure-port-ix include \"_BUNDLE_\" \"_MEMBER_\"";

    public static final String BUNDLE_PORT_EXCLUDE = "configure-port-ix exclude \"_BUNDLE_\" \"_MEMBER_\"";

    public static final String BUNDLE_PORT_ARG1_BUNDLE = "_BUNDLE_";
    public static final String BUNDLE_PORT_ARG2_MEMBER = "_MEMBER_";

    public static final String NODE_GROUP_CREATE = "new-named-model node-group \"_NAME_\"";
    public static final String NODE_GROUP_CREATE_KEY_1 = "_NAME_";

    public static final String NODE_GROUP_ADD_NODE = " attribute add " + ATTR.NODEGROUP_MEMBER + " \"_NODENAME_\"";
    public static final String NODE_GROUP_ADD_NODE_KEY1 = "_NODENAME_";

    public static final String NODE_GROUP_REMOVE_NODE = " attribute remove " + ATTR.NODEGROUP_MEMBER + " \"_NODENAME_\"";
    public static final String NODE_GROUP_REMOVE_NODE_KEY1 = "_NODENAME_";

    public static final String CREATE_NODE = "new-node \"_NODENAME_\"";
    public static final String CREATE_NODE_KEY_1 = "_NODENAME_";

    public static final String NEW_HARDWARE = "new-hardware \"_TYPE_\" \"_ID_\"";
    public static final String NEW_HARDWARE_BODY = "new-hardware";
    public static final String NEW_HARDWARE_KEY_1 = "_TYPE_";
    public static final String NEW_HARDWARE_KEY_2 = "_ID_";

    public static final String NEW_PORT = "new-port \"_TYPE_\" \"_NAME_\"";
    public static final String NEW_PORT_KEY_1 = "_TYPE_";
    public static final String NEW_PORT_KEY_2 = "_NAME_";

    public static final String REMOVE_ELEMENT = "remove-element \"_TYPE_\" \"_NAME_\"";
    public static final String REMOVE_ELEMENT_KEY_1 = "_TYPE_";
    public static final String REMOVE_ELEMENT_KEY_2 = "_NAME_";

    public static final String HIERARCHICAL_MODEL_CREATE = "new-hierarchical-model \"_TYPE_\" \"_NAME_\"";
    public static final String HIERARCHICAL_MODEL_CREATE_ARG1 = "_TYPE_";
    public static final String HIERARCHICAL_MODEL_CREATE_ARG2 = "_NAME_";

    public static final String HIERARCHICAL_MODEL_PARENT_CHANGE = "alter-hierarchical-model-parent \"_NEW_PARENT_\"";
    public static final String HIERARCHICAL_MODEL_PARENT_CHANGE_ARG = "_NEW_PARENT_";

    public static final String CUSTOMER_INFO_CREATE = "new-named-model customer-info \"_NAME_\"";
    public static final String CUSTOMER_INFO_CREATE_KEY_1 = "_NAME_";

    public static final String TASK_NEW = "attribute-task new";

    public static final String TASK_ADD_ENTRY = "attribute-task add-entry \"_TYPE_\" \"_VALUE_\"";
    public static final String TASK_ADD_ENTRY_ARG1_TYPE = "_TYPE_";
    public static final String TASK_ADD_ENTRY_ARG2_VALUE = "_VALUE_";

    public static final String TASK_CANCEL = "attribute-task cancel";

    public static final String TASK_COMMIT = "attribute-task activate";

    public static final String SYSTEM_USER_CREATE = NEW_NAMED_OBJECT + " " + ATTR.TYPE_SYSTEM_USER + " \"_NAME_\"";


    public static final String OF_PATCH_LINK_CREATE = "new-network " + ATTR.TYPE_OF_PATCH_LINK + " \"_NAME_\"";
    public static final String OF_PATCH_LINK_SET_PORT1 = "set-patch-port1";
    public static final String OF_PATCH_LINK_SET_PORT2 = "set-patch-port2";
    public static final String OF_PATCH_LINK_RESET_PORT1 = "reset-patch-port1";
    public static final String OF_PATCH_LINK_RESET_PORT2 = "reset-patch-port2";
    public static final String OF_PATCH_LINK_CONFIGURE_SET_PORT = "configure-of-patch-link \"_OP_\" \"_NAME_\"";
    public static final String OF_PATCH_LINK_CONFIGURE_RESET_PORT = "configure-of-patch-link \"_OP_\"";
}