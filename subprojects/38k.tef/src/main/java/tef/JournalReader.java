package tef;

import java.io.IOException;
import java.text.DecimalFormat;

final class JournalReader {

    private JournalReader() {
    }

    static void restoreJournals(final TefService tefService)
            throws IOException, JournalRestorationException {
        FullJournalEntries entries = new FullJournalEntries(tefService);
        restoreCommittedLogs(tefService, entries);
        entries.close();

        Transaction lastCommittedTransaction = Transaction.getLastCommittedTransaction();
        if (lastCommittedTransaction != null) {
            Integer maxRollbackedTxid = entries.getMaxRollbackedTransactionId();
            if (maxRollbackedTxid != null) {
                if (lastCommittedTransaction.getId().serial < maxRollbackedTxid) {
                    Transaction.setMaxWriteTransactionId(maxRollbackedTxid);
                }
            }
        }
    }

    private static void restoreCommittedLogs
            (final TefService tefService, final FullJournalEntries entries)
            throws JournalRestorationException {
        boolean showProgress = false;
        String runningMode = System.getProperty("running_mode");
        if (runningMode != null
                && runningMode.equals("console")) {
            showProgress = true;
        }

        class Progress {
            volatile boolean completed = false;
            volatile int counter = 0;
            volatile long completedSize = 0;
        }

        final Progress progress = new Progress();

        if (showProgress) {
            new Thread() {

                private long totalSize_ = getTotalSize();

                private long getTotalSize() {
                    long result = 0;
                    for (JournalEntry entry : entries.getJournalEntryIterable()) {
                        result += entry.getSize();
                    }
                    return result;
                }

                public void run() {
                    long completedSize = 0;

                    DecimalFormat formatter = new DecimalFormat("0.0");

                    try {
                        while (!progress.completed) {
                            float completedPercent
                                    = (float) progress.completedSize / (float) totalSize_
                                    * 100;
                            StringBuffer message = new StringBuffer("restoring: ");
                            message.append(Integer.toString(progress.counter + 1));
                            message.append("(");
                            message.append(formatter.format(completedPercent));
                            message.append("%)\r");
                            System.out.print(message.toString());

                            Thread.sleep(100);
                        }
                    } catch (InterruptedException ie) {
                    }

                    System.out.println("restored: " + progress.counter + " transactions.");
                }
            }.start();
        }

        final JournalEntryRestorer restorer = new JournalEntryRestorer(tefService, true);
        for (JournalEntry journal : entries.getJournalEntryIterable()) {
            restorer.restore(journal);
            TransactionContext.commit();

            progress.completedSize += journal.getSize();
            progress.counter++;
        }

        progress.completed = true;
    }
}
