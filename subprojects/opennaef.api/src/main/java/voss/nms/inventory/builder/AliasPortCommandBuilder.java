package voss.nms.inventory.builder;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import tef.skelton.dto.EntityDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.NameUtil;

import java.util.Collection;
import java.util.Date;

public class AliasPortCommandBuilder extends PortCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final NodeDto vm;
    private final NodeDto host;
    private final PortDto alias;
    private PortDto aliasSource;
    private String aliasSourceNodeName;
    private String aliasSourceIfName;

    public AliasPortCommandBuilder(NodeDto vm, String editorName) {
        super(PortDto.class, vm, null, editorName);
        setConstraint(IpIfDto.class);
        if (vm == null) {
            throw new IllegalArgumentException("vm is null.");
        } else if (vm.getVirtualizationHostNode() == null) {
            throw new IllegalArgumentException("VM has no HyperVisor: " + DtoUtil.toDebugString(vm));
        }
        this.vm = vm;
        this.host = vm.getVirtualizationHostNode();
        this.alias = null;
        this.aliasSource = null;
    }

    public AliasPortCommandBuilder(PortDto alias, String editorName) {
        super(alias.getClass(), alias.getNode(), alias, editorName);
        setConstraint(alias.getClass());
        if (alias.getAliasSource() == null) {
            throw new IllegalArgumentException("alias-source is null.");
        } else if (alias.getNode() == null) {
            throw new IllegalArgumentException("alias is already deleted: " + DtoUtil.toDebugString(alias));
        } else if (alias.getNode().getVirtualizationHostNode() == null) {
            throw new IllegalArgumentException("alias' owner node is not virtual-machine: " + DtoUtil.toDebugString(alias));
        }
        this.vm = alias.getNode();
        this.host = this.vm.getVirtualizationHostNode();
        this.alias = alias;
        this.aliasSource = alias.getAliasSource();
        if (alias != null) {
            initialize();
        }
    }

    private void initialize() {
        this.cmd.addVersionCheckTarget(alias);
    }

    public void setAliasSourcePort(PortDto aliasSource) {
        if (aliasSource == null) {
            throw new IllegalArgumentException("alias-source is null.");
        } else if (aliasSource.getOwner() == null) {
            throw new IllegalArgumentException("alias-source is deleted. " + DtoUtil.toDebugString(aliasSource));
        } else if (DtoUtil.mvoEquals(this.aliasSource, aliasSource)) {
            return;
        } else if (!DtoUtil.mvoEquals(this.host, aliasSource.getNode())) {
            throw new IllegalStateException("alias-source node mismatch: " +
                    "aliasSource=" + DtoUtil.toDebugString(aliasSource) +
                    ", HyperVisor=" + DtoUtil.toDebugString(this.host));
        }
        recordChange("Alias Source", NameUtil.getNodeIfName(this.aliasSource), NameUtil.getNodeIfName(aliasSource));
        this.aliasSource = aliasSource;
    }

    public void setAliasSourceAbsoluteName(String nodeName, String ifName) {
        if (nodeName == null || ifName == null) {
            throw new IllegalArgumentException();
        }
        this.aliasSourceNodeName = nodeName;
        this.aliasSourceIfName = ifName;
    }

    public String getIfName() {
        if (this.alias != null) {
            return this.alias.getIfname();
        } else if (this.aliasSource != null) {
            return this.aliasSource.getIfname();
        } else if (this.aliasSourceIfName != null) {
            return this.aliasSourceIfName;
        } else {
            return null;
        }
    }

    @Override
    public String getPortContext() {
        if (this.alias != null) {
            return this.alias.getAbsoluteName();
        } else if (this.aliasSource != null) {
            return AbsoluteNameFactory.getIfNameAbsoluteName(this.vm.getName(), this.aliasSource.getIfname());
        } else if (this.aliasSourceNodeName != null && this.aliasSourceIfName != null) {
            return AbsoluteNameFactory.getIfNameAbsoluteName(this.vm.getName(), this.aliasSourceIfName);
        } else {
            throw new IllegalStateException("no alias and alias-source.");
        }
    }

    @Override
    public String getNodeContext() {
        return AbsoluteNameFactory.getNodeAbsoluteName2(this.vm.getName());
    }

    @Override
    public BuildResult buildPortCommands() {
        if (this.alias != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        resolveAliasSource();
        if (this.aliasSource == null) {
            throw new IllegalStateException("no alias-source.");
        }
        checkAliasSource();
        String ifName = this.aliasSource.getIfname();
        if (alias == null) {
            if (!DtoUtil.getBoolean(this.aliasSource, ATTR.ATTR_ALIAS_SOURCEABLE)) {
                InventoryBuilder.changeContext(cmd, this.aliasSource);
                InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.ATTR_ALIAS_SOURCEABLE, Boolean.TRUE.toString());
            }
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
            InventoryBuilder.changeContext(cmd, vm);
            SimpleNodeBuilder.buildPortCreationCommands(cmd, this.aliasSource.getObjectTypeName(), ifName);
            InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.ATTR_ALIAS_SOURCE, this.aliasSource.getAbsoluteName());
            InventoryBuilder.buildAttributeCopy(cmd, MPLSNMS_ATTR.PORT_TYPE, this.aliasSource);
            InventoryBuilder.buildAttributeCopy(cmd, MPLSNMS_ATTR.IFNAME, this.aliasSource);
            InventoryBuilder.buildAttributeCopy(cmd, MPLSNMS_ATTR.IFINDEX, this.aliasSource);
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
            InventoryBuilder.buildSetAttributeUpdateCommand(cmd, listAttributes);
            this.cmd.addLastEditCommands();
        } else {
            InventoryBuilder.changeContext(cmd, this.alias);
            this.cmd.addLastEditCommands();
            InventoryBuilder.buildAttributeUpdateCommand(cmd, this.alias, attributes);
            InventoryBuilder.buildSetAttributeUpdateCommand(cmd, listAttributes);
            String currentIfName = DtoUtil.getStringOrNull(this.alias, MPLSNMS_ATTR.IFNAME);
            if (!currentIfName.equals(ifName)) {
                InventoryBuilder.buildRenameCommands(cmd, ifName);
            }
        }
        return BuildResult.SUCCESS;
    }

    private void resolveAliasSource() {
        if (this.aliasSource != null) {
            return;
        } else if (this.aliasSourceNodeName == null || this.aliasSourceIfName == null) {
            throw new IllegalStateException("no alias-source information.");
        }
        String absName = AbsoluteNameFactory.getIfNameAbsoluteName(this.aliasSourceNodeName, this.aliasSourceIfName);
        try {
            EntityDto dto = InventoryConnector.getInstance().getMvoDtoByAbsoluteName(absName);
            if (!PortDto.class.isInstance(dto)) {
                throw new IllegalStateException("unexpected dto: " + DtoUtil.toDebugString(dto) + " absoluteName=" + absName);
            }
            this.aliasSource = PortDto.class.cast(dto);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void checkAliasSource() {
        this.aliasSource.renew();
        Collection<PortDto> aliases = this.aliasSource.getAliases();
        if (this.aliasSource.getOwner() == null) {
            throw new IllegalStateException("alias-source is deleted: " + DtoUtil.toDebugString(this.aliasSource));
        } else if (aliases.size() == 1) {
            PortDto _alias = aliases.iterator().next();
            if (!DtoUtil.mvoEquals(this.alias, _alias)) {
                throw new IllegalStateException("alias-source has aliases already: "
                        + DtoUtil.toDebugString(this.aliasSource));
            }
        } else if (aliases.size() > 1) {
            throw new IllegalStateException("alias-source has aliases already: "
                    + DtoUtil.toDebugString(this.aliasSource));
        }
    }

    @Override
    protected void buildPortDeleteCommand() {
        InventoryBuilder.changeContext(cmd, this.alias);
        InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.ATTR_ALIAS_SOURCE, null);
        super.buildPortDeleteCommand();
    }

    public PortDto getAlias() {
        return this.alias;
    }

    public String getObjectType() {
        return DiffObjectType.ALIAS.getCaption();
    }

}