package voss.multilayernms.inventory.renderer;

import naef.dto.SystemUserDto;
import naef.ui.NaefDtoFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.constant.SystemUserAttribute;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.util.DtoUtil;
import voss.nms.inventory.database.InventoryConnector;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class SystemUserRenderer extends GenericRenderer {
    @SuppressWarnings("unused")
    private final static Logger log = LoggerFactory.getLogger(SystemUserRenderer.class);
    private final SystemUserDto user;

    public SystemUserRenderer(SystemUserDto dto) {
        this.user = dto;
    }

    public String getName() {
        return getName(this.user);
    }

    public static String getName(SystemUserDto dto) {
        if (dto == null) {
            return null;
        }
        return dto.getName();
    }

    public boolean isActive() {
        return isActive(this.user);
    }

    public static boolean isActive(SystemUserDto dto) {
        return DtoUtil.getBoolean(dto, SystemUserAttribute.AUTH_ACTIVE);
    }

    public String getStatus() {
        return getStatus(this.user);
    }

    public static String getStatus(SystemUserDto dto) {
        boolean b = isActive(dto);
        if (b) {
            return "O";
        } else {
            return "x";
        }
    }

    public String getCaption() {
        return getCaption(this.user);
    }

    public static String getCaption(SystemUserDto dto) {
        return DtoUtil.getStringOrNull(dto, SystemUserAttribute.AUTH_CAPTION);
    }

    public String getPasswordScheme() {
        return getPasswordScheme(this.user);
    }

    public static String getPasswordScheme(SystemUserDto dto) {
        return DtoUtil.getStringOrNull(dto, SystemUserAttribute.AUTH_PASSWORD_SCHEME);
    }

    public Date getPasswordExpire() {
        return getPasswordExpire(this.user);
    }

    public static Date getPasswordExpire(SystemUserDto dto) {
        return DtoUtil.getDate(dto, SystemUserAttribute.AUTH_PASSWORD_EXPIRE);
    }

    public String getPasswordExpireString() {
        return getPasswordExpireString(this.user);
    }

    public static String getPasswordExpireString(SystemUserDto dto) {
        return getPasswordExpireString(dto, "yyyy/MM/dd HH:mm");
    }

    public String getPasswordExpireString(String pattern) {
        return getPasswordExpireString(this.user, pattern);
    }

    public static String getPasswordExpireString(SystemUserDto dto, String pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException();
        }
        Date d = getPasswordExpire(dto);
        if (d == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(d);
    }

    public String getExternalAuthenticator() {
        return getExternalAuthenticator(this.user);
    }

    public static String getExternalAuthenticator(SystemUserDto dto) {
        return DtoUtil.getStringOrNull(dto, SystemUserAttribute.AUTH_EXTERNAL_AUTHENTICATOR);
    }

    public String getPasswordHash() {
        return getPasswordHash(this.user);
    }

    public static String getPasswordHash(SystemUserDto dto) {
        return DtoUtil.getStringOrNull(dto, SystemUserAttribute.AUTH_PASSWORD_HASH);
    }

    public String getNote() {
        return getNote(this.user);
    }


    public static SystemUserDto getSystemUserDto(String id) {
        Set<SystemUserDto> systemUsers;
        SystemUserDto systemUser = null;
        try {
            InventoryConnector conn = InventoryConnector.getInstance();
            NaefDtoFacade facade;
            facade = conn.getDtoFacade();
            systemUsers = facade.getRootObjects(SystemUserDto.class);
            for (SystemUserDto su : systemUsers) {
                if (su.getName().equals(id)) {
                    systemUser = su;
                }
            }
        } catch (IOException e) {
            log.debug("IOException", e);
        } catch (ExternalServiceException e) {
            log.debug("ExternalServiceException", e);
        } catch (AuthenticationException e) {
            log.debug("AuthenticationException", e);
        }
        return systemUser;
    }

    public static List<SystemUserDto> getAllSystemUser() {
        Set<SystemUserDto> systemUsers;
        List<SystemUserDto> allSystemUsers = new ArrayList<SystemUserDto>();
        try {
            InventoryConnector conn = InventoryConnector.getInstance();
            NaefDtoFacade facade;
            facade = conn.getDtoFacade();
            systemUsers = facade.getRootObjects(SystemUserDto.class);
            for (SystemUserDto systemUser : systemUsers) {
                if (!DtoUtil.getBoolean(systemUser, ATTR.DELETE_FLAG)
                        && DtoUtil.getStringOrNull(systemUser, "active") != null) {
                    allSystemUsers.add(systemUser);
                }
            }
        } catch (IOException e) {
            log.debug("IOException", e);
        } catch (ExternalServiceException e) {
            log.debug("ExternalServiceException", e);
        } catch (AuthenticationException e) {
            log.debug("AuthenticationException", e);
        }
        return allSystemUsers;
    }

    public static List<String> getAllSystemUserID() {
        List<String> systemUsers = new ArrayList<String>();
        for (SystemUserDto systemUser : getAllSystemUser()) {
            systemUsers.add(systemUser.getName());
        }
        return systemUsers;
    }

    public static boolean isAdminActor(String id) {
        SystemUserDto su = getSystemUserDto(id);
        if (su.getCustomerInfos() == null) {
            return true;
        } else if (su.getCustomerInfos().size() == 0) {
            return true;
        }
        return false;
    }

    public static List<String> getAllAdminActorID() {
        List<String> allAdminActor = new ArrayList<String>();
        for (SystemUserDto systemUser : getAllSystemUser()) {
            if (isAdminActor(systemUser.getName())) {
                allAdminActor.add(systemUser.getName());
            }
        }
        return allAdminActor;
    }

    public static List<SystemUserDto> getAllAdminActorDto() {
        List<SystemUserDto> allAdminActor = new ArrayList<SystemUserDto>();
        for (SystemUserDto systemUser : getAllSystemUser()) {
            if (isAdminActor(systemUser.getName())) {
                allAdminActor.add(systemUser);
            }
        }
        return allAdminActor;
    }

    public static List<SystemUserDto> getRemovedSystemUser() {
        Set<SystemUserDto> systemUsers;
        List<SystemUserDto> allSystemUsers = new ArrayList<SystemUserDto>();
        try {
            InventoryConnector conn = InventoryConnector.getInstance();
            NaefDtoFacade facade;
            facade = conn.getDtoFacade();
            systemUsers = facade.getRootObjects(SystemUserDto.class);
            for (SystemUserDto systemUser : systemUsers) {
                if (DtoUtil.getBoolean(systemUser, ATTR.DELETE_FLAG)) {
                    allSystemUsers.add(systemUser);
                }
            }
        } catch (IOException e) {
            log.debug("IOException", e);
        } catch (ExternalServiceException e) {
            log.debug("ExternalServiceException", e);
        } catch (AuthenticationException e) {
            log.debug("AuthenticationException", e);
        }
        return allSystemUsers;
    }
}