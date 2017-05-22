package voss.discovery.runner.console;

import voss.discovery.agent.common.DeviceDiscovery;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.DeviceAccessFactory;
import voss.discovery.iolib.RealDeviceAccessFactory;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.simpletelnet.*;
import voss.discovery.utils.NodeInfoUtil;
import voss.model.NodeInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainDriver {

    public static void main(String[] args) {
        try {
            DeviceAccessFactory factory;
            if (args.length < 2) {
                System.err.println("Usage: MainDriver ADDRESS:SNMP_COMMUNITY:user:pass:admin:pass:snmp_type:console_type:console_port:options [mode]");
                System.err.println("mode is optional and telnet server dependent");
                System.exit(-1);
            }
            factory = new RealDeviceAccessFactory();
            NodeInfo target = extraceNodeInfo(args[0]);
            String mode = null;
            if (args.length > 1) {
                mode = args[1];
                mode = mode.trim().toLowerCase();
                if (mode.isEmpty()) {
                    mode = null;
                }
            }
            CommandLineAgent agent = new CommandLineAgent(factory, false);
            if (target == null) {
                System.exit(0);
            }
            System.out.println("Target: " + target.getFirstIpAddress().getHostAddress());
            DeviceAccess access = null;
            BufferedReader reader = null;
            try {
                DeviceDiscovery discovery = agent.getDeviceDiscovery(target);
                access = discovery.getDeviceAccess();
                ConsoleAccess console = access.getConsoleAccess();
                if (console == null) {
                    System.out.println("No console found. exit.");
                    System.exit(0);
                }
                console.connect();
                ModeChanger m = null;
                if (mode == null) {
                    m = new NullMode();
                } else if ("enable".equals(mode)) {
                    m = new EnableMode();
                } else if ("global".equals(mode)) {
                    m = new GlobalMode();
                } else if ("configure".equals(mode)) {
                    m = new ConfigMode();
                } else {
                    m = new NullMode();
                }
                System.out.println("using mode: " + m.getModeName());
                reader = new BufferedReader(new InputStreamReader(System.in));
                String line = null;
                System.out.print(">> ");
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) {
                        break;
                    }
                    line = line.trim();
                    if (line.equals("exit")) {
                        break;
                    }
                    ConsoleCommand cmd = new ConsoleCommand(m, line);
                    String res = console.getResponse(cmd);
                    System.out.println("Reponse ----");
                    System.out.println(res);
                    System.out.print("----\r\n>> ");
                }
                System.out.println("exit.");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (access != null) {
                    access.close();
                }
            }
            System.exit(0);
        } catch (Throwable ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    private static NodeInfo extraceNodeInfo(String line) {
        String[] split = line.split(":");
        if (split.length >= 2) {
            NodeInfo nodeinfo = NodeInfoUtil.createNodeInfo(split);
            if (nodeinfo != null) {
                System.out.println("using: " + line);
                return nodeinfo;
            }
        } else {
            System.err.println("ignored: Illegal Format: " + line);
        }
        return null;
    }

}