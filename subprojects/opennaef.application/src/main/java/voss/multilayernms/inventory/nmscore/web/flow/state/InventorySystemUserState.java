package voss.multilayernms.inventory.nmscore.web.flow.state;

import naef.dto.SystemUserDto;
import net.phalanx.portedit.core.model.SystemUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.builder.SystemUserCommandBuilder;
import voss.core.server.database.ATTR;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.Util;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;
import voss.multilayernms.inventory.renderer.SystemUserRenderer;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class InventorySystemUserState extends UnificUIViewState {
    static Logger log = LoggerFactory.getLogger(InventorySystemUserState.class);

    public InventorySystemUserState(StateId stateId) {
        super(stateId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            String reqMethod = context.getHttpServletRequest().getMethod();
            String user = context.getUser();

            if (reqMethod.equals("GET")) {
                super.setXmlObject(getAllSystemUser());
            } else if (reqMethod.equals("DELETE")) {
                String mvoId = context.getParameter("mvo-id");
                delete(mvoId, user);
            } else {
                List<SystemUser> models = (List<SystemUser>) Operation.getTargets(context);
                List<CommandBuilder> commandBuilders = new ArrayList<CommandBuilder>();
                for (SystemUser model : models) {
                    commandBuilders.add(update(model, user));
                }
                ShellConnector.getInstance().executes(commandBuilders);
            }

            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e.getLocalizedMessage());
        }
    }

    private SystemUserCommandBuilder update(SystemUser model, String user) throws InventoryException, IOException, ExternalServiceException {
        SystemUserCommandBuilder builder;

        if (model.getId() == null) {
            SystemUserDto removedUser = getRemovedSystemUserDtoByName(model.getName());
            if (removedUser != null) {
                builder = new SystemUserCommandBuilder(removedUser, user);
                builder.setValue(ATTR.DELETE_FLAG, false);
            } else {
                builder = new SystemUserCommandBuilder(model.getName(), user);
            }
            builder.setActive(true);
        } else {
            SystemUserDto dto = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getId(), SystemUserDto.class);
            builder = new SystemUserCommandBuilder(dto, user);
        }

        builder.setCaption(model.getName());
        String hash = Util.getDigest("SHA-256", model.getPassword());
        builder.setPasswordScheme("SHA-256");
        builder.setPasswordHash(hash);
        builder.buildCommand();
        return builder;
    }

    private void delete(String mvoId, String user) throws IOException, InventoryException, ExternalServiceException {
        SystemUserDto dto = MplsNmsInventoryConnector.getInstance().getMvoDto(mvoId, SystemUserDto.class);
        SystemUserCommandBuilder builder = new SystemUserCommandBuilder(dto, user);
        builder.buildDeleteCommand();
        ShellConnector.getInstance().executes(builder);
    }

    private List<SystemUser> getAllSystemUser() {
        List<SystemUser> result = new ArrayList<SystemUser>();
        for (SystemUserDto dto : SystemUserRenderer.getAllAdminActorDto()) {
            SystemUser su = new SystemUser();
            su.setId(DtoUtil.getMvoIdString(dto));
            su.setName(SystemUserRenderer.getName(dto));
            result.add(su);
        }
        return result;
    }

    private SystemUserDto getRemovedSystemUserDtoByName(String name) {
        List<SystemUserDto> removedUsers = SystemUserRenderer.getRemovedSystemUser();
        for (SystemUserDto dto : removedUsers) {
            if (dto.getName().equals(name)) {
                return dto;
            }
        }
        return null;
    }
}