package tef.ui.shell;

import lib38k.misc.CommandlineParser;
import lib38k.text.TextTable;
import tef.DateTime;
import tef.GlobalTransactionId;
import tef.TransactionContext;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class ShellCommand {

    public static class Commandline {

        private final String commandline_;
        private final String commandName_;
        private final List<String> args_;

        Commandline(String commandline) throws ShellCommandException {
            commandline_ = commandline;
            String[] tokens = CommandlineParser.parse(commandline);
            if (tokens == null) {
                throw new ShellCommandException("commandline parse error.");
            }

            commandName_ = tokens[0];

            List<String> args = new ArrayList<String>(Arrays.asList(tokens));
            args.remove(0);
            args_ = Collections.unmodifiableList(args);
        }

        public String getCommandline() {
            return commandline_;
        }

        public String getCommandName() {
            return commandName_;
        }

        public List<String> args() {
            return args_;
        }

        public String arg(int index) {
            return index >= argsSize()
                    ? null
                    : args().get(index);
        }

        public int argsSize() {
            return args().size();
        }
    }

    private ShellSession session_;

    public static class ShellCommandException extends Exception {

        public ShellCommandException(String message) {
            super(message);
        }
    }

    public abstract String getArgumentDescription();

    public abstract void process(Commandline commandline) throws ShellCommandException;

    void setSession(ShellSession session) {
        if (session_ != null) {
            throw new IllegalStateException();
        }

        session_ = session;
    }

    protected ShellSession getSession() {
        return session_;
    }

    protected PrintStream getPrintStream() {
        return getSession().getOut();
    }

    protected void println() {
        getPrintStream().println();
    }

    protected void println(String str) {
        getPrintStream().println(str);
    }

    protected void print(String str) {
        getPrintStream().print(str);
    }

    protected void printStackTrace(Throwable t) {
        t.printStackTrace(getPrintStream());
    }

    protected void printTable(TextTable table) {
        for (String tableLine : table.build()) {
            println(tableLine);
        }
    }

    protected static String getArg(String commandline) {
        if (commandline.indexOf(" ") == -1) {
            return null;
        }

        return commandline.substring(commandline.indexOf(" ") + 1, commandline.length());
    }

    public String getCommandUsage() {
        return "usage: "
                + session_.getCommandName(ShellCommand.this)
                + " " + getArgumentDescription();
    }

    protected void beginDistributedTransaction() {
        if (!getSession().isProcessingInBatchMode()) {
            GlobalTransactionId globalTransactionId
                    = TransactionContext.beginDistributedTransaction();
            TransactionContext.resumeLocalTransaction(globalTransactionId);

            session_.log
                    ("distributed-transaction:" + globalTransactionId.getIdString()
                            + "(local:" + TransactionContext.getTransactionId() + ")");
        }

        initializeTransactionParameters();
    }

    protected void beginWriteTransaction() {
        if (!getSession().isProcessingInBatchMode()) {
            TransactionContext.beginWriteTransaction
                    (getSession().getTransactionDescription());
            session_.log("transaction:" + TransactionContext.getTransactionId());
        }

        initializeTransactionParameters();
    }

    protected void beginReadTransaction() {
        if (!getSession().isProcessingInBatchMode()) {
            TransactionContext
                    .beginReadTransaction(getSession().getTransactionDescription());
        }

        initializeTransactionParameters();
    }

    private void initializeTransactionParameters() {
        TransactionContext.setTargetTime(getSession().getTargetTime());
        TransactionContext.setTargetVersion(getSession().getTargetVersion());
    }

    protected void commitTransaction() {
        if (!getSession().isProcessingInBatchMode()) {
            TransactionContext.commit();
        }
    }

    protected void rollbackTransaction() {
        TransactionContext.rollback();
    }

    protected void closeTransaction() {
        TransactionContext.close();
    }

    protected long getTransactionTargetTime() {
        return TransactionContext.getTargetTime();
    }

    protected void setTransactionTargetTime(long time) {
        TransactionContext.setTargetTime(time);
    }

    protected void setTransactionTargetTime(DateTime time) {
        setTransactionTargetTime(time.getValue());
    }

    protected void checkArgsSize(Commandline commandline, int expectedSize)
            throws ShellCommandException {
        if (commandline.argsSize() != expectedSize) {
            throw new ShellCommandException(getCommandUsage());
        }
    }

    protected void checkArgsSize
            (Commandline commandline, int lowerExpectedSize, int upperExpectedSize)
            throws ShellCommandException {
        if (commandline.argsSize() < lowerExpectedSize
                || upperExpectedSize < commandline.argsSize()) {
            throw new ShellCommandException(getCommandUsage());
        }
    }
}
