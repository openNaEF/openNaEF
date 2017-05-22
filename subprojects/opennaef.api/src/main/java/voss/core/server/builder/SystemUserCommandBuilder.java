package voss.core.server.builder;

import naef.dto.CustomerInfoDto;
import naef.dto.SystemUserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.constant.SystemUserAttribute;
import voss.core.server.database.ATTR;
import voss.core.server.database.CoreConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.MvoDtoList;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class SystemUserCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SystemUserCommandBuilder.class);

    private final SystemUserDto user;
    private final List<CustomerInfoDto> customerInfos = new MvoDtoList<CustomerInfoDto>();
    private final List<CustomerInfoDto> previousCustomerInfos = new MvoDtoList<CustomerInfoDto>();
    private String userName = null;

    public SystemUserCommandBuilder(SystemUserDto user, String editorName) {
        super(SystemUserDto.class, user, editorName);
        setConstraint(SystemUserDto.class);
        if (user == null) {
            throw new IllegalArgumentException();
        }
        this.user = user;
        this.userName = user.getName();
        Set<CustomerInfoDto> customerInfos = this.user.getCustomerInfos();
        if (customerInfos != null) {
            this.customerInfos.addAll(customerInfos);
            this.previousCustomerInfos.addAll(customerInfos);
        }
    }

    public SystemUserCommandBuilder(String userName, String editorName) {
        super(SystemUserDto.class, null, editorName);
        setConstraint(SystemUserDto.class);
        this.user = null;
        this.userName = userName;
        recordChange("UserName", null, userName);
    }

    public void setCaption(String caption) {
        setValue(SystemUserAttribute.AUTH_CAPTION, caption);
    }

    public void setPasswordScheme(String scheme) {
        setValue(SystemUserAttribute.AUTH_PASSWORD_SCHEME, scheme);
    }

    public void setPasswordHash(String hash) {
        setValue(SystemUserAttribute.AUTH_PASSWORD_HASH, hash);
    }

    public void setPasswordExpire(Date expire) {
        if (expire == null) {
            setValue(SystemUserAttribute.AUTH_PASSWORD_EXPIRE, (String) null);
        } else {
            String expireValue = DtoUtil.getMvoDateFormat().format(expire);
            setValue(SystemUserAttribute.AUTH_PASSWORD_EXPIRE, expireValue);
        }
    }

    public void setExternalAuthenticator(String authenticator) {
        setValue(SystemUserAttribute.AUTH_EXTERNAL_AUTHENTICATOR, authenticator);
    }

    public void setActive(boolean value) {
        setValue(SystemUserAttribute.AUTH_ACTIVE, value);
    }

    public void addCustomerInfo(CustomerInfoDto customerInfo) {
        if (customerInfo == null) {
            return;
        } else if (this.previousCustomerInfos.contains(customerInfo)) {
            return;
        }
        this.customerInfos.add(customerInfo);
        recordChange(ATTR.ATTR_SYSTEM_USER_REF_TO_CUSTOMER_INFO, null, customerInfo.getName());
    }

    public void addCustomerInfos(List<CustomerInfoDto> infos) {
        if (infos == null) {
            return;
        }
        for (CustomerInfoDto info : infos) {
            addCustomerInfo(info);
        }
    }

    public void removeCustomerInfo(CustomerInfoDto customerInfo) {
        if (customerInfo == null) {
            return;
        } else if (!this.previousCustomerInfos.contains(customerInfo)) {
            return;
        }
        this.customerInfos.remove(customerInfo);
        recordChange(ATTR.ATTR_SYSTEM_USER_REF_TO_CUSTOMER_INFO, customerInfo.getName(), null);
    }

    public void removeCustomerInfos(List<CustomerInfoDto> infos) {
        if (infos == null) {
            return;
        }
        for (CustomerInfoDto info : infos) {
            removeCustomerInfo(info);
        }
    }

    public void resetCustomerInfos() {
        this.customerInfos.clear();
    }

    public void setNote(String note) {
        setValue(ATTR.NOTE, note);
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, ExternalServiceException {
        if (!hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        log.debug("target userName: " + this.userName);
        checkUserName();
        if (this.user == null) {
            log.debug("not found: create: " + this.userName);
            InventoryBuilder.buildSystemUserCreationCommand(cmd, this.userName);
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes, false);
            updateCustomerInfoRefereces();
        } else {
            log.debug("found: update: " + this.user.getName());
            setValue(ATTR.DELETE_FLAG, (String) null);
            InventoryBuilder.changeContext(this.cmd, this.user);
            InventoryBuilder.buildAttributeUpdateCommand(this.cmd, this.user, this.attributes, false);
            updateCustomerInfoRefereces();
            String oldName = this.user.getName();
            if (!oldName.equals(this.userName)) {
                log.debug("- rename from " + this.user.getName() + " to " + this.userName);
                InventoryBuilder.buildRenameCommands(cmd, this.userName);
            }
        }
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        if (this.user == null) {
            return BuildResult.NO_CHANGES;
        }
        InventoryBuilder.changeContext(this.cmd, this.user);
        InventoryBuilder.buildCollectionAttributeRemoveCommands(cmd, this.user, ATTR.ATTR_SYSTEM_USER_REF_TO_CUSTOMER_INFO);
        InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.DELETE_FLAG, Boolean.TRUE.toString());
        recordChange("Name", "Delete", null);
        return BuildResult.SUCCESS;
    }

    private void updateCustomerInfoRefereces() {
        List<CustomerInfoDto> adds = new MvoDtoList<CustomerInfoDto>();
        adds.addAll(this.customerInfos);
        adds.removeAll(this.previousCustomerInfos);
        List<CustomerInfoDto> removes = new MvoDtoList<CustomerInfoDto>();
        removes.addAll(this.previousCustomerInfos);
        removes.removeAll(this.customerInfos);
        for (CustomerInfoDto add : adds) {
            String id = ATTR.TYPE_CUSTOMER_INFO + ATTR.NAME_DELIMITER_PRIMARY + add.getName();
            InventoryBuilder.buildAttributeAdd(cmd, ATTR.ATTR_SYSTEM_USER_REF_TO_CUSTOMER_INFO, id);
        }
        for (CustomerInfoDto remove : removes) {
            String id = ATTR.TYPE_CUSTOMER_INFO + ATTR.NAME_DELIMITER_PRIMARY + remove.getName();
            InventoryBuilder.buildAttributeRemove(cmd, ATTR.ATTR_SYSTEM_USER_REF_TO_CUSTOMER_INFO, id);
        }
    }

    private void checkUserName() throws IOException, ExternalServiceException {
        List<SystemUserDto> current = CoreConnector.getInstance().getSystemUsers();
        for (SystemUserDto user : current) {
            if (DtoUtil.mvoEquals(user, this.user)) {
                continue;
            }
            if (user.getName().equals(this.userName)) {
                throw new IllegalStateException("User[" + this.userName + "] is already registered.");
            }
        }
    }

    public String getObjectType() {
        return DiffObjectType.SYSTEM_USER.getCaption();
    }
}