package voss.discovery.iolib.console;

import voss.discovery.iolib.SupportedDiscoveryType;
import voss.discovery.iolib.simulation.SimulationArchive;
import voss.discovery.iolib.simulation.SimulationEntry;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.model.NodeInfo;

import java.io.IOException;
import java.net.InetAddress;

public class SimulatedConsoleClientFactory implements ConsoleClientFactory {
    private final SimulationArchive archive;

    public SimulatedConsoleClientFactory(SimulationArchive archive) {
        this.archive = archive;
    }

    public ConsoleClient getConsoleClient(NodeInfo nodeinfo, SnmpAccess snmp,
                                          InetAddress inetAddress, SupportedDiscoveryType type) throws IOException {
        SimulationEntry entry = this.archive.getSimulationEntry(nodeinfo);
        if (entry == null) {
            throw new IllegalArgumentException("target not found in archive: "
                    + archive.getSimulationArchiveFile().getCanonicalPath());
        }
        SimulatedConsoleClient client = new SimulatedConsoleClient(entry);
        return client;
    }

}