package voss.discovery.iolib.snmp;

import net.snmp.SnmpAgentEmulator;
import net.snmp.SnmpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.simulation.SimulationArchive;
import voss.discovery.iolib.simulation.SimulationEntry;
import voss.model.NodeInfo;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.zip.ZipFile;

public class SimulatedSnmpClientFactory implements SnmpClientFactory {
    private final static Logger log = LoggerFactory.getLogger(SimulatedSnmpClientFactory.class);
    private SimulationArchive archive;

    public SimulatedSnmpClientFactory(SimulationArchive archive) {
        this.archive = archive;
    }

    public SnmpClient createSnmpClient(InetAddress targetInetAddress, NodeInfo nodeinfo) throws IOException {
        List<InetAddress> ipAddresses = nodeinfo.listIpAddress();
        if (ipAddresses.size() == 0) {
            throw new IllegalArgumentException();
        }
        if (targetInetAddress == null) {
            for (InetAddress inetAddress : ipAddresses) {
                String target = archive.getTargetBy(inetAddress);
                if (archive.exists(target)) {
                    targetInetAddress = inetAddress;
                    break;
                }
            }
        }
        SimulationEntry entry = archive.getSimulationEntry(nodeinfo);
        return createSnmpClient(targetInetAddress, entry);
    }

    public SnmpClient createSnmpClient(
            final InetAddress nodeAddress,
            SimulationEntry entry)
            throws IOException {
        if (nodeAddress == null) {
            throw new IOException("nodeAddress == null");
        }
        if (archive == null) {
            throw new IOException("archive is null");
        }
        if (!archive.exists(nodeAddress.getHostAddress())) {
            throw new IOException("no simulationEntry found: " + nodeAddress.getHostAddress());
        }
        DatagramSocket socket = new DatagramSocket();
        final SnmpAgentEmulator agentEmulator =
                new SnmpAgentEmulator(
                        new ZipFile(archive.getSimulationArchiveFile()),
                        entry.getMibZipEntryName(),
                        socket);
        agentEmulator.start();
        return new SnmpClient(
                InetAddress.getLocalHost(),
                socket.getLocalPort(),
                "public".getBytes(),
                new SnmpClientLogger()) {
            public void close() {
                try {
                    agentEmulator.stop();
                } catch (Exception e) {
                    log.info(e.toString());
                }
            }

            public InetSocketAddress getSnmpAgentAddress() {
                return new InetSocketAddress(nodeAddress, 0);
            }
        };
    }

    public void setTimeout(int sec) {
    }

    public void setRetry(int retryCount) {
    }
}