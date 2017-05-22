package voss.discovery.iolib.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.Access;
import voss.discovery.iolib.ProgressMonitor;
import voss.discovery.iolib.simpletelnet.NullMode;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConsoleAccess implements Access {
    private static final Logger log = LoggerFactory.getLogger(ConsoleAccess.class);
    private final ConsoleClient client;
    private ProgressMonitor monitor;
    private final Map<String, String> commandResultCache =
            Collections.synchronizedMap(new HashMap<String, String>());

    public ConsoleAccess(ConsoleClient client) {
        this.client = client;
        this.monitor = null;
    }

    public void connect() throws IOException, ConsoleException, AbortedException {
        assert this.monitor != null;
        if (!this.monitor.isRunning()) {
            throw new AbortedException();
        }
        if (isConnected()) {
            return;
        }
        client.login();
        commandResultCache.clear();
    }

    public boolean isConnected() throws AbortedException {
        assert this.monitor != null;
        if (!this.monitor.isRunning()) {
            throw new AbortedException();
        }
        return client.isConnected();
    }

    public void close() throws IOException, ConsoleException {
        if (this.client.isConnected()) {
            client.logout();
        }
        log.debug("console-access closed.");
        commandResultCache.clear();
    }

    public void breakConnection() throws IOException, ConsoleException {
        if (this.client.isConnected()) {
            client.breakConnection();
        }
        log.debug("console-access closed(break).");
        commandResultCache.clear();
    }

    public String getResponse(ConsoleCommand command) throws IOException, ConsoleException, AbortedException {
        assert this.monitor != null;
        if (!this.monitor.isRunning()) {
            throw new AbortedException();
        }
        if (command.getMode() != null && !(command.getMode() instanceof NullMode)) {
            client.changeMode(command.getMode().getModeName());
        }
        String result = commandResultCache.get(command.getCommand());
        if (result == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            result = client.execute(command.getCommand());
            commandResultCache.put(command.getCommand(), result);
        }
        log.trace("---->" + command.getCommand() + "\r\n====>\r\n" + result + "====");
        return result;
    }

    public String getDirectResponse(ConsoleCommand command) throws IOException, ConsoleException, AbortedException {
        assert this.monitor != null;
        if (!this.monitor.isRunning()) {
            throw new AbortedException();
        }
        if (command.getMode() != null && !(command.getMode() instanceof NullMode)) {
            client.changeMode(command.getMode().getModeName());
        }
        String result = client.execute(command.getCommand());
        return result;
    }

    public Map<String, String> getCachedCommandResult() {
        Map<String, String> result = new HashMap<String, String>();
        result.putAll(this.commandResultCache);
        return result;
    }

    public void setMonitor(ProgressMonitor monitor) {
        monitor.addAccess(this);
        this.monitor = monitor;
    }

    public ProgressMonitor getMonitor() {
        return this.monitor;
    }

    public ConsoleClient getConsoleClient() {
        return this.client;
    }
}