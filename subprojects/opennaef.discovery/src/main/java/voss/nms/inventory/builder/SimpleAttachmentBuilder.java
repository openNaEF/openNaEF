package voss.nms.inventory.builder;

import naef.dto.LocationDto;
import naef.dto.NaefDto;
import naef.dto.NodeElementDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vpls.VplsDto;
import naef.dto.vrf.VrfDto;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.InventoryException;
import voss.nms.inventory.element.HyperLink;

public class SimpleAttachmentBuilder extends InventoryBuilder {
    public static final String CMD_ADD_ATTACHMENT = "attribute add _KEY_ _VALUE_";
    public static final String CMD_REMOVE_ATTACHMENT = "attribute remove _KEY_ _VALUE_";
    public static final String fileKey = "Attachment file";
    public static final String urlKey = "Reference link";

    public static void addAttachment(NaefDto dto, String key, String caption, String url, String editorName)
            throws InventoryException {
        addAttachment(dto, key, HyperLink.getLinkWithCaption(caption, url), editorName);
    }

    public static void addAttachment(NaefDto dto, String key, String value, String editorName)
            throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        if (dto instanceof NodeElementDto) {
            changeContext(commands, (NodeElementDto) dto);
        } else if (dto instanceof VlanDto) {
            changeContext(commands, (VlanDto) dto);
        } else if (dto instanceof VplsDto) {
            changeContext(commands, (VplsDto) dto);
        } else if (dto instanceof VrfDto) {
            changeContext(commands, (VrfDto) dto);
        } else if (dto instanceof PseudowireDto) {
            changeContext(commands, (PseudowireDto) dto);
        } else if (dto instanceof LocationDto) {
            changeContext(commands, (LocationDto) dto);
        }
        commands.addVersionCheckTarget(dto);
        String cmd = translate(CMD_ADD_ATTACHMENT, "_KEY_", key, "_VALUE_", value);
        commands.addCommand(cmd);
        commands.addLastEditCommands();
        ShellConnector.getInstance().execute2(commands);
    }

    public static void removeAttachment(NaefDto dto, String key, String caption, String url, String editorName)
            throws InventoryException {
        removeAttachment(dto, key, HyperLink.getLinkWithCaption(caption, url), editorName);
    }

    public static void removeAttachment(NaefDto dto, String key, String value, String editorName)
            throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        if (dto instanceof NodeElementDto) {
            changeContext(commands, (NodeElementDto) dto);
        } else if (dto instanceof VlanDto) {
            changeContext(commands, (VlanDto) dto);
        } else if (dto instanceof VplsDto) {
            changeContext(commands, (VplsDto) dto);
        } else if (dto instanceof VrfDto) {
            changeContext(commands, (VrfDto) dto);
        } else if (dto instanceof PseudowireDto) {
            changeContext(commands, (PseudowireDto) dto);
        } else if (dto instanceof LocationDto) {
            changeContext(commands, (LocationDto) dto);
        } else {
            throw new IllegalArgumentException("It is an object that does not correspond to attached file: " + dto.getAbsoluteName());
        }
        String cmd = translate(CMD_REMOVE_ATTACHMENT, "_KEY_", key, "_VALUE_", value);
        commands.addVersionCheckTarget(dto);
        commands.addCommand(cmd);
        commands.addLastEditCommands();
        ShellConnector.getInstance().execute2(commands);
    }

}