package tef.ui.shell;

import tef.DateTime;

import java.io.PrintStream;

public abstract class ShellExtProgram {

    public static class ShellExtProgramException extends Exception {

        public ShellExtProgramException(String message) {
            super(message);
        }
    }

    public PrintStream out;
    public String[] args;
    public ShellSession session;

    private RunExternalProgramCommand command_;

    protected ShellExtProgram() {
    }

    void setCommand(RunExternalProgramCommand command) {
        command_ = command;
    }

    public abstract void run() throws ShellExtProgramException;

    protected void beginDistributedTransaction() {
        command_.beginDistributedTransaction();
    }

    protected void beginWriteTransaction() {
        command_.beginWriteTransaction();
    }

    protected void beginReadTransaction() {
        command_.beginReadTransaction();
    }

    protected void commitTransaction() {
        command_.commitTransaction();
    }

    protected void rollbackTransaction() {
        command_.rollbackTransaction();
    }

    protected void closeTransaction() {
        command_.closeTransaction();
    }

    protected long getTransactionTargetTime() {
        return command_.getTransactionTargetTime();
    }

    protected void setTransactionTargetTime(long time) {
        command_.setTransactionTargetTime(time);
    }

    protected void setTransactionTargetTime(DateTime time) {
        setTransactionTargetTime(time.getValue());
    }
}
