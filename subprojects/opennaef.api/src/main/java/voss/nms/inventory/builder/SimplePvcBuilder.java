package voss.nms.inventory.builder;

import naef.dto.HardPortDto;
import naef.dto.PortDto;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.atm.AtmPvpIfDto;
import naef.dto.fr.FrPvcIfDto;
import naef.dto.serial.SerialPortDto;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ATTR;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.Util;

import java.util.Map;

public class SimplePvcBuilder extends InventoryBuilder {

    public static final String CMD_CREATE_ATM_PVP = "new-port atm-pvp-if _VPI_";
    public static final String CMD_CREATE_ATM_PVC = "new-port atm-pvc-if _VPI_/_VCI_";
    public static final String CMD_CREATE_FR_PVC = "new-port fr-pvc-if _DLCI_";
    public static final String CMD_CREATE_SERIAL_SUBIF = "new-port serial-port _SUFFIX_";
    public static final String KEY_VPI = "_VPI_";
    public static final String KEY_VCI = "_VCI_";
    public static final String KEY_DLCI = "_DLCI_";
    public static final String KEY_SUFFIX = "_SUFFIX_";

    public static void buildAtmPvpCreationCommand(ShellCommands commands, PortDto port,
                                                  String suffix, int vpi, boolean permanent) {
        changeContext(commands, port);
        commands.addCommand(translate(CMD_CREATE_ATM_PVP, KEY_VPI, String.valueOf(vpi)));
        buildAttributeSetOrReset(commands, ATTR.ATTR_ATM_PVP_VPI, String.valueOf(vpi));
        if (suffix != null) {
            buildAttributeSetOrReset(commands, ATTR.SUFFIX, suffix);
        }
        if (permanent) {
            buildAttributeSetOrReset(commands, ATTR.IMPLICIT, null);
        } else {
            buildAttributeSetOrReset(commands, ATTR.IMPLICIT, Boolean.TRUE.toString());
        }
    }

    public static void buildAtmPvpCreationCommand(ShellCommands cmd, String atmPortName,
                                                  String suffix, int vpi, boolean permanent) {
        changeContext(cmd, atmPortName);
        translate(cmd, CMD_CREATE_ATM_PVP, KEY_VPI, String.valueOf(vpi));
        buildAttributeSetOrReset(cmd, ATTR.ATTR_ATM_PVP_VPI, String.valueOf(vpi));
        if (suffix != null) {
            buildAttributeSetOrReset(cmd, ATTR.SUFFIX, suffix);
        }
        if (permanent) {
            buildAttributeSetOrReset(cmd, ATTR.IMPLICIT, null);
        } else {
            buildAttributeSetOrReset(cmd, ATTR.IMPLICIT, Boolean.TRUE.toString());
        }
    }

    public static void buildAtmPvcCreationCommand(ShellCommands commands, HardPortDto port,
                                                  String suffix, int vpi, int vci, Map<String, String> attributes) {
        changeContext(commands, port, ATTR.TYPE_ATM_PVP_IF, String.valueOf(vpi));
        commands.addCommand(translate(CMD_CREATE_ATM_PVC, KEY_VPI, String.valueOf(vpi), KEY_VCI, String.valueOf(vci)));
        buildAttributeSetOrReset(commands, ATTR.ATTR_ATM_PVC_VCI, String.valueOf(vci));
        buildAttributeSetOrReset(commands, ATTR.SUFFIX, suffix);
        buildAttributeSetOnCurrentContextCommands(commands, attributes);
    }

