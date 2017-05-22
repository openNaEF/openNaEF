package voss.discovery.runner.simple;


import voss.discovery.iolib.DeviceAccessFactory;
import voss.discovery.iolib.RealDeviceAccessFactory;
import voss.discovery.iolib.simulation.SimulatedDeviceAccessFactory;
import voss.discovery.iolib.simulation.SimulationArchive;
import voss.discovery.iolib.simulation.SimulationUtil;
import voss.discovery.utils.NodeInfoUtil;
import voss.model.Device;
import voss.model.NodeInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainDriver {
    public final static String VIEWER_SIMPLE = "SIMPLE";
    public final static String VIEWER_BIN = "BIN";

    public final static String ACCESS_LOCAL = "-";

    public static void main(String[] args) {
        try {
            DeviceAccessFactory factory;
            List<NodeInfo> targets = new ArrayList<NodeInfo>();
            boolean saveArchive = false;

            if (args.length < 3) {
                System.err.println("Usage (Simulation): MainDriver [ViewerType] [AccessType] [ZIP_FILE_NAME] ([TARGET], ...)");
                System.err.println("Usage (Real): MainDriver [ViewerType] [AccessType] [ADDRESS:SNMP_COMMUNITY] ([ADDRESS:SNMP_COMMUNITY] ...)");
                System.err.println("AccessType: \"-\": direct access, \"[IP]:[port]\": remote access.");
                System.exit(-1);
            }
            String viewerType = args[0];
            String accessType = args[1];

            if (args[2].endsWith("zip")) {
                String filename = args[2];
                File file = new File(filename);
                if (!file.exists()) {
                    System.err.println("file [" + filename + "] not found.");
                    System.exit(1);
                }
                if (args.length > 3) {
                    for (int i = 3; i < args.length; i++) {
                        if (args[i].startsWith("#")) {
                            continue;
                        }
                        targets.add(NodeInfoUtil.createNodeInfo(args[i].split(":")));
                    }
                } else {
                    List<String> targetIpAddresses = SimulationUtil.getEntryList(file);
                    for (String targetIpAddress : targetIpAddresses) {
                        NodeInfo nodeinfo = NodeInfoUtil.createNodeInfo(new String[]{targetIpAddress, "public", "", "", "", ""});
                        if (nodeinfo != null) {
                            targets.add(nodeinfo);
                        }
                    }
                }
                SimulationArchive archive = SimulationUtil.selectSimulationArchive(file);
                factory = new SimulatedDeviceAccessFactory(archive);
            } else {
                saveArchive = true;
                factory = new RealDeviceAccessFactory();
                if (!accessType.equals(ACCESS_LOCAL)) {
                    String[] addrPort = accessType.split(":");
                    String addr = addrPort[0];
                    int port = Integer.parseInt(addrPort[1]);
                    ((RealDeviceAccessFactory) factory).setRemoteSnmpAccessServer(addr, port);
                }
                if (args[2].endsWith(".txt")) {
                    File file = new File(args[2]);
                    if (file.exists()) {
                        extractNodeInfo(targets, file);
                    }
                } else {
                    for (int i = 2; i < args.length; i++) {
                        String line = args[i];
                        extractNodeInfo(targets, line);
                    }
                }
            }

            CommandLineAgent agent = new CommandLineAgent(factory, saveArchive);

            for (NodeInfo target : targets) {
                if (target == null) {
                    continue;
                }
                System.out.println("Target: " + target.getFirstIpAddress().getHostAddress());
                System.out.println(target);
                try {
                    Device result = agent.getDevice(target);
                    SwitchView viewer = null;
                    if (viewerType.equals(VIEWER_SIMPLE)) {
                        viewer = new SimpleSwitchView(result);
                    } else if (viewerType.equals(VIEWER_BIN)) {
                        viewer = new ObjectSaveView(result);
                    } else {
                        System.err.println("unknown viewer type: " + viewerType);
                    }

                    if (viewer != null) {
                        viewer.view();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.exit(0);
        } catch (Throwable ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    private static void extractNodeInfo(List<NodeInfo> targets, File file) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                extractNodeInfo(targets, line);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private static void extractNodeInfo(List<NodeInfo> targets, String line) {
        String[] split = line.split(":");
        if (split.length >= 2) {
            NodeInfo nodeinfo = NodeInfoUtil.createNodeInfo(split);
            if (nodeinfo != null) {
                targets.add(nodeinfo);
                System.out.println("using: " + line);
            }
        } else {
            System.err.println("ignored: Illegal Format: " + line);
        }
    }

}