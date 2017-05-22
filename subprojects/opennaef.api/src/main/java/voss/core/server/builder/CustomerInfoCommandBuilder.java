package voss.core.server.builder;

import naef.dto.CustomerInfoDto;
import naef.dto.NaefDto;
import naef.dto.SystemUserDto;
import voss.core.server.database.ATTR;
import voss.core.server.database.CoreConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.MvoDtoSet;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class CustomerInfoCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final CustomerInfoDto target;
    private String idType;
    private String id;
    private final MvoDtoSet<NaefDto> addedDtos = new MvoDtoSet<NaefDto>();
    private final Set<String> addedNames = new HashSet<String>();
    private final MvoDtoSet<NaefDto> removedDtos = new MvoDtoSet<NaefDto>();
    private final Set<String> removedNames = new HashSet<String>();

    public CustomerInfoCommandBuilder(CustomerInfoDto target, String editorName) {
        super(CustomerInfoDto.class, target, editorName);
        setConstraint(CustomerInfoDto.class);
        this.target = target;
        if (target != null) {
            initialize();
        }
    }

    public CustomerInfoCommandBuilder(String editorName) {
        super(CustomerInfoDto.class, null, editorName);
        setConstraint(CustomerInfoDto.class);
        this.target = null;
        this.id = null;
        this.idType = null;
    }

    private void initialize() {
        this.id = DtoUtil.getStringOrNull(this.target, ATTR.CUSTOMER_INFO_ID);
        this.idType = DtoUtil.getStringOrNull(this.target, ATTR.CUSTOMER_INFO_ID_TYPE);
    }

    public void setID(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null.");
        } else if (this.id != null) {
            throw new IllegalStateException("id cannot change.");
        }
        this.id = id;
        setValue(ATTR.CUSTOMER_INFO_ID, id);
    }

    public String getID() {
        return this.id;
    }

    public boolean hasID() {
        return this.id != null;
    }

    public void setIDType(String idType) {
        if (idType == null) {
            throw new IllegalArgumentException("idType must not be null.");
        }
        this.idType = idType;
        setValue(ATTR.CUSTOMER_INFO_ID_TYPE, idType);
    }

    public String getIDType() {
        return this.idType;
    }

    public boolean hasIDType() {
        return this.idType != null;
    }

    public String getName() {
        return (this.idType == null ? "" : this.idType + ":") + this.id;
    }

    public void setSystemUser(SystemUserDto user) {
        setValue(ATTR.ATTR_CUSTOMER_INFO_REF_TO_SYSTEM_USER, user.getAbsoluteName());
    }

    public void addTarget(String absoluteName) {
        if (absoluteName == null) {
            return;
        }
        this.addedNames.add(absoluteName);
    }

    public void addTarget(NaefDto target) {
        if (target == null) {
            return;
        }
        this.addedDtos.add(target);
    }

    public void removeTarget(String absoluteName) {
        if (absoluteName == null) {
            return;
        }
        this.removedNames.add(absoluteName);
    }

    public void removeTarget(NaefDto removed) {
        if (removed == null) {
            return;
        }
        this.removedDtos.remove(removed);
    }

    private <T extends NaefDto> Set<String> getAddedTargets(MvoDtoSet<T> current,
                                                            MvoDtoSet<T> addedDtos, Set<String> addedNames) {
        Set<String> addedTargets = new HashSet<String>();
        for (T addedDto : addedDtos) {
            if (current.contains(addedDto)) {
                continue;
            }
            addedTargets.add(addedDto.getAbsoluteName());
            recordChange("references", null, "+" + addedDto.getAbsoluteName());
        }
        for (String addedName : addedNames) {
            for (T dto : current) {
                if (dto.getAbsoluteName().equals(addedName)) {
                    continue;
                }
            }
            addedTargets.add(addedName);
            recordChange("references", null, "+" + addedName);
        }
        return addedTargets;
    }

    private <T extends NaefDto> MvoDtoSet<T> getRemovedTargets(MvoDtoSet<T> current,
                                                               MvoDtoSet<T> removedDtos, Set<String> removedNames) {
        MvoDtoSet<T> removedTargets = new MvoDtoSet<T>();
        for (T removedDto : removedDtos) {
            if (!current.contains(removedDto)) {
                continue;
            }
            removedTargets.add(removedDto);
            recordChange("references", null, "-" + removedDto.getAbsoluteName());
        }
        for (String removedName : removedNames) {
            for (T dto : current) {
                if (dto.getAbsoluteName().equals(removedName)) {
                    removedTargets.add(dto);
                    recordChange("references", null, "-" + removedName);
                    break;
                }
            }
        }
        return removedTargets;
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException, ExternalServiceException {
        CustomerInfoDto info = this.target;
        MvoDtoSet<NaefDto> current = new MvoDtoSet<NaefDto>();
        if (info == null) {
            info = CoreConnector.getInstance().getCustomerInfoByName(getName());
            this.attributes.put(ATTR.DELETE_FLAG, null);
        }
        if (info != null) {
            for (NaefDto dto : info.getReferences()) {
                if (dto == null) {
                    continue;
                }
                current.add(dto);
            }
        }
        Set<String> addedTargets = getAddedTargets(current, this.addedDtos, this.addedNames);
        MvoDtoSet<NaefDto> removedTargets = getRemovedTargets(current, this.removedDtos, this.removedNames);
        if (!hasChange()) {
            return setResult(BuildResult.NO_CHANGES);
        }
        if (info == null) {
            InventoryBuilder.buildCustomerInfoCreationCommands(cmd, id, idType);
        } else {
            InventoryBuilder.changeContext(cmd, info);
        }
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, getAttributes());
        for (NaefDto ref : removedTargets) {
            InventoryBuilder.buildAttributeRemove(cmd, ATTR.CUSTOMER_INFO_REFERENCES, ref.getAbsoluteName());
        }
        for (String addedTarget : addedTargets) {
            InventoryBuilder.buildAttributeAdd(cmd, ATTR.CUSTOMER_INFO_REFERENCES, addedTarget);
        }
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, InventoryException, ExternalServiceException {
        CustomerInfoDto info = this.target;
        if (info == null) {
            info = CoreConnector.getInstance().getCustomerInfoByName(getName());
        }
        if (info == null) {
            throw new IllegalArgumentException("no delete target.");
        }
        InventoryBuilder.changeContext(cmd, info);
        for (NaefDto ref : info.getReferences()) {
            InventoryBuilder.buildAttributeRemove(cmd, ATTR.CUSTOMER_INFO_REFERENCES, ref.getAbsoluteName());
        }
        InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.ATTR_CUSTOMER_INFO_REF_TO_SYSTEM_USER, null);
        InventoryBuilder.buildCustomerInfoDeletionCommands(cmd, info.getName());
        recordChange("customer-info", info.getName(), null);
        return BuildResult.SUCCESS;
    }

    @Override
    public String getObjectType() {
        return "Customer-Info";
    }

}