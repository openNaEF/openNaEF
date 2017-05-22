package voss.discovery.iolib.netconf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.simulation.SimulationEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SimulatedNetConfClient implements NetConfClient {
    private final static Logger log = LoggerFactory.getLogger(SimulatedNetConfClient.class);
    private final SimulationEntry simulationEntry;

    public SimulatedNetConfClient(SimulationEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("entry is null.");
        }
        this.simulationEntry = entry;
    }

    @Override
    public void connect() {
    }

    public boolean isConnected() {
        return true;
    }

    @Override
    public void disconnect() {
    }

    @Override
    public String get(String command) throws IOException {
        log.trace("execute():command=" + command);
        StringBuffer sb = new StringBuffer();
        InputStream is = simulationEntry.getNetConfCommandResponse(command);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\r\n");
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        log.trace("execute():response=" + sb.toString());
        return sb.toString();
    }

    @Override
    public void put(String command) {
    }
}