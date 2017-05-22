package tef.ui.shell;

import lib38k.text.TextTable;
import tef.NoTransactionContextFoundException;
import tef.TransactionContext;
import tef.TransactionId;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ShowActiveTransactionsCommand extends ShellCommand {

    private static final String[] COLUMNS
            = new String[]{"id", "since", "time", "spec"};

    private static final DateFormat timeFormatter__
            = new SimpleDateFormat("HH:mm:ss");
    private static final NumberFormat elapsedTimeFormatter__
            = new DecimalFormat("###,##0.0");

    private static final int COLUMN_WIDTH = 10;

    public String getArgumentDescription() {
        return "";
    }

    @Override
    public void process(Commandline commandline) {
        beginReadTransaction();

        TransactionId transactionId = TransactionContext.getTransactionId();

        TransactionId[] activeTxIds = TransactionContext.getActiveTransactionIds();
        if (activeTxIds.length == 0) {
            return;
        }

        TextTable table = new TextTable(COLUMNS);
        for (TransactionId activeTransactionId : activeTxIds) {
            if (activeTransactionId.equals(transactionId)) {
                continue;
            }

            try {
                table.addRow(getRow(activeTransactionId));
            } catch (NoTransactionContextFoundException ntcfe) {
                continue;
            }
        }

        printTable(table);
    }

    private String[] getRow(TransactionId transactionId)
            throws NoTransactionContextFoundException {
        String[] result = new String[COLUMNS.length];

        int columnCounter = 0;

        result[columnCounter++] = transactionId.getIdString();

        long beginTime = TransactionContext.getActiveTransactionBeginTime(transactionId);
        synchronized (timeFormatter__) {
            result[columnCounter++] = timeFormatter__.format(new Date(beginTime));
        }

        long time = System.currentTimeMillis() - beginTime;
        synchronized (elapsedTimeFormatter__) {
            result[columnCounter++]
                    = elapsedTimeFormatter__.format(time / 1000.0d);
        }

        String transactionDesc = TransactionContext.getTransactionDescription(transactionId);
        result[columnCounter++]
                = transactionDesc == null
                ? "null"
                : transactionDesc;

        return result;
    }
}
