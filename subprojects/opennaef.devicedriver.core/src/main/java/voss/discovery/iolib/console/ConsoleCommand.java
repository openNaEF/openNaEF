package voss.discovery.iolib.console;

import voss.discovery.iolib.simpletelnet.ModeChanger;

import java.io.Serializable;

public class ConsoleCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    private final ModeChanger mode;
    private final String command;
    private final String extInfoKeyName;

    public ConsoleCommand(ModeChanger mode, String command) {
        this.mode = mode;
        this.command = command;
        this.extInfoKeyName = command;
    }

    public ConsoleCommand(ModeChanger mode, String command, String extInfoKeyName) {
        this.mode = mode;
        this.command = command;
        this.extInfoKeyName = extInfoKeyName;
    }

    public ModeChanger getMode() {
        return this.mode;
    }

    public String getCommand() {
        return this.command;
    }

    public String getExtInfoKeyName() {
        return this.extInfoKeyName;
    }
}