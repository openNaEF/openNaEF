package voss.discovery.iolib.console;

import java.io.IOException;

@SuppressWarnings("serial")
public class InvalidCommandException extends IOException {
    public final ConsoleCommand invalidCommand;

    public InvalidCommandException() {
        super();
        invalidCommand = null;
    }

    public InvalidCommandException(String msg) {
        super(msg);
        invalidCommand = null;
    }

    public InvalidCommandException(String msg, Throwable cause) {
        super(msg, cause);
        invalidCommand = null;
    }

    public InvalidCommandException(Throwable cause) {
        super(cause);
        invalidCommand = null;
    }

    public InvalidCommandException(ConsoleCommand command) {
        super();
        invalidCommand = command;
    }

    public InvalidCommandException(String msg, ConsoleCommand command) {
        super(msg);
        invalidCommand = command;
    }

    public InvalidCommandException(String msg, Throwable cause, ConsoleCommand command) {
        super(msg, cause);
        invalidCommand = command;
    }

    public InvalidCommandException(Throwable cause, ConsoleCommand command) {
        super(cause);
        invalidCommand = command;
    }

    public ConsoleCommand getInvaidCommand() {
        return this.invalidCommand;
    }
}