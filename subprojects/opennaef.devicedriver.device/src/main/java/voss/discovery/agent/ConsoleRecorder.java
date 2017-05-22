package voss.discovery.agent;

import voss.discovery.agent.common.DeviceDiscovery;
import voss.discovery.agent.common.DiscoveryFactory;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.DeviceAccessFactory;
import voss.discovery.iolib.RealDeviceAccessFactory;
import voss.discovery.iolib.simulation.SimulatedDeviceAccessFactory;
import voss.discovery.iolib.simulation.SimulationArchive;
import voss.discovery.iolib.simulation.SimulationArchiveImpl;
import voss.discovery.iolib.simulation.SimulationUtil;
import voss.discovery.utils.NodeInfoUtil;
import voss.model.NodeInfo;
import voss.model.Protocol;
import voss.model.ProtocolPort;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConsoleRecorder {
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

    public static NodeInfo createNodeInfo(String[] parameters) {
        if (parameters.length < 2) {
            throw new IllegalArgumentException();
        }
        return NodeInfoUtil.createNodeInfo(parameters);
    }

    public static void main(String[] args) {
        DeviceRecorder recorder = null;
        long current = 0L;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
            recorder = new DeviceRecorder(sdf.format(new Date()));

            DeviceAccessFactory factory;
            List<NodeInfo> targets = new ArrayList<NodeInfo>();

            if (args.length == 0) {
                System.err.println("Usage (Simulation): ConsoleRecorder [ZIP_FILE_NAME]");
                System.err.println("Usage (Real): ConsoleRecorder [ADDRESS:SNMP_COMMUNITY] ([ADDRESS:SNMP_COMMUNITY] ...)");
                System.exit(-1);
            }
            if (args[0].endsWith("zip")) {
                String filename = args[0];
                File file = new File(filename);
                if (!file.exists()) {
                    System.err.println("file [" + filename + "] not found.");
                    System.exit(1);
                }
                List<String> targetIpAddresses = SimulationUtil.getEntryList(file);
                for (String targetIpAddress : targetIpAddresses) {
                    NodeInfo nodeinfo = createNodeInfo(new String[]{targetIpAddress, "public", "", "", "", ""});
                    if (nodeinfo != null) {
                        targets.add(nodeinfo);
                    }
                }
                SimulationArchive archive = new SimulationArchiveImpl(file);
                factory = new SimulatedDeviceAccessFactory(archive);
            } else {
                factory = new RealDeviceAccessFactory();
                for (String arg : args) {
                    String[] split = arg.split(":");
                    if (split.length >= 2) {
                        NodeInfo nodeinfo = createNodeInfo(split);
                        if (nodeinfo != null) {
                            ProtocolPort pp = new ProtocolPort(Protocol.SNMP_V2C_GETBULK);
                            nodeinfo.addSupportedProtocol(pp);
                            targets.add(nodeinfo);
                            System.out.println("using: " + nodeinfo);
                        }
                    } else {
                        System.err.println("ignored: Illegal Format: " + arg);
                    }
                }
            }

            for (NodeInfo target : targets) {
                current = System.currentTimeMillis();
                System.err.println("**** " + format.format(new Date())
                        + " BEGINNING OF " + target.getFirstIpAddress().getHostAddress());

                DeviceAccess access = factory.getDeviceAccess(target);
                if (access == null) {
                    continue;
                }

                DeviceDiscovery discovery = DiscoveryFactory.getInstance().getDiscovery(access);
                if (discovery == null) {
                    System.err.println("no discovery found.");
                    System.err.println("**** IGNORED " + target.getFirstIpAddress().getHostAddress());
                    continue;
                }
                discovery.record(recorder);

                System.err.println("lapse: " + (System.currentTimeMillis() - current) + "ms");
                System.err.println("**** " + format.format(new Date())
                        + " END OF " + target.getFirstIpAddress().getHostAddress());
            }

        } catch (Exception e) {
            System.err.println("lapse: " + (System.currentTimeMillis() - current) + "ms");
            System.err.println(format.format(new Date()));
            e.printStackTrace();
        } finally {
            try {
                recorder.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("recorder close failed.");
            }
        }
        System.exit(0);
    }
}