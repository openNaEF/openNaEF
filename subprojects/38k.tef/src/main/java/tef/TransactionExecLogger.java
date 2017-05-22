package tef;

import lib38k.logger.FileLogger;
import lib38k.misc.BinaryStringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class TransactionExecLogger {

    private static final String TRANSACTION_TYPE_STANDALONE_SYMBOL = "*";
    private static final String TRANSACTION_TYPE_DISTRIBUTED_SYMBOL = "#";

    private final TefService tefService_;
    private final FileLogger logger_;

    TransactionExecLogger(TefService tefService, FileLogger logger) {
        tefService_ = tefService;
        logger_ = logger;

        TefServiceConfig.StackTraceCatalogConfig stacktraceCatalogConfig
                = TefService.instance().getTefServiceConfig().stackTraceCatalogConfig;
        if (stacktraceCatalogConfig != null
                && stacktraceCatalogConfig.omissionLineRegExps != null) {
            Collection<String> omissionLineRegExps = stacktraceCatalogConfig.omissionLineRegExps;
            logger_.println
                    ("stacktrace lines to omit recording: " + omissionLineRegExps.size());
            for (String omissionLineRegExp : omissionLineRegExps) {
                logger_.println(" - " + omissionLineRegExp);
            }
        }

        logger_.println();
    }

    void writeReceptionLog
            (Transaction transaction,
             boolean transactionType,
             StackTraceCatalog.CatalogId stacktraceCatalogId) {
        String transactionTypeSymbol;
        if (transactionType = Transaction.TRANSACTION_TYPE_STANDALONE) {
            transactionTypeSymbol = TRANSACTION_TYPE_STANDALONE_SYMBOL;
        } else if (transactionType = Transaction.TRANSACTION_TYPE_DISTRIBUTED) {
            transactionTypeSymbol = TRANSACTION_TYPE_DISTRIBUTED_SYMBOL;
        } else {
            throw new RuntimeException();
        }
        String extraInfo = getStacktraceCatalogIdStr(stacktraceCatalogId);
        write(transactionTypeSymbol, transaction, extraInfo);
    }

    void writeBeginLog(Transaction transaction) {
        String extraInfo = getGlobalTransactionId(transaction);
        write("+", transaction, extraInfo);
    }

    void writeCommitLog
            (Transaction transaction,
             StackTraceCatalog.CatalogId stacktraceCatalogId) {
        String extraInfo
                = getStacktraceCatalogIdStr(stacktraceCatalogId)
                + "\t"
                + Integer.toString(transaction.countNewObjects())
                + "," + Integer.toString(transaction.countChangedObjects())
                + "," + Integer.toString(transaction.countChangedFields());
        write("c", transaction, extraInfo);
    }

    void writeKillLog(Transaction transaction) {
        write("k", transaction);
    }

    void writeRollbackLog
            (Transaction transaction, StackTraceCatalog.CatalogId stacktraceCatalogId) {
        write("r", transaction, getStacktraceCatalogIdStr(stacktraceCatalogId));
    }

    void writeCloseLog(Transaction transaction) {
        write("-", transaction);
    }

    private void write(String symbol, Transaction transaction) {
        write(symbol, transaction, null);
    }

    private void write(String symbol, Transaction transaction, String extraInfo) {
        synchronized (logger_) {
            String log
                    = symbol + "\t"
                    + transaction.getId().getIdString() + "\t"
                    + Long.toString(System.currentTimeMillis(), 16)
                    + ":" + Long.toString(transaction.getTime(), 16)
                    + ":" + Long.toString(transaction.getCpuTime(), 16)
                    + ":" + Long.toString(transaction.getUserTime(), 16)
                    + (extraInfo == null ? "" : "\t" + extraInfo);
            logger_.println(log);
        }
    }

    private String getStacktraceCatalogIdStr(StackTraceCatalog.CatalogId stacktraceCatalogId) {
        if (stacktraceCatalogId == null) {
            return "-";
        } else {
            boolean isLocalStacktrace
                    = TefService.instance().getServiceName()
                    .equals(stacktraceCatalogId.tefServiceId);
            return isLocalStacktrace
                    ? stacktraceCatalogId.getLocalId()
                    : BinaryStringUtils.getHexExpression
                    (stacktraceCatalogId.tefServiceId.getBytes())
                    + ":" + stacktraceCatalogId.getLocalId();
        }
    }

    private String getGlobalTransactionId(Transaction transaction) {
        GlobalTransactionId globalTransactionId
                = TransactionManager.Facade.getGlobalTransactionId(transaction);
        return globalTransactionId == null
                ? "-"
                : globalTransactionId.getIdString();
    }

    List<String> getStacktraceLines(TransactionId transactionId) {
        for (File logFile : logger_.getFile().getParentFile().listFiles()) {
            List<String> stacktraceLines;
            try {
                stacktraceLines = getStacktraceLines(logFile, transactionId);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }

            if (stacktraceLines != null) {
                return stacktraceLines;
            }
        }
        return null;
    }

    private List<String> getStacktraceLines(File logFile, TransactionId transactionId)
            throws IOException {
        String transactionIdStr = transactionId.getIdString();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(logFile));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\t");
                if (tokens.length < 4) {
                    continue;
                }
                if (!(tokens[0].equals(TRANSACTION_TYPE_STANDALONE_SYMBOL)
                        || tokens[0].equals(TRANSACTION_TYPE_DISTRIBUTED_SYMBOL))) {
                    continue;
                }

                if (!tokens[1].equals(transactionIdStr)) {
                    continue;
                } else {
                    String stacktraceCatalogIdStr = tokens[3];
                    if (stacktraceCatalogIdStr.equals("-")) {
                        return new ArrayList<String>();
                    }

                    if (stacktraceCatalogIdStr.indexOf(":") < 0) {
                        return new ArrayList<String>();
                    }

                    StackTraceCatalog.CatalogId catalogId
                            = StackTraceCatalog.CatalogId.parseAsLocalId(stacktraceCatalogIdStr);
                    StackTraceCatalog stacktraceCatalog
                            = catalogId == null
                            ? null
                            : tefService_.getStacktraceCatalog(catalogId.type);
                    return stacktraceCatalog == null
                            ? new ArrayList<String>()
                            : stacktraceCatalog.getStacktraceLines(catalogId.catalogElementId);
                }
            }
            return null;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
}
