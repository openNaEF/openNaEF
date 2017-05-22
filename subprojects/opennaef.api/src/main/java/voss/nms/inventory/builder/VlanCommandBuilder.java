package voss.nms.inventory.builder;

import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIdPoolDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.Util;
import voss.core.server.util.VlanUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;

public class VlanCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;

    protected VlanIdPoolDto pool;
    protected VlanDto vlan;
    protected Integer vlanID;
    protected String operStatus;

    public VlanCommandBuilder(VlanDto target, String editorName) {
        super(VlanDto.class, target, editorName);
        if (target == null) {
            throw new IllegalStateException("vlan is null.");
        }
        setConstraint(VlanDto.class);
        this.vlan = target;
        this.pool = target.getIdPool();
    }

    public VlanCommandBuilder(VlanIdPoolDto pool, String editorName) {
        super(VlanDto.class, null, editorName);
        if (pool == null) {
            throw new IllegalStateException("pool is null.");
        }
        setConstraint(VlanDto.class);
        this.vlan = null;
        this.pool = pool;
        this.operStatus = MPLSNMS_ATTR.OPER_STATUS_DEFAULT;
    }

    public void initialize() {
        if (this.vlan == null) {
            return;
        }
        this.vlanID = this.vlan.getVlanId();
        this.operStatus = DtoUtil.getStringOrNull(this.vlan, MPLSNMS_ATTR.OPER_STATUS);
    }

    public void setVlanID(Integer id) {
        if (Util.equals(this.vlanID, id)) {
            return;
        }
        recordChange("VLAN ID", this.vlanID, id);
        this.vlanID = id;
    }

    public void setPool(VlanIdPoolDto pool) {
        if (pool == null) {
            throw new IllegalArgumentException("pool is null.");
        }
        if (DtoUtil.isSameMvoEntity(this.pool, pool)) {
            return;
        }
        recordChange(ATTR.ATTR_VLAN_POOL, this.pool, pool);
        this.pool = pool;
    }

    public void setPurpose(String purpose) {
        setValue(MPLSNMS_ATTR.PURPOSE, purpose);
    }

    public void setNote(String note) {
        setValue(MPLSNMS_ATTR.NOTE, note);
    }

    public void setFacilityStatus(String status) {
        setValue(MPLSNMS_ATTR.FACILITY_STATUS, status);
    }

    public void setOperStatus(String status) {
        setValue(MPLSNMS_ATTR.OPER_STATUS, status);
        this.operStatus = status;
    }

    public void setEndUserName(String name) {
        setValue(MPLSNMS_ATTR.END_USER, name);
    }

    protected BuildResult buildCommandInner() throws IOException, InventoryException {
        if (!hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        if (this.vlan == null) {
            InventoryBuilder.changeContext(cmd, pool);
            InventoryBuilder.buildNetworkIDCreationCommand(cmd, ATTR.NETWORK_TYPE_VLAN,
                    ATTR.ATTR_VLAN_ID, this.vlanID.toString(),
                    ATTR.ATTR_VLAN_POOL, pool.getAbsoluteName());
            cmd.addLastEditCommands();
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes);
        } else {
            InventoryBuilder.changeContext(this.cmd, this.vlan);
            InventoryBuilder.buildAttributeUpdateCommand(this.cmd, this.vlan, this.attributes, false);
            VlanIdPoolDto currentPool = vlan.getIdPool();
            if (!DtoUtil.isSameMvoEntity(currentPool, this.pool)) {
                VlanDto existing = VlanUtil.getVlan(this.pool, this.vlanID);
                if (existing != null) {
                    throw new IllegalStateException("A VLAN with the same ID already exists. " +
                            "Please specify a different pool. [" + this.pool.getName() + "]");
                }
                InventoryBuilder.changeContext(this.cmd, this.vlan);
                InventoryBuilder.buildNetworkIDReleaseCommand(this.cmd,
                        ATTR.ATTR_VLAN_ID, ATTR.ATTR_VLAN_POOL);
                InventoryBuilder.buildNetworkIDAssignmentCommand(this.cmd,
                        ATTR.ATTR_VLAN_ID, this.vlanID.toString(),
                        ATTR.ATTR_VLAN_POOL, this.pool.getAbsoluteName());
            }
        }
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, InventoryException {
        if (this.vlan == null) {
            return BuildResult.NO_CHANGES;
        }
        InventoryBuilder.changeContext(this.cmd, this.vlan);
        InventoryBuilder.buildNetworkIDReleaseCommand(this.cmd,
                ATTR.ATTR_VLAN_ID, ATTR.ATTR_VLAN_POOL);
        return BuildResult.SUCCESS;
    }

    public ShellCommands getCommands() {
        return this.cmd;
    }

    public VlanIdPoolDto getPool() {
        return this.pool;
    }

    public String getContext() {
        if (this.vlan != null) {
            return this.vlan.getAbsoluteName();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(this.pool.getAbsoluteName());
            sb.append(ATTR.NAME_DELIMITER_PRIMARY).append(ATTR.TYPE_ID);
            sb.append(ATTR.NAME_DELIMITER_SECONDARY).append(this.vlanID);
            return sb.toString();
        }
    }

    @Override
    public String getObjectType() {
        return DiffObjectType.VLAN.getCaption();
    }
}