package voss.nms.inventory.builder;

import naef.dto.PortDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.PseudowireLongIdPoolDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ATTR;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.database.InventoryConnector;

import java.util.Map;

public class SimplePseudoWireBuilder extends InventoryBuilder {
    private static final Logger log = LoggerFactory.getLogger(SimplePseudoWireBuilder.class);

    public static void createNewPseudoWireLongTypeIdPool(String domain, String name, String range,
                                                         Map<String, String> attributes, String editorName) {
        ShellCommands commands = new ShellCommands(editorName);
        try {
            checkDuplication(name);
            buildPseudoWireLongTypeIdPoolCreationCommands(commands, name, domain, range, attributes);
            commands.addLastEditCommands();
            ShellConnector.getInstance().execute2(commands);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static void buildPseudoWireLongTypeIdPoolCreationCommands(ShellCommands commands, String name,
                                                                       String parent, String range, Map<String, String> attributes) {
        changeContext(commands, ATTR.POOL_TYPE_PSEUDOWIRE_LONG_TYPE, parent);
        commands.addCommand(translate(CMD.POOL_CREATE,
                CMD.POOL_CREATE_ARG1, ATTR.POOL_TYPE_PSEUDOWIRE_LONG_TYPE,
                CMD.POOL_CREATE_ARG2, name));
        commands.addCommand(translate(CMD.POOL_RANGE_ALLOCATE, "_RANGE_", range));
        buildAttributeSetOnCurrentContextCommands(commands, attributes);
    }

    public static void createNewPseudoWireStringTypeIdPool(String domain, String name, String range,
                                                         Map<String, String> attributes, String editorName) {
        ShellCommands commands = new ShellCommands(editorName);
        try {
            checkDuplication(name);
            buildPseudoWireStringTypeIdPoolCreationCommands(commands, name, domain, range, attributes);
            commands.addLastEditCommands();
            ShellConnector.getInstance().execute2(commands);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static void buildPseudoWireStringTypeIdPoolCreationCommands(ShellCommands commands, String name,
                                                                       String parent, String range, Map<String, String> attributes) {
        changeContext(commands, ATTR.POOL_TYPE_PSEUDOWIRE_STRING_TYPE, parent);
        commands.addCommand(translate(CMD.POOL_CREATE,
                CMD.POOL_CREATE_ARG1, ATTR.POOL_TYPE_PSEUDOWIRE_STRING_TYPE,
                CMD.POOL_CREATE_ARG2, name));
        commands.addCommand(translate(CMD.POOL_RANGE_ALLOCATE, "_RANGE_", range));
        buildAttributeSetOnCurrentContextCommands(commands, attributes);
    }

    public static void updatePseudoWireIdPoolAttributes(PseudowireLongIdPoolDto pool, Map<String, String> attributes, String editorName)
            throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, pool);
        buildAttributeUpdateCommand(commands, pool, attributes);
        ShellConnector.getInstance().execute2(commands);
    }

    public static void buildPseudoWireIdPoolAttributeUpdateCommands(ShellCommands commands,
                                                                    PseudowireLongIdPoolDto pool, Map<String, String> attributes) {
        if (pool != null) {
            changeContext(commands, pool);
        }
        buildAttributeUpdateCommand(commands, pool, attributes);
    }

    public static void buildPseudoWireIdRangeChangeCommands(ShellCommands commands, PseudowireLongIdPoolDto pool, String newRange)
            throws InventoryException {
        buildChangeRangeCommand(commands, pool.getConcatenatedIdRangesStr(), newRange, false);
    }

    public static void buildChangePseudoWireIdPoolContextCommand(ShellCommands commands, PseudowireLongIdPoolDto pool) {
        changeContext(commands, pool);
    }

    private static void checkDuplication(String name) throws InventoryException {
        try {
            PseudowireLongIdPoolDto dto = InventoryConnector.getInstance().getPseudoWireLongIdPool(name);
            if (dto != null) {
                throw new InventoryException("Duplicate PseudoWire ID pool name.");
            }
        } catch (Exception e) {

        }
    }

    public static void updatePseudoWireAc(PseudowireDto pw, PortDto ac1, PortDto ac2, String editorName) {
        try {
            ShellCommands commands = new ShellCommands(editorName);
            changeContext(commands, pw);
            String cmd2 = translate(CMD.PSEUDOWIRE_ADD_AC,
                    CMD.PSEUDOWIRE_ADD_AC_ARG1, "ac1",
                    CMD.PSEUDOWIRE_ADD_AC_ARG2, ac1.getAbsoluteName());
            commands.addCommand(cmd2);
            String cmd3 = translate(CMD.PSEUDOWIRE_ADD_AC,
                    CMD.PSEUDOWIRE_ADD_AC_ARG1, "ac2",
                    CMD.PSEUDOWIRE_ADD_AC_ARG2, ac2.getAbsoluteName());
            commands.addCommand(cmd3);
            commands.addLastEditCommands();
            ShellConnector.getInstance().execute2(commands);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }


    public static void buildPseudoWireAc1AttachCommand(ShellCommands commands, PseudowireDto pw, PortDto port) throws InventoryException {
        buildPseudoWireAcUpdateCommand(commands, "1", pw, pw.getAc1(), port);
    }

    public static void buildPseudoWireAc2AttachCommand(ShellCommands commands, PseudowireDto pw, PortDto port) throws InventoryException {
        buildPseudoWireAcUpdateCommand(commands, "2", pw, pw.getAc2(), port);
    }

    public static void buildPseudoWireAc1DetachCommand(ShellCommands commands, PseudowireDto pw) throws InventoryException {
        buildPseudoWireAcUpdateCommand(commands, "1", pw, pw.getAc1(), null);
    }

    public static void buildPseudoWireAc2DetachCommand(ShellCommands commands, PseudowireDto pw) throws InventoryException {
        buildPseudoWireAcUpdateCommand(commands, "2", pw, pw.getAc2(), null);
    }

    private static void buildPseudoWireAcUpdateCommand(ShellCommands commands, String suffix, PseudowireDto pw,
                                                       PortDto before, PortDto after) throws InventoryException {
        if (pw == null) {
            log.info("pw is null. skipped.");
            return;
        }
        changeContext(commands, pw);
        if (isRemoved(before, after)) {
            String cmd2 = translate(CMD.PSEUDOWIRE_REMOVE_AC,
                    CMD.PSEUDOWIRE_REMOVE_AC_ARG, suffix);
            commands.addCommand(cmd2);
        }
        if (isAdded(before, after)) {
            String cmd2 = translate(CMD.PSEUDOWIRE_ADD_AC,
                    CMD.PSEUDOWIRE_ADD_AC_ARG1, suffix,
                    CMD.PSEUDOWIRE_ADD_AC_ARG2, after.getAbsoluteName());
            commands.addCommand(cmd2);
        }
    }

    public static void buildPseudoWireUpdateCommands(ShellCommands commands,
                                                     PseudowireDto pw, Map<String, String> attributes)
            throws InventoryException {
        changeContext(commands, pw);
        buildAttributeUpdateCommand(commands, pw, attributes);
    }

}