package voss.discovery.agent.common;

import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleException;

import java.io.IOException;

public interface DiscoveryLogicUnit {
    void execute(DeviceAccess access) throws ConsoleException, IOException, AbortedException;

    boolean isConsoleNeeded();
}