    public static void deleteAtmPvc(AtmPvcIfDto pvc, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, pvc);
        commands.addCommand(CMD.CONTEXT_DOWN);
        commands.addCommand(translate(CMD.REMOVE_ELEMENT, "_TYPE_", ATTR.TYPE_ATM_PVC_IF, "_NAME_", pvc.getName()));
        ShellConnector.getInstance().execute2(commands);
    }

    public static void deleteAtmPvp(AtmPvpIfDto pvp, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, pvp);
        commands.addCommand(CMD.CONTEXT_DOWN);
        commands.addCommand(translate(CMD.REMOVE_ELEMENT, "_TYPE_", ATTR.TYPE_ATM_PVP_IF, "_NAME_", pvp.getName()));
        ShellConnector.getInstance().execute2(commands);
    }

    public static void buildFrameRelayPvcCreationCommands(ShellCommands commands, HardPortDto port,
                                                          int dlci, String suffix, Map<String, String> attributes) throws InventoryException {
        buildFrPvcCreationCommand(commands, port, dlci, suffix);
        buildAttributeSetOnCurrentContextCommands(commands, attributes);
    }

    public static void updateFrameRelayPvcAttributes(FrPvcIfDto pvc, String suffix, Map<String, String> attributes, String editorName)
            throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, pvc);
        buildAttributeSetOrReset(commands, ATTR.SUFFIX, suffix);
        buildAttributeUpdateCommand(commands, pvc, attributes);
        ShellConnector.getInstance().execute2(commands);
    }

    protected static void buildFrPvcCreationCommand(ShellCommands commands, HardPortDto port, int dlci, String suffix) {
        changeContext(commands, port);
        commands.addCommand(translate(CMD_CREATE_FR_PVC, KEY_DLCI, String.valueOf(dlci)));
        buildAttributeSetOrReset(commands, ATTR.ATTR_FR_DLCI, String.valueOf(dlci));
        buildAttributeSetOrReset(commands, ATTR.SUFFIX, suffix);
    }

    public static void deleteFrameRelayPvc(FrPvcIfDto fr, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, fr);
        commands.addCommand(CMD.CONTEXT_DOWN);
        commands.addCommand(translate(CMD.REMOVE_ELEMENT, "_TYPE_", ATTR.TYPE_FR_PVC_IF, "_NAME_", fr.getName()));
        ShellConnector.getInstance().execute2(commands);
    }

    public static void buildSerialSubInterfaceCreationCommand(ShellCommands commands, HardPortDto port, String suffix) {
        changeContext(commands, port);
        commands.addCommand(translate(CMD_CREATE_SERIAL_SUBIF, KEY_SUFFIX, suffix));
        buildAttributeSetOrReset(commands, ATTR.SUFFIX, suffix);
    }

    public static void buildSerialSubInterfaceRenameCommand(ShellCommands commands, SerialPortDto serial, String newSuffix) {
        changeContext(commands, serial);
        commands.addCommand(translate(CMD.RENAME, CMD.RENAME_ARG1, newSuffix));
        buildAttributeSetOrReset(commands, ATTR.SUFFIX, newSuffix);
    }

    public static void buildSerialSubInterfaceDeletionCommand(ShellCommands commands, SerialPortDto serial) {
        changeContext(commands, serial);
        commands.addCommand(CMD.CONTEXT_DOWN);
        commands.addCommand(translate(CMD.REMOVE_ELEMENT, CMD.ARG_TYPE, serial.getObjectTypeName(), CMD.ARG_NAME, serial.getName()));
    }

    public static final String CMD_CONNECT_ATM_PVC = "connect-link atm-pvc \"_PVC1_\" \"_PVC2_\"";
    public static final String CMD_CONNECT_FR_PVC = "connect-link fr-pvc \"_PVC1_\" \"_PVC2_\"";
    public static final String KEY_CONNECT_PVC_1 = "_PVC1_";
    public static final String KEY_CONNECT_PVC_2 = "_PVC2_";

    protected static void buildFrPvcConnectionCreationCommand(ShellCommands commands, FrPvcIfDto fr1,
                                                              FrPvcIfDto fr2, String editorName) {
        if (Util.isNull(fr1, fr2)) {
            throw new IllegalArgumentException("fr-pvc is null.");
        }
        String fqn1 = fr1.getAbsoluteName();
        String fqn2 = fr2.getAbsoluteName();
        commands.addCommand(translate(CMD_CONNECT_FR_PVC, KEY_CONNECT_PVC_1, fqn1, KEY_CONNECT_PVC_2, fqn2));
        changeContext(commands, fr1);
        commands.addLastEditCommands();
        changeContext(commands, fr2);
        commands.addLastEditCommands();
    }

    protected static void buildAtmPvcConnectionCreationCommand(ShellCommands commands,
                                                               AtmPvcIfDto pvc1, AtmPvcIfDto pvc2, String editorName) {
        if (Util.isNull(pvc1, pvc2)) {
            throw new IllegalArgumentException("atm-pvc is null.");
        }
        String fqn1 = pvc1.getAbsoluteName();
        String fqn2 = pvc2.getAbsoluteName();
        commands.addCommand(translate(CMD_CONNECT_ATM_PVC, KEY_CONNECT_PVC_1, fqn1, KEY_CONNECT_PVC_2, fqn2));
        changeContext(commands, pvc1);
        commands.addLastEditCommands();
        changeContext(commands, pvc2);
        commands.addLastEditCommands();
    }
}