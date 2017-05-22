package voss.discovery.iolib.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.simulation.SimulationEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SimulatedConsoleClient implements ConsoleClient {
    private final static Logger log = LoggerFactory.getLogger(SimulatedConsoleClient.class);
    private final SimulationEntry simulationEntry;

    public SimulatedConsoleClient(SimulationEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("entry is null.");
        }
        this.simulationEntry = entry;
    }

    @Override
    public void login() {
    }

    public boolean isConnected() {
        return true;
    }

    @Override
    public void logout() {
    }

    @Override
    public void breakConnection() {
    }

    @Override
    public String execute(String command) throws IOException {
        log.trace("execute():command=" + command);
        StringBuffer sb = new StringBuffer();
        InputStream is = simulationEntry.getConsoleCommandResponse(command);
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
    public String changeMode(String mode) throws IOException {
        return null;
    }

    @Override
    public String getPrompt() {
        return "#";
    }

}