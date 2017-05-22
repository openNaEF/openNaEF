package voss.multilayernms.inventory.nmscore.view.datamodifier;

import jp.iiga.nmt.core.model.*;
import naef.dto.NaefDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ShellConnector;
import voss.core.server.naming.inventory.InventoryIdDecoder;
import voss.multilayernms.inventory.constants.ViewConstants;

import java.util.Map;

public class LayoutSaver {

    private final Logger log = LoggerFactory.getLogger(LayoutSaver.class);

    private final Diagram target;
    private final String userName;

    public LayoutSaver(Diagram target, String userName) {
        this.target = target;
        this.userName = userName;
        ShellCommands cmd = new ShellCommands(userName);
        for (PositionalModel model : target.getChildren()) {
            if (!isPositionSaveTarget(model)) {
                continue;
            }
            Map<String, Object> map = model.getProperties();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
            String constraint = (String) map.get(ViewConstants.VIEW_POSITION);
            if (constraint == null) {
                continue;
            }
            Device device = (Device) model;
            InventoryBuilder.changeContext(cmd, device.getName());
            InventoryBuilder.buildAttributeSetOrReset(cmd, ViewConstants.ATTR_POSITION, constraint);
            for (PhysicalEthernetPort port : device.getPorts()) {
                Integer portConstraint = (Integer) port.getPropertyValue(ViewConstants.PORT_POSITION);
                String value = (portConstraint == null ? null : portConstraint.toString());
                String id = port.getId();
                try {
                    NaefDto dto = InventoryIdDecoder.getDto(id);
                    InventoryBuilder.changeContext(cmd, dto);
                    InventoryBuilder.buildAttributeSetOrReset(cmd, ViewConstants.ATTR_POSITION, value);
                } catch (Exception e) {
                }
            }
        }
        try {
            ShellConnector.getInstance().execute2(cmd);
        } catch (Exception e) {
            log.error("failed to save layout.", e);
            throw new IllegalStateException("failed to save layout: " + e.getMessage());
        }
    }

    private boolean isPositionSaveTarget(PositionalModel model) {
        if (model == null) {
            return false;
        } else if (model instanceof Device) {
            return true;
        }
        return false;
    }

    public Diagram getTarget() {
        return target;
    }

    public String getUserName() {
        return (userName == null ? "null" : userName);
    }

    public IDiagram save() {
        if (getTarget() == null) {
            throw new IllegalArgumentException("can't save layout : diagram.size() is not 1.");
        }

        return getTarget();
    }

}