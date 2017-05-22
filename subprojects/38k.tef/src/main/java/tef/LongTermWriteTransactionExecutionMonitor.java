package tef;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/*
 * 20110712-848
 *  - 更新transactionを監視し, 長時間の実行が検出された場合は定期的に全thread dumpを行う.
 */
class LongTermWriteTransactionExecutionMonitor {

    private static final long SAMPLING_INTERVAL = 30 * 1000;
    private static final long THRESHOLD = 100 * 1000;
    private static final long DUMP_INTERVAL = 10 * 1000;

    private static final LongTermWriteTransactionExecutionMonitor instance__
            = new LongTermWriteTransactionExecutionMonitor();

    static synchronized LongTermWriteTransactionExecutionMonitor getInstance() {
        return instance__;
    }

    private final Set<Transaction> transactions_ = new HashSet<Transaction>();

    private LongTermWriteTransactionExecutionMonitor() {
        new Thread("long-term write-tx execution monitor") {

            private final SimpleDateFormat dateFormat_
                    = new SimpleDateFormat("yyyy-MMdd-HHmmss-SSS");
            private final File logDir_
                    = TefService.instance().getLogs().getThreadDumpDirectory();

            private PrintStream out_ = null;

            @Override
            public void run() {
                try {
                    while (true) {
                        if (isToDumpThreads()) {
                            if (out_ == null) {
                                String logFileName = getTimestamp() + ".log";
                                File logFile = new File(logDir_, logFileName);
                                try {
                                    out_ = new PrintStream
                                            (new BufferedOutputStream
                                                    (new FileOutputStream(logFile, true)));
                                } catch (FileNotFoundException fnfe) {
                                    TefService.instance().logError("", fnfe);
                                    throw new RuntimeException(fnfe);
                                }
                            }

                            dumpThreads(out_);

                            try {
                                Thread.sleep(DUMP_INTERVAL);
                            } catch (InterruptedException ie) {
                            }
                        } else {
                            if (out_ != null) {
                                dumpThreads(out_);

                                out_.close();
                                out_ = null;
                            }

                            try {
                                Thread.sleep(SAMPLING_INTERVAL);
                            } catch (InterruptedException ie) {
                            }
                        }
                    }
                } catch (RuntimeException re) {
                    TefService.instance().logError("", re);
                    throw re;
                } catch (Error e) {
                    TefService.instance().logError("", e);
                    throw e;
                }
            }

            private String getTimestamp() {
                return dateFormat_.format(new Date());
            }

            private void dumpThreads(PrintStream out) {
                out.println(getTimestamp());
                out.println();

                for (ThreadInfo info
                        : ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)) {
                    out.print(info);
                }

                out.println();
                out.println();

                out.flush();
            }
        }.start();
    }

    private synchronized boolean isToDumpThreads() {
        long now = System.currentTimeMillis();
        for (Transaction tx : transactions_) {
            if (THRESHOLD < now - tx.getBeginTime()) {
                return true;
            }
        }
        return false;
    }

    synchronized void begin(Transaction tx) {
        transactions_.add(tx);
    }

    synchronized void end(Transaction tx) {
        transactions_.remove(tx);
    }
}
