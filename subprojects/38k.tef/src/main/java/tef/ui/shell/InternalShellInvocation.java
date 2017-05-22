package tef.ui.shell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class InternalShellInvocation {

    public static class InvocationException extends Exception {

        private String command_;
        private Integer batchLineCount_;
        private Throwable error_;

        InvocationException
                (String command, Integer batchLineCount, String message, Throwable error) {
            super(message);

            command_ = command;
            batchLineCount_ = batchLineCount;
            error_ = error;
        }

        public Throwable getError() {
            return error_;
        }

        public String getCommand() {
            return command_;
        }

        public Integer getBatchLineCount() {
            return batchLineCount_;
        }
    }

    private static class InternalShellConnection implements ShellConnection {

        private PrintStream out_ = new PrintStream(new ByteArrayOutputStream());

        public String readLine() throws IOException {
            throw new UnsupportedOperationException();
        }

        public PrintStream getOut() {
            return out_;
        }

        public void setEchoEnabled(boolean value) {
        }

        public void resetCommandHistory() {
            throw new UnsupportedOperationException();
        }

        public void close() throws IOException {
            out_.close();
        }
    }

    private ShellSession shellSession_;

    public InternalShellInvocation() {
        shellSession_ =
                ShellServer.getInstance().createSession
                        (new InternalShellConnection(), ShellServer.getInstance().getLogger());
        shellSession_.setInvocationMode(ShellSession.InvocationMode.INTERNAL);
        shellSession_.log("internal shell invocation.");
    }

    public ShellSession getShellSession() {
        return shellSession_;
    }

    public void close() throws IOException {
        shellSession_.log("closing session.");
        shellSession_.close();
        shellSession_.log("session closed.");

        shellSession_ = null;
    }

    public void processCommandline(String commandline) throws InvocationException {
        ShellSession.CommandlineProcessedResult processResult
                = shellSession_.processCommandline(commandline);
        handleResult(processResult);
    }

    public void processBatch(String[] commandlines) throws InvocationException {
        ShellSession.CommandlineProcessedResult processResult
                = shellSession_.processBatch(commandlines, true);
        handleResult(processResult);
    }

    private void handleResult(ShellSession.CommandlineProcessedResult result)
            throws InvocationException {
        ShellSession.CommandlineProcessedResultType resultType = result.getResultType();
        if (resultType == ShellSession.CommandlineProcessedResultType.OK
                || resultType == ShellSession.CommandlineProcessedResultType.NOP) {
            return;
        } else if (resultType == ShellSession.CommandlineProcessedResultType.EXIT) {
            throw new InvocationException(null, null, "batch aborted.", null);
        } else if (resultType == ShellSession.CommandlineProcessedResultType.DEFINED_ERROR) {
            throw new InvocationException
                    (result.getErrorCommand(), result.getErrorLineCount(), result.getMessage(),
                            null);
        } else if (resultType == ShellSession.CommandlineProcessedResultType.UNKNOWN_ERROR) {
            throw new InvocationException
                    (result.getErrorCommand(), result.getErrorLineCount(), result.getMessage(),
                            result.getError());
        } else if (resultType == ShellSession.CommandlineProcessedResultType.COMMAND_NOT_FOUND) {
            throw new InvocationException
                    (result.getErrorCommand(), result.getErrorLineCount(), "command not found.",
                            null);
        } else {
            throw new RuntimeException("unknown result type.");
        }
    }
}
