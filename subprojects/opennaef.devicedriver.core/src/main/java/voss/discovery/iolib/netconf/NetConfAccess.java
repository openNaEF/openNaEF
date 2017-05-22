package voss.discovery.iolib.netconf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.Access;
import voss.discovery.iolib.ProgressMonitor;
import voss.discovery.iolib.console.ConsoleException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NetConfAccess implements Access {
    private static final Logger log = LoggerFactory.getLogger(NetConfAccess.class);
    private final NetConfClient client;
    private ProgressMonitor monitor;
    private final Map<String, String> commandResultCache =
            Collections.synchronizedMap(new HashMap<String, String>());

    public NetConfAccess(NetConfClient client) {
        this.client = client;
        this.monitor = null;
    }

    public void connect() throws IOException, NetConfException, AbortedException {
        assert this.monitor != null;
        if (!this.monitor.isRunning()) {
            throw new AbortedException();
        }
        if (isConnected()) {
            return;
        }
        client.connect();
        commandResultCache.clear();
    }

    public boolean isConnected() throws AbortedException {
        assert this.monitor != null;
        if (!this.monitor.isRunning()) {
            throw new AbortedException();
        }
        return client.isConnected();
    }

    public void close() throws IOException {
        if (this.client.isConnected()) {
            client.disconnect();
        }
        log.debug("console-access closed.");
        commandResultCache.clear();
    }

    public String getResponse(String command) throws IOException, NetConfException, AbortedException {
        assert this.monitor != null;
        if (!this.monitor.isRunning()) {
            throw new AbortedException();
        }
        String result = commandResultCache.get(command);
        if (result == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            result = client.get(command);
            commandResultCache.put(command, result);
        }
        log.trace("---->" + command + "\r\n====>\r\n" + result + "====");
        return result;
    }

    public String getDirectResponse(String command) throws IOException, NetConfException, AbortedException {
        assert this.monitor != null;
        if (!this.monitor.isRunning()) {
            throw new AbortedException();
        }
        String result = client.get(command);
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

    public NetConfClient getConsoleClient() {
        return this.client;
    }
}