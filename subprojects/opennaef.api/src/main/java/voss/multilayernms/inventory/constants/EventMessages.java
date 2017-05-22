package voss.multilayernms.inventory.constants;

import voss.core.server.naming.inventory.InventoryIdDecoder;
import voss.core.server.util.ExceptionUtils;

import java.text.ParseException;

public class EventMessages {

    private static final String ID = "_ID_";

    private static final String PORT_DOWN = "Port _ID_ state changed to DOWN";

    public static String getPortDownMessage(String id) {
        try {
            String ifName = InventoryIdDecoder.getPortIfName(id);
            return PORT_DOWN.replace(ID, ifName);
        } catch (ParseException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private static final String PORT_UP = "Port _ID_ state changed to UP";

    public static String getPortUpMessage(String id) {
        try {
            String ifName = InventoryIdDecoder.getPortIfName(id);
            return PORT_UP.replace(ID, ifName);
        } catch (ParseException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static String getPortMessage(boolean isUP, String id) {
        if (isUP) {
            return getPortUpMessage(id);
        } else {
            return getPortDownMessage(id);
        }
    }

    private static final String LINK_DOWN = "Link _ID_ state changed to DOWN";

    public static String getLinkDownMessage(String id) {
        return LINK_DOWN.replace(ID, id);
    }

    private static final String LINK_UP = "Link _ID_ state changed to UP";

    public static String getLinkUpMessage(String id) {
        return LINK_UP.replace(ID, id);
    }

    public static String getLinkMessage(boolean isUP, String id) {
        if (isUP) {
            return getLinkUpMessage(id);
        } else {
            return getLinkDownMessage(id);
        }
    }

    private static final String LSP_DOWN = "LSP _ID_ state changed to DOWN";

    public static String getLspDownMessage(String id) {
        try {
            String lspName = InventoryIdDecoder.getLspName(id);
            return LSP_DOWN.replace(ID, lspName);
        } catch (ParseException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private static final String LSP_UP = "LSP _ID_ state changed to UP";

    public static String getLspUpMessage(String id) {
        try {
            String lspName = InventoryIdDecoder.getLspName(id);
            return LSP_UP.replace(ID, lspName);
        } catch (ParseException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static String getLspMessage(boolean isUP, String id) {
        if (isUP) {
            return getLspUpMessage(id);
        } else {
            return getLspDownMessage(id);
        }
    }

    private static final String LSP_ACTIVE_PATH_CHANGED = "LSP _ID_ active path changed";

    public static String getLspActivePathChangedMessage(String id) {
        try {
            String lspName = InventoryIdDecoder.getLspName(id);
            return LSP_ACTIVE_PATH_CHANGED.replace(ID, lspName);
        } catch (ParseException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private static final String PW_DOWN = "PW _ID_ state changed to DOWN";

    public static String getPseudoWireDownMessage(String id) {
        try {
            String pwID = InventoryIdDecoder.getPseudoWireID(id);
            return PW_DOWN.replace(ID, pwID);
        } catch (ParseException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private static final String PW_UP = "PW _ID_ state changed to UP";

    public static String getPseudoWireUpMessage(String id) {
        try {
            String pwID = InventoryIdDecoder.getPseudoWireID(id);
            return PW_UP.replace(ID, pwID);
        } catch (ParseException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static String getPseudoWireMessage(boolean isUP, String id) {
        if (isUP) {
            return getPseudoWireUpMessage(id);
        } else {
            return getPseudoWireDownMessage(id);
        }
    }

    private static final String PATH_DOWN = "PATH _ID_ state changed to DOWN";

    public static String getPathDownMessage(String id) {
        try {
            String lspName = InventoryIdDecoder.getLspName(id);
            String pathName = InventoryIdDecoder.getPathName(id);
            return PATH_DOWN.replace(ID, lspName + ":" + pathName);
        } catch (ParseException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private static final String PATH_UP = "PATH _ID_ state changed to UP";

    public static String getPathUpMessage(String id) {
        try {
            String lspName = InventoryIdDecoder.getLspName(id);
            String pathName = InventoryIdDecoder.getPathName(id);
            return PATH_UP.replace(ID, lspName + ":" + pathName);
        } catch (ParseException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static String getPathMessage(boolean isUP, String id) {
        if (isUP) {
            return getPathUpMessage(id);
        } else {
            return getPathDownMessage(id);
        }
    }
}