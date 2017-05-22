package tef.ui.shell;

import lib38k.misc.CommandlineParser;
import tef.*;

import java.io.*;
import java.util.*;
import java.util.List;

public class ShellSession {

    public static enum InvocationMode {

        REGULAR,
        INTERNAL
    }

    enum CommandlineProcessedResultType {

        NOP,
        OK,
        EXIT,
        DEFINED_ERROR,
        UNKNOWN_ERROR,
        COMMAND_NOT_FOUND;
    }

    private enum BatchMode {

        LOCAL_TRANSACTION {
            @Override
            void beginTransaction(String txDesc) {
                TransactionContext.beginWriteTransaction(txDesc);
            }
        },
        DISTRIBUTED_TRANSACTION {
            @Override
            void beginTransaction(String txDesc) {
                GlobalTransactionId txid = TransactionContext.beginDistributedTransaction();
                TransactionContext.resumeLocalTransaction(txid);
            }
        };

        abstract void beginTransaction(String txDesc);
    }

    static final class CommandlineProcessedResult {

        private CommandlineProcessedResultType resultType_;
        private String message_;
        private Throwable error_;
        private String errorCommand_;
        private Integer errorLineCount_;

        CommandlineProcessedResult(CommandlineProcessedResultType resultType) {
            this(resultType, (String) null);
        }

        CommandlineProcessedResult
                (CommandlineProcessedResultType resultType, String message) {
            resultType_ = resultType;
            message_ = message;
            errorCommand_ = null;
            errorLineCount_ = null;
        }

        CommandlineProcessedResult
                (CommandlineProcessedResultType resultType, Throwable error) {
            this(resultType, error == null ? null : error.getMessage());

            error_ = error;
        }

        void setErrorLine(String command, int errorLineCount) {
            errorCommand_ = command;
            errorLineCount_ = new Integer(errorLineCount);
        }

        public String getErrorCommand() {
            return errorCommand_;
        }

        public Integer getErrorLineCount() {
            return errorLineCount_;
        }

        public CommandlineProcessedResultType getResultType() {
            return resultType_;
        }

        public String getMessage() {
            return message_;
        }

        public Throwable getError() {
            return error_;
        }
    }

    static final String EXIT_COMMAND = "exit";
    static final String BATCH_COMMAND = "batch";
    static final String PROCESS_BATCH_COMMAND = "process-batch";
    static final String BATCH_2PC_COMMAND = "batch-2pc";

    private static class Logger {

        private ShellSession session_;
        private lib38k.logger.Logger logger_;

        Logger(ShellSession session, lib38k.logger.Logger logger) {
            session_ = session;
            logger_ = logger;
        }

        String getMessageHeader() {
            String sessionId
                    = "[session:" + session_.getSessionId()
                    + "-" + session_.getInteractionId() + "]";
            String batchProcessId
                    = session_.isProcessingInBatchMode()
                    ? ("[batch:" + session_.getBatchProcessId().toString()
                    + "-" + session_.getBatchLineCount().toString() + "]")
                    : "";
            return sessionId + batchProcessId;
        }

        void log(String message) {
            logger_.log(getMessageHeader() + message);
        }

        void logUnknownError(Throwable t, String message) {
            logger_.logError(getMessageHeader() + "[unknown-error]" + message, t);
        }
    }

    private static int sessionCounter__ = 0;

    private int sessionId_;
    private int interactionId_ = 0;
    private ShellConnection shellConnection_;
    private Logger logger_;
    private long targetTime_;
    private TransactionId.W targetVersion_;
    private final Map<String, ShellCommand> name2commands_ = new HashMap<String, ShellCommand>();
    private final Map<Class<? extends ShellCommand>, String> command2names_
            = new HashMap<Class<? extends ShellCommand>, String>();
    private boolean isProcessingInBatchMode_ = false;
    private int batchProcessId_ = 0;
    private int batchLineCount_;
    private InvocationMode invocationMode_ = InvocationMode.REGULAR;

    public ShellSession(ShellConnection shellConnection, lib38k.logger.Logger logger) {
        sessionId_ = sessionCounter__++;
        shellConnection_ = shellConnection;
        logger_ = new Logger(this, logger);

        setTargetTime(System.currentTimeMillis());
        setTargetVersion(null);

        updateCommands();
    }

    void start() throws IOException {
        new Thread() {

            public void run() {
                if (!authenticateConnection()) {
                    log("authentication failed.");
                    return;
                } else {
                    log("session started.");
                }

                try {
                    printTelnetBanner();

                    while (true) {
                        ShellSession.CommandlineProcessedResultType processedResult
                                = interact();
                        getOut().flush();
                        if (processedResult
                                == ShellSession.CommandlineProcessedResultType.EXIT) {
                            log(ShellSession.EXIT_COMMAND);
                            break;
                        }
                    }
                } catch (Throwable t) {
                    log
                            ("unknown error: "
                                    + t.getClass().getName() + "\t" + t.getMessage());
                    t.printStackTrace();
                } finally {
                    try {
                        log("closing session.");
                        close();
                        log("session closed.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    void close() throws IOException {
        shellConnection_.close();
    }

    public int getSessionId() {
        return sessionId_;
    }

    int getInteractionId() {
        return interactionId_;
    }

    public void log(String message) {
        logger_.log(message);
    }

    public void logUnknownError(Throwable t, String message) {
        logger_.logUnknownError(t, message);
    }

    protected synchronized void updateCommands() {
        name2commands_.clear();
        command2names_.clear();

        for (Map.Entry<String, Class<? extends ShellCommand>> entry
                : ShellPluginsConfig.getInstance().getCommandPrototypes().entrySet()) {
            try {
                String name = entry.getKey();
                Class<? extends ShellCommand> prototype = entry.getValue();
                ShellCommand command = prototype.newInstance();
                name2commands_.put(name, command);
                command2names_.put(prototype, name);
                command.setSession(ShellSession.this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private CommandlineProcessedResultType interact() throws IOException {
        try {
            interactionId_++;

            getOut().print(getPrompt());
            getOut().flush();

            String commandline = shellConnection_.readLine();
            String commandName = getCommandlineCommandName(commandline);
            if (commandName != null
                    && (commandName.equals(BATCH_COMMAND)
                    || commandName.equals(PROCESS_BATCH_COMMAND))) {
                return processBatch(commandline, BatchMode.LOCAL_TRANSACTION);
            } else if (commandName != null && commandName.equals(BATCH_2PC_COMMAND)) {
                return processBatch(commandline, BatchMode.DISTRIBUTED_TRANSACTION);
            } else {
                return processCommandline(commandline).getResultType();
            }
        } finally {
            TransactionContext.close();
        }
    }

    private boolean isCommentLine(String commandline) {
        String trimmedline = commandline.trim();
        return trimmedline.length() > 0 && trimmedline.charAt(0) == '#';
    }

    private String getCommandlineCommandName(String commandline) {
        String[] commandTokens = CommandlineParser.parse(commandline);
        return commandTokens == null
                ? null
                : (commandTokens.length == 0 ? "" : commandTokens[0]);
    }

    public final ShellConnection getShellConnection() {
        return shellConnection_;
    }

    public final PrintStream getOut() {
        return shellConnection_.getOut();
    }

    protected void printTelnetBanner() {
        getOut().println
                (TefService.TEF_NAME + " [build:" + TefService.instance().getBuildVersion() + "]");
        getOut().println();
    }

    private CommandlineProcessedResultType processBatch
            (String batchCommand, BatchMode batchMode) {
        log("[process-batch]" + batchCommand);

        String[] batchCommandTokens = CommandlineParser.parse(batchCommand);
        if (batchCommandTokens.length < 1 || 3 < batchCommandTokens.length) {
            log("[defined-error]" + batchCommand);
            getOut().println("* error: argument error.");
            return CommandlineProcessedResultType.DEFINED_ERROR;
        }

        boolean isEchoOn = true;

        String[] commandlines = null;
        try {
            if (batchCommandTokens.length == 1) {
                commandlines = readBatchCommandlinesByTty();
            } else if (batchCommandTokens.length >= 2) {
                commandlines
                        = readBatchCommandlinesByFile(new File(batchCommandTokens[1]));
            }

            if (batchCommandTokens.length >= 3) {
                String echoModeStr = batchCommandTokens[2];
                if (echoModeStr.equals("echo-on")) {
                    isEchoOn = true;
                } else if (echoModeStr.equals("echo-off")) {
                    isEchoOn = false;
                } else {
                    log("[defined-error]" + batchCommand);
                    getOut().println
                            ("* error: argument error (echo-mode:[echo-on|echo-off]).");
                    return CommandlineProcessedResultType.DEFINED_ERROR;
                }
            }
        } catch (IOException ioe) {
            log("i/o error: " + ioe.getClass().getName() + " " + ioe.getMessage());
            getOut().println("* error: i/o error.");
            ioe.printStackTrace(getOut());
            return CommandlineProcessedResultType.UNKNOWN_ERROR;
        }

        if (commandlines == null) {
            return CommandlineProcessedResultType.NOP;
        }

        try {
            log("begin-batch: " + batchProcessId_);
            getOut().println("begin-batch...");

            batchMode.beginTransaction(getTransactionDescription());

            CommandlineProcessedResult batchProcessedResult
                    = processBatch(commandlines, isEchoOn);
            CommandlineProcessedResultType resultType = batchProcessedResult.getResultType();
            if (resultType == CommandlineProcessedResultType.OK) {
                TransactionContext.commit();
                log("batch completed.");
                getOut().println("batch completed.");
            } else {
                TransactionContext.rollback();
                log("batch aborted.");
                getOut().println("batch aborted.");
            }
            return resultType;
        } finally {
            TransactionContext.close();
        }
    }

    synchronized CommandlineProcessedResult processBatch
            (String[] commandlines, boolean isEchoOn) {
        log("transaction-" + TransactionContext.getTransactionId());
        try {
            isProcessingInBatchMode_ = true;

            batchProcessId_++;

            if (!isEchoOn) {
                getOut().print(commandlines.length + " lines:");
            }

            for (int i = 0; i < commandlines.length; i++) {
                batchLineCount_ = i + 1;

                String commandline = commandlines[i];

                if (isEchoOn) {
                    getOut().print(getPrompt());
                    getOut().println(commandline);
                    getOut().flush();
                } else {
                    getOut().print(".");
                    getOut().flush();
                }

                CommandlineProcessedResult processedResult
                        = processCommandline(commandline);
                CommandlineProcessedResultType resultType = processedResult.getResultType();
                if (resultType == CommandlineProcessedResultType.OK
                        || resultType == CommandlineProcessedResultType.NOP) {
                    continue;
                } else if (resultType == CommandlineProcessedResultType.EXIT) {
                    return processedResult;
                } else if (resultType == CommandlineProcessedResultType.DEFINED_ERROR
                        || resultType == CommandlineProcessedResultType.UNKNOWN_ERROR
                        || resultType == CommandlineProcessedResultType.COMMAND_NOT_FOUND) {
                    processedResult.setErrorLine(commandline, batchLineCount_);
                    return processedResult;
                } else {
                    throw new RuntimeException("unknown result type.");
                }
            }

            if (!isEchoOn) {
                getOut().println();
            }

            return new CommandlineProcessedResult(CommandlineProcessedResultType.OK);
        } finally {
            isProcessingInBatchMode_ = false;
            batchLineCount_ = 0;

            batchPostprocess();
        }
    }

    protected void batchPostprocess() {
    }

    private String[] readBatchCommandlinesByTty() throws IOException {
        List<String> definedCommandlines = new ArrayList<String>();
        while (true) {
            int commandlinesCount = definedCommandlines.size() + 1;
            getOut().print("[batch-def:" + commandlinesCount + "] ");
            getOut().flush();

            String commandline;
            commandline = shellConnection_.readLine();
            getOut().flush();

            if (commandline.equals("abort")) {
                return null;
            }
            if (commandline.equals("end")) {
                break;
            }
            if (commandline.equals("help")) {
                getOut().println("abort");
                getOut().println("end");
                continue;
            }

            definedCommandlines.add(commandline);
        }

        return definedCommandlines.toArray(new String[0]);
    }

    private String[] readBatchCommandlinesByFile(File batchFile) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(batchFile));

            List<String> result = new ArrayList<String>();
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
            return result.toArray(new String[0]);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    synchronized CommandlineProcessedResult processCommandline(String commandline) {
        if (commandline.equals(EXIT_COMMAND)) {
            return new CommandlineProcessedResult(CommandlineProcessedResultType.EXIT);
        }

        if (commandline.equals("")) {
            return new CommandlineProcessedResult(CommandlineProcessedResultType.NOP);
        }
        if (commandline.trim().equals("") || isCommentLine(commandline)) {
            log("[nop]" + commandline);
            return new CommandlineProcessedResult(CommandlineProcessedResultType.NOP);
        }

        ShellCommand command = getCommand(getCommandlineCommandName(commandline));
        if (command == null) {
            log("[no-command]" + commandline);
            getOut().println("* error: command not found.");
            return new CommandlineProcessedResult
                    (CommandlineProcessedResultType.COMMAND_NOT_FOUND);
        }

        try {
            command.process(new ShellCommand.Commandline(commandline));
            log("[ok]" + commandline);
            return new CommandlineProcessedResult(CommandlineProcessedResultType.OK);
        } catch (ShellCommand.ShellCommandException sce) {
            log("[defined-error]" + commandline);
            getOut().println("* error: " + sce.getMessage());
            return new CommandlineProcessedResult
                    (CommandlineProcessedResultType.DEFINED_ERROR, sce.getMessage());
        } catch (Throwable t) {
            logUnknownError(t, commandline);
            getOut().println("* unknown-error:");
            t.printStackTrace(getOut());
            return new CommandlineProcessedResult
                    (CommandlineProcessedResultType.UNKNOWN_ERROR, t);
        }
    }

    protected ShellCommand getCommand(String commandName) {
        return name2commands_.get(commandName);
    }

    protected List<String> getAvailableCommandNames() {
        return new ArrayList<String>(name2commands_.keySet());
    }

    protected boolean authenticateConnection() {
        return true;
    }

    public synchronized void setTargetTime(long targetTime) {
        targetTime_ = targetTime;
    }

    public void setTargetTime(DateTime time) {
        setTargetTime(time.getValue());
    }

    public synchronized long getTargetTime() {
        return targetTime_;
    }

    public synchronized void setTargetVersion(TransactionId.W version) {
        targetVersion_ = version;
    }

    public synchronized TransactionId.W getTargetVersion() {
        return targetVersion_;
    }

    protected String getPrompt() {
        return "[" + getPromptBody() + "] # ";
    }

    protected String getPromptBody() {
        String serviceName = TefService.instance().getServiceName();
        return (serviceName != null ? serviceName + ":" : "")
                + (isProcessingInBatchMode()
                ? "batch-" + getBatchLineCount() + ":" : "")
                + getTargetTimeString()
                + (getTargetVersion() == null
                ? "" : " " + getTargetVersion().toString());
    }

    protected String getTargetTimeString() {
        if (targetTime_ % 1000 != 0) {
            return DateTimeFormat.YMDHMSS_DOT.format(targetTime_);
        }

        String ymd = DateTimeFormat.YMD_DOT.format(targetTime_);
        String ymdhms = DateTimeFormat.YMDHMS_DOT.format(targetTime_);
        return (ymd + "-00:00:00").equals(ymdhms)
                ? ymd
                : ymdhms;
    }

    protected String getTransactionDescription() {
        return null;
    }

    protected String getCommandName(ShellCommand command) {
        return command2names_.get(command.getClass());
    }

    public boolean isProcessingInBatchMode() {
        return isProcessingInBatchMode_;
    }

    protected Integer getBatchProcessId() {
        return isProcessingInBatchMode()
                ? (new Integer(batchProcessId_))
                : null;
    }

    protected Integer getBatchLineCount() {
        return isProcessingInBatchMode()
                ? (new Integer(batchLineCount_))
                : null;
    }

    public void setInvocationMode(InvocationMode mode) {
        invocationMode_ = mode;
    }

    public InvocationMode getInvocationMode() {
        return invocationMode_;
    }

    public List<String> getCommandCandidates(String currentLine) {
        Set<String> cmdNames = new HashSet<String>();
        cmdNames.add(BATCH_COMMAND);
        cmdNames.add(BATCH_2PC_COMMAND);
        cmdNames.add(PROCESS_BATCH_COMMAND);
        for (String cmdname : getAvailableCommandNames()) {
            cmdNames.add(cmdname);
        }

        SortedSet<String> selectedCmdNames = new TreeSet<String>();
        for (String cmdName : cmdNames) {
            if (cmdName.startsWith(currentLine)) {
                selectedCmdNames.add(cmdName);
            }
        }

        List<String> result = new ArrayList<String>(selectedCmdNames);
        Collections.sort(result);
        return result;
    }
}
