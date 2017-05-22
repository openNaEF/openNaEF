package voss.core.server.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class CommandUtil {

    public static void logCommands(List<String> commands) {
        Logger log = LoggerFactory.getLogger(CommandUtil.class);
        log.info("commands ----");
        String formatter = "%0" + String.valueOf(commands.size()).length() + "d";
        int line = 0;
        for (String command : commands) {
            line++;
            if (command == null) {
                command = "";
            } else if (command.contains("パスワード")) {
                command = escape(command, "パスワード");
            } else if (command.toLowerCase().contains("password")) {
                command = escape(command, "password");
            }
            log.info("[" + String.format(formatter, Integer.valueOf(line)) + "] " + command);
        }
        log.info("----");
    }

    public static void logCommands(CommandBuilder builder) throws IOException {
        showChanges(builder);
        ShellCommands cmd = builder.getCommand();
        logCommands(cmd);
    }

    public static void showChanges(CommandBuilder builder) {
        Logger log = LoggerFactory.getLogger(CommandUtil.class);
        log.info("changed = " + builder.hasChange());
        if (builder.hasChange()) {
            log.info("- attributes=" + builder.getDiffContent());
        }
    }

    public static void logCommands(Commands cmd) throws IOException {
        Logger log = LoggerFactory.getLogger(CommandUtil.class);
        log.info("commands ----");
        int i = 0;
        List<String> commands = cmd.getCommands();
        int length = String.valueOf(commands.size()).length();
        String format = "%0" + length + "d";
        for (String command : commands) {
            i++;
            if (command == null) {
                log.info("");
            }
            if (command.contains("パスワード")) {
                command = escape(command, "パスワード");
            } else if (command.toLowerCase().contains("password")) {
                command = escape(command, "password");
            }
            command = "[" + String.format(format, i) + "]" + command;
            log.info(command);
        }
        log.info("----");
    }

    private static String escape(String command, String escapeWord) {
        StringBuilder sb = new StringBuilder();
        boolean occured = false;
        for (String s : command.split(" ")) {
            if (occured) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append("****");
            } else {
                if (s != null) {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(s);
                    occured = s.toLowerCase().contains(escapeWord);
                }
            }
        }
        return sb.toString();
    }

    public static void dumpChanges(CommandBuilder builder) {
        Logger log = LoggerFactory.getLogger(CommandUtil.class);
        StringBuilder sb = new StringBuilder();
        for (ChangeUnit unit : builder.getChangeUnits()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(unit.simpleToString());
        }
        log.debug("BuildResult: changed:" + builder.getClass().getSimpleName() + "\r\n" + sb.toString());
    }
}