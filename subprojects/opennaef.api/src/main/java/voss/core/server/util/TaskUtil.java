package voss.core.server.util;

import naef.dto.NaefDto;
import naef.ui.NaefDtoFacade;
import tef.skelton.dto.EntityDto;
import voss.core.server.database.CoreConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;
import java.rmi.RemoteException;

public class TaskUtil {

    public static boolean hasTask(NaefDto dto) throws RemoteException, ExternalServiceException, IOException, InventoryException {
        if (dto == null) {
            return false;
        }
        NaefDtoFacade facade = CoreConnector.getInstance().getDtoFacade();
        String taskName = facade.getTaskName(dto);
        return taskName != null;
    }

    public static String getTaskName(NaefDto dto) throws RemoteException, IOException, InventoryException, ExternalServiceException {
        if (dto == null) {
            return null;
        }
        NaefDtoFacade facade = CoreConnector.getInstance().getDtoFacade();
        String taskName = facade.getTaskName(dto);
        return taskName;
    }

    @SuppressWarnings("unchecked")
    public static <T extends NaefDto> T getTaskDto(T dto) throws RemoteException, ExternalServiceException, IOException, InventoryException {
        if (dto == null) {
            return null;
        }
        NaefDtoFacade facade = CoreConnector.getInstance().getDtoFacade();
        EntityDto taskDto = facade.buildTaskSynthesizedDto(dto);
        if (taskDto instanceof NaefDto) {
            return (T) taskDto;
        }
        throw new IllegalStateException("unexpected type: " + dto.getAbsoluteName());
    }
}