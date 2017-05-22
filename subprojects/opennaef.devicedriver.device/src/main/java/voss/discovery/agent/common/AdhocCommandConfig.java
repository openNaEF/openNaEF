package voss.discovery.agent.common;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.config.VossDiscoveryConfiguration;
import voss.discovery.constant.DiscoveryParameterType;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.simpletelnet.GlobalMode;
import voss.discovery.iolib.simpletelnet.ModeChanger;
import voss.discovery.iolib.simpletelnet.NullMode;
import voss.model.Device;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class AdhocCommandConfig {
    private static final Logger log = LoggerFactory.getLogger(AdhocCommandConfig.class);
    private static AdhocCommandConfig adhocCommandConfig = null;
    private static Map<Subject, List<ConsoleCommand>> adhocCommands = new LinkedHashMap<Subject, List<ConsoleCommand>>();

    private AdhocCommandConfig() {
    }

    public synchronized static AdhocCommandConfig getInstance() {
        if (adhocCommandConfig == null) {
            adhocCommandConfig = new AdhocCommandConfig();
        }
        adhocCommandConfig.load();
        return adhocCommandConfig;
    }

    private synchronized void load() {
        HashMap<Subject, List<ConsoleCommand>> map = new LinkedHashMap<Subject, List<ConsoleCommand>>();
        Subject subject = null;
        List<ConsoleCommand> commands = null;
        BufferedReader reader = null;
        String basedir = VossDiscoveryConfiguration.getInstance().getConfigDirectory();
        File file = new File(basedir, DiscoveryParameterType.ADHOC_COMMAND_DEF.toString() + ".txt");
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().length() == 0) {
                    continue;
                } else if (line.startsWith("class=")) {
                    String[] subjects = line.split(",");
                    String className = null;
                    String namePattern = null;
                    for (String subjectString : subjects) {
                        subjectString = subjectString.trim();
                        if (subjectString.startsWith("class=")) {
                            int index = subjectString.indexOf('=');
                            className = subjectString.substring(index + 1);
                        } else if (subjectString.startsWith("node=")) {
                            int index = subjectString.indexOf('=');
                            namePattern = subjectString.substring(index + 1);
                        }
                    }
                    if (className != null) {
                        subject = new Subject(className, namePattern);
                        commands = new ArrayList<ConsoleCommand>();
                        map.put(subject, commands);
                    } else {
                        throw new IllegalArgumentException("illegal format: " + line);
                    }
                } else if (line.startsWith(" ") || line.startsWith("\t")) {
                    assert subject != null;
                    assert commands != null;
                    String[] body = line.split(",");
                    if (body.length < 2) {
                        log.error("illegal format: " + line);
                        throw new IllegalArgumentException();
                    }
                    String modeName = body[0].trim();
                    String commandString = body[1].trim();
                    ModeChanger mode = null;
                    if (modeName.toUpperCase().equals("ENABLE")) {
                        mode = new GlobalMode();
                    } else if (modeName.toUpperCase().equals("NULL")) {
                        mode = new NullMode();
                    } else {
                        throw new IllegalArgumentException("unknown mode: " + line);
                    }
                    ConsoleCommand command = new ConsoleCommand(mode, commandString);
                    commands.add(command);
                    log.debug(subject + ": MODE=" + modeName + ", COMMAND=" + commandString);
                } else {
                    log.warn("unknown line: " + line);
                    continue;
                }
            }
        } catch (IOException e) {
            log.error("failed to read definition file [" + file + "].", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.warn("faile to close.", e);
                }
            }
        }

        adhocCommands = map;
    }

    public synchronized List<ConsoleCommand> getAdhocCommands(DeviceDiscovery discovery, Device device) {
        for (Map.Entry<Subject, List<ConsoleCommand>> entry : adhocCommands.entrySet()) {
            Subject s = entry.getKey();
            if (s.matches(discovery, device)) {
                return entry.getValue();
            }
        }
        return new ArrayList<ConsoleCommand>();
    }

    public static class Subject {
        public final String className;
        public final String namePattern;

        public Subject(String className, String namePattern) {
            assert className != null;
            this.className = className;
            this.namePattern = namePattern;
        }

        public boolean matches(DeviceDiscovery discovery, Device device) {
            assert discovery != null;
            assert device != null;

            if (this.namePattern != null && !device.getDeviceName().matches(this.namePattern)) {
                return false;
            }

            return discovery.getClass().getSimpleName().equals(this.className);
        }

        @Override
        public String toString() {
            return this.className + ":" + this.namePattern;
        }
    }

    public static void main(String[] args) {
        AdhocCommandConfig.getInstance();
    }
}