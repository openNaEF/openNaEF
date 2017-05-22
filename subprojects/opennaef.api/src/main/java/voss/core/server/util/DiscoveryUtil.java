package voss.core.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.DeviceAccessFactory;
import voss.discovery.iolib.RealDeviceAccessFactory;
import voss.discovery.iolib.simulation.SimulatedDeviceAccessFactory;
import voss.discovery.iolib.simulation.SimulationArchive;
import voss.discovery.iolib.simulation.SimulationUtil;
import voss.model.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class DiscoveryUtil {
    private static final Logger log = LoggerFactory.getLogger(DiscoveryUtil.class);
    private static final String SIMULATION_FILE = "./simulation.txt";

    public static DeviceAccessFactory createDeviceAccessFactory() throws IOException {
        DeviceAccessFactory factory;
        String simulationDef = System.getProperty("voss.simulation.file", SIMULATION_FILE);
        File file = new File(simulationDef);
        if (!file.exists()) {
            factory = new RealDeviceAccessFactory();
            return factory;
        }
        List<String> lines = FileUtils.loadLines(file);
        if (lines.size() == 0) {
            throw new IllegalStateException("no simulation archive info.");
        }
        log.debug("simulation archive: " + lines.get(0));
        File zip = new File(lines.get(0));
        if (!zip.exists()) {
            throw new IllegalArgumentException("zip file not exist:" + zip.getAbsolutePath());
        }
        SimulationArchive archive = SimulationUtil.selectSimulationArchive(zip);
        factory = new SimulatedDeviceAccessFactory(archive);
        return factory;
    }

    public static boolean isAggregatedPort(Port port) {
        if (port == null) {
            return false;
        }
        if (EthernetPortsAggregator.class.isInstance(port)) {
            return true;
        } else if (EthernetProtectionPort.class.isInstance(port)) {
            return true;
        } else if (AtmAPSImpl.class.isInstance(port)) {
            return true;
        } else if (POSAPSImpl.class.isInstance(port)) {
            return true;
        }
        return false;
    }
}