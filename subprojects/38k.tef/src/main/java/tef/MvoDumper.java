package tef;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class MvoDumper {

    private static final File DUMP_DIR
            = new File(TefService.instance().getWorkingDirectory(), "dump");
    private static final File DUMP_FILE
            = new File(DUMP_DIR, MvoBulkDump.DUMP_FILE_NAME);
    private static final File FIELD_TO_MVO_DUMP_POS_INDEX_FILE
            = new File(DUMP_DIR, MvoBulkDump.FIELD_TO_MVO_DUMP_POS_INDEX_FILE_NAME);
    private static final File MVO_TO_FIELD_POS_INDEX_FILE
            = new File(DUMP_DIR, MvoBulkDump.MVO_TO_FIELD_POS_INDEX_FILE_NAME);

    public static interface MvoDumpStream {

        public void preprocessMvo(MVO mvo);

        public void postprocessMvo(MVO mvo);

        public void preprocessF1(MVO.F1 f1);

        public void postprocessF1(MVO.F1 f1);

        public void preprocessS1(MVO.S1 s1);

        public void postprocessS1(MVO.S1 s1);

        public void preprocessM1(MVO.M1 m1);

        public void postprocessM1(MVO.M1 m1);

        public void preprocessF2(MVO.F2 f2);

        public void postprocessF2(MVO.F2 f2);

        public void preprocessS2(MVO.S2 s2);

        public void postprocessS2(MVO.S2 s2);

        public void preprocessM2(MVO.M2 m2);

        public void postprocessM2(MVO.M2 m2);

        public void println(String line);
    }

    public static final class DefaultMvoDumpStream implements MvoDumpStream {

        private PrintStream out_;

        public DefaultMvoDumpStream(PrintStream out) {
            out_ = out;
        }

        public void preprocessMvo(MVO mvo) {
        }

        public void postprocessMvo(MVO mvo) {
        }

        public void preprocessF1(MVO.F1 f1) {
        }

        public void postprocessF1(MVO.F1 f1) {
        }

        public void preprocessS1(MVO.S1 s1) {
        }

        public void postprocessS1(MVO.S1 s1) {
        }

        public void preprocessM1(MVO.M1 m1) {
        }

        public void postprocessM1(MVO.M1 m1) {
        }

        public void preprocessF2(MVO.F2 f2) {
        }

        public void postprocessF2(MVO.F2 f2) {
        }

        public void preprocessS2(MVO.S2 s2) {
        }

        public void postprocessS2(MVO.S2 s2) {
        }

        public void preprocessM2(MVO.M2 m2) {
        }

        public void postprocessM2(MVO.M2 m2) {
        }

        public void println(String line) {
            out_.println(line);
        }
    }

    private Transaction lastTransaction_;
    private MvoDumpStream out_;

    private final MVO.F1HistoryElementProcessor f1Processor_
            = new MVO.F1HistoryElementProcessor() {

        private MonaxisEngine.HistoryElementProcessor monaxisProcessor_
                = new MonaxisEngine.HistoryElementProcessor() {
            public final void process
                    (Transaction historyElementTransaction,
                     Object historyElementValue) {
                if (!isTarget(historyElementTransaction)) {
                    return;
                }

                if (!processed_) {
                    printFieldDescriptor(target_);
                    processed_ = true;
                }
                println
                        (" " + historyElementTransaction.getId().getIdString()
                                + " " + encodeValue(historyElementValue));
            }
        };

        private MVO.F1 target_;
        private boolean processed_;

        public void preprocess(MVO.F1 target) {
            target_ = target;
            processed_ = false;

            out_.preprocessF1(target);
        }

        public void postprocess(MVO.F1 target) {
            out_.postprocessF1(target);
        }

        public MonaxisEngine.HistoryElementProcessor getMonaxisProcessor() {
            return monaxisProcessor_;
        }
    };

    private final MVO.F2HistoryElementProcessor f2Processor_
            = new MVO.F2HistoryElementProcessor() {

        private BinaxesEngine.HistoryElementProcessor binaxesProcessor_
                = new BinaxesEngine.HistoryElementProcessor() {
            public final void process
                    (Transaction historyElementTransaction,
                     long historyElementTargetTime,
                     Object historyElementValue) {
                if (!isTarget(historyElementTransaction)) {
                    return;
                }

                if (!processed_) {
                    printFieldDescriptor(target_);
                    processed_ = true;
                }
                println
                        (" " + historyElementTransaction.getId().getIdString()
                                + " " + Long.toString(historyElementTargetTime, 16)
                                + " " + encodeValue(historyElementValue));
            }
        };

        private MVO.F2 target_;
        private boolean processed_;

        public void preprocess(MVO.F2 target) {
            target_ = target;
            processed_ = false;

            out_.preprocessF2(target);
        }

        public void postprocess(MVO.F2 target) {
            out_.postprocessF2(target);
        }

        public BinaxesEngine.HistoryElementProcessor getBinaxesProcessor() {
            return binaxesProcessor_;
        }
    };

    private final MVO.S1HistoryElementProcessor s1Processor_
            = new MVO.S1HistoryElementProcessor() {
        private MonaxisEngine.HistoryElementProcessor elementHistoryProcessor_
                = new MonaxisEngine.HistoryElementProcessor() {
            public void process
                    (Transaction historyElementTransaction,
                     Object historyElementValue) {
                if (!isTarget(historyElementTransaction)) {
                    return;
                }

                if (!fieldProcessed_) {
                    printFieldDescriptor(targetField_);
                    fieldProcessed_ = true;
                }
                if (!elementProcessed_) {
                    println(" " + encodeValue(targetElement_));
                    elementProcessed_ = true;
                }
                println
                        ("  " + historyElementTransaction.getId().getIdString()
                                + " "
                                + (((Boolean) historyElementValue).booleanValue()
                                ? "+" : "-"));
            }
        };

        private MVO.S1 targetField_;
        private boolean fieldProcessed_;

        private Object targetElement_;
        private boolean elementProcessed_;

        public void preprocessS1(MVO.S1 target) {
            targetField_ = target;
            fieldProcessed_ = false;

            out_.preprocessS1(targetField_);
        }

        public void postprocessS1(MVO.S1 target) {
            out_.postprocessS1(target);
        }

        public void preprocessEntry(Object key) {
            targetElement_ = key;
            elementProcessed_ = false;
        }

        public void postprocessEntry(Object key) {
        }

        public MonaxisEngine.HistoryElementProcessor getElementHistoryProcessor() {
            return elementHistoryProcessor_;
        }
    };

    private final MVO.M1HistoryElementProcessor m1Processor_
            = new MVO.M1HistoryElementProcessor() {
        private MonaxisEngine.HistoryElementProcessor elementHistoryProcessor_
                = new MonaxisEngine.HistoryElementProcessor() {
            public void process
                    (Transaction historyElementTransaction,
                     Object historyElementValue) {
                if (!isTarget(historyElementTransaction)) {
                    return;
                }

                if (!fieldProcessed_) {
                    printFieldDescriptor(targetField_);
                    fieldProcessed_ = true;
                }
                if (!elementProcessed_) {
                    println(" " + encodeValue(key_));
                    elementProcessed_ = true;
                }
                println
                        ("  " + historyElementTransaction.getId().getIdString()
                                + " " + encodeValue(historyElementValue));
            }
        };

        private MVO.M1 targetField_;
        private boolean fieldProcessed_;

        private Object key_;
        private boolean elementProcessed_;

        public void preprocessM1(MVO.M1 target) {
            targetField_ = target;
            fieldProcessed_ = false;

            out_.preprocessM1(targetField_);
        }

        public void postprocessM1(MVO.M1 target) {
            out_.postprocessM1(target);
        }

        public void preprocessEntry(Object key) {
            key_ = key;
            elementProcessed_ = false;
        }

        public void postprocessEntry(Object key) {
        }

        public MonaxisEngine.HistoryElementProcessor getElementHistoryProcessor() {
            return elementHistoryProcessor_;
        }
    };

    private final MVO.S2HistoryElementProcessor s2Processor_
            = new MVO.S2HistoryElementProcessor() {
        private BinaxesEngine.HistoryElementProcessor elementHistoryProcessor_
                = new BinaxesEngine.HistoryElementProcessor() {
            public final void process
                    (Transaction historyElementTransaction,
                     long historyElementTargetTime,
                     Object historyElementValue) {
                if (!isTarget(historyElementTransaction)) {
                    return;
                }

                if (!fieldProcessed_) {
                    printFieldDescriptor(targetField_);
                    fieldProcessed_ = true;
                }
                if (!elementProcessed_) {
                    println(" " + encodeValue(targetElement_));
                    elementProcessed_ = true;
                }
                println
                        ("  " + historyElementTransaction.getId().getIdString()
                                + " "
                                + (((Boolean) historyElementValue).booleanValue()
                                ? "+" : "-")
                                + " " + Long.toString(historyElementTargetTime, 16));
            }
        };

        private MVO.S2 targetField_;
        private boolean fieldProcessed_;

        private Object targetElement_;
        private boolean elementProcessed_;

        public void preprocessS2(MVO.S2 target) {
            targetField_ = target;
            fieldProcessed_ = false;

            out_.preprocessS2(target);
        }

        public void postprocessS2(MVO.S2 target) {
            out_.postprocessS2(target);
        }

        public void preprocessEntry(Object key) {
            targetElement_ = key;
            elementProcessed_ = false;
        }

        public void postprocessEntry(Object key) {
        }

        public BinaxesEngine.HistoryElementProcessor getElementHistoryProcessor() {
            return elementHistoryProcessor_;
        }
    };

    private final MVO.M2HistoryElementProcessor m2Processor_
            = new MVO.M2HistoryElementProcessor() {
        private BinaxesEngine.HistoryElementProcessor elementHistoryProcessor_
                = new BinaxesEngine.HistoryElementProcessor() {
            public final void process
                    (Transaction historyElementTransaction,
                     long historyElementTargetTime,
                     Object historyElementValue) {
                if (!isTarget(historyElementTransaction)) {
                    return;
                }

                if (!fieldProcessed_) {
                    printFieldDescriptor(targetField_);
                    fieldProcessed_ = true;
                }
                if (!elementProcessed_) {
                    println(" " + encodeValue(targetElement_));
                    elementProcessed_ = true;
                }
                println
                        ("  " + historyElementTransaction.getId().getIdString()
                                + " " + encodeValue(historyElementValue)
                                + " " + Long.toString(historyElementTargetTime, 16));
            }
        };

        private MVO.M2 targetField_;
        private boolean fieldProcessed_;

        private Object targetElement_;
        private boolean elementProcessed_;

        public void preprocessM2(MVO.M2 target) {
            targetField_ = target;
            fieldProcessed_ = false;

            out_.preprocessM2(target);
        }

        public void postprocessM2(MVO.M2 target) {
            out_.postprocessM2(target);
        }

        public void preprocessEntry(Object key) {
            targetElement_ = key;
            elementProcessed_ = false;
        }

        public void postprocessEntry(Object key) {
        }

        public BinaxesEngine.HistoryElementProcessor getElementHistoryProcessor() {
            return elementHistoryProcessor_;
        }
    };

    private final MVO.MvoFieldProcessor mvoProcessor_
            = new MVO.MvoFieldProcessor() {
        public void preprocess(MVO mvo) {
            out_.preprocessMvo(mvo);
        }

        public void postprocess(MVO mvo) {
            out_.postprocessMvo(mvo);
        }

        public MVO.F1HistoryElementProcessor getF1Processor() {
            return f1Processor_;
        }

        public MVO.S1HistoryElementProcessor getS1Processor() {
            return s1Processor_;
        }

        public MVO.M1HistoryElementProcessor getM1Processor() {
            return m1Processor_;
        }

        public MVO.F2HistoryElementProcessor getF2Processor() {
            return f2Processor_;
        }

        public MVO.S2HistoryElementProcessor getS2Processor() {
            return s2Processor_;
        }

        public MVO.M2HistoryElementProcessor getM2Processor() {
            return m2Processor_;
        }

        public MVO.N2HistoryElementProcessor getN2Processor() {
            throw new RuntimeException();
        }
    };

    private final boolean isTarget(Transaction transaction) {
        return transaction.getId().serial <= lastTransaction_.getId().serial;
    }

    private final boolean isTarget(MVO mvo) {
        return MvoUtils.isExistingAt(mvo, (TransactionId.W) lastTransaction_.getId());
    }

    private static String encodeValue(Object value) {
        return ValueEncoder.encode(value);
    }

    private MvoMeta mvoMeta_;

    public MvoDumper(MvoDumpStream out, TransactionId.W lastTransactionId) {
        mvoMeta_ = TefService.instance().getMvoMeta();

        out_ = out;

        if (lastTransactionId == null) {
            lastTransaction_ = Transaction.getLastCommittedTransaction();
        } else {
            lastTransaction_ = Transaction.getTransaction(lastTransactionId);
            if (lastTransaction_ == null) {
                throw new IllegalArgumentException(lastTransactionId.getIdString());
            }
        }
    }

    public void fullDump() {
        dumpTransactions();
        dumpMvos();
    }

    private void dumpMvos() {
        List<MVO> mvos = TefService.instance().getMvoRegistry().list();

        for (MVO mvo : mvos) {
            if (!isTarget(mvo)) {
                continue;
            }

            println
                    (mvo.getInitialVersion().getIdString()
                            + " " + mvo.getTransactionLocalSerial()
                            + " " + mvo.getClass().getName());
        }
        println(".");

        for (MVO mvo : mvos) {
            if (!isTarget(mvo)) {
                continue;
            }

            println
                    (mvo.getMvoId().getLocalStringExpression()
                            + " f0 "
                            + JournalWriter.getFieldDescriptors(mvo));
        }
        println(".");

        for (MVO mvo : mvos) {
            if (!isTarget(mvo)) {
                continue;
            }

            dump(mvo);
        }
    }

    public void dump(MVO mvo) {
        mvo.traverseMvoFields(mvoProcessor_);
    }

    private void dumpTransactions() {
        Transaction[] committedTransactions = Transaction.getCommittedTransactions();
        if (committedTransactions.length == 0) {
            return;
        }

        Transaction[] transactions;
        if (lastTransaction_ == null) {
            transactions = committedTransactions;
        } else {
            int index = Arrays.binarySearch
                    (committedTransactions,
                            lastTransaction_,
                            new Comparator<Transaction>() {

                                public int compare(Transaction o1, Transaction o2) {
                                    return o1.getId().serial - o2.getId().serial;
                                }
                            });
            transactions = new Transaction[index + 1];
            System.arraycopy(committedTransactions, 0, transactions, 0, index + 1);
        }

        dumpTransactions(transactions);
    }

    private void dumpTransactions(Transaction[] transactions) {
        for (int i = 0; i < transactions.length; i++) {
            println(JournalWriter.getTransactionDescriptor(transactions[i]));
        }
        println(".");
    }

    private void println(String line) {
        out_.println(line);
    }

    public static void fullDump(TransactionId.W lastTransactionId) throws IOException {
        RandomAccessFile mvoDumpOut = null;
        RandomAccessFile fieldToMvoDumpPosIndexOut = null;
        PrintStream mvoToFieldPosIndexOut = null;
        try {
            class DumpStream implements MvoDumper.MvoDumpStream {

                private RandomAccessFile mvoDumpOut_;
                private RandomAccessFile fieldToMvoDumpPosIndexOut_;
                private PrintStream mvoToFieldPosIndexOut_;

                DumpStream
                        (RandomAccessFile mvoDumpOut,
                         RandomAccessFile fieldToMvoDumpPosIndexOut,
                         PrintStream mvoToFieldPosIndexOut) {
                    mvoDumpOut_ = mvoDumpOut;
                    fieldToMvoDumpPosIndexOut_ = fieldToMvoDumpPosIndexOut;
                    mvoToFieldPosIndexOut_ = mvoToFieldPosIndexOut;
                }

                public void preprocessMvo(MVO mvo) {
                    mvoToFieldPosIndexOut_.print
                            (mvo.getMvoId().getLocalStringExpression()
                                    + " "
                                    + getFilePointerStr(fieldToMvoDumpPosIndexOut_));
                }

                public void postprocessMvo(MVO mvo) {
                    mvoToFieldPosIndexOut_.println
                            (" " + getFilePointerStr(fieldToMvoDumpPosIndexOut_));
                }

                public void preprocessF1(MVO.F1 f1) {
                    print
                            (fieldToMvoDumpPosIndexOut_,
                                    getFieldDescriptor(f1)
                                            + " " + getFilePointerStr(mvoDumpOut_));
                }

                public void postprocessF1(MVO.F1 f1) {
                    println
                            (fieldToMvoDumpPosIndexOut_,
                                    " " + getFilePointerStr(mvoDumpOut_));
                }

                public void preprocessS1(MVO.S1 s1) {
                    print
                            (fieldToMvoDumpPosIndexOut_,
                                    getFieldDescriptor(s1) + " "
                                            + Long.toString(getFilePointer(mvoDumpOut_), 16));
                }

                public void postprocessS1(MVO.S1 s1) {
                    println
                            (fieldToMvoDumpPosIndexOut_,
                                    " " + getFilePointerStr(mvoDumpOut_));
                }

                public void preprocessM1(MVO.M1 m1) {
                    print
                            (fieldToMvoDumpPosIndexOut_,
                                    getFieldDescriptor(m1) + " "
                                            + Long.toString(getFilePointer(mvoDumpOut_), 16));
                }

                public void postprocessM1(MVO.M1 m1) {
                    println
                            (fieldToMvoDumpPosIndexOut_,
                                    " " + getFilePointerStr(mvoDumpOut_));
                }

                public void preprocessF2(MVO.F2 f2) {
                    print
                            (fieldToMvoDumpPosIndexOut_,
                                    getFieldDescriptor(f2) + " "
                                            + Long.toString(getFilePointer(mvoDumpOut_), 16));
                }

                public void postprocessF2(MVO.F2 f2) {
                    println
                            (fieldToMvoDumpPosIndexOut_,
                                    " " + getFilePointerStr(mvoDumpOut_));
                }

                public void preprocessS2(MVO.S2 s2) {
                    print
                            (fieldToMvoDumpPosIndexOut_,
                                    getFieldDescriptor(s2) + " "
                                            + Long.toString(getFilePointer(mvoDumpOut_), 16));
                }

                public void postprocessS2(MVO.S2 s2) {
                    println
                            (fieldToMvoDumpPosIndexOut_,
                                    " " + getFilePointerStr(mvoDumpOut_));
                }

                public void preprocessM2(MVO.M2 m2) {
                    print
                            (fieldToMvoDumpPosIndexOut_,
                                    getFieldDescriptor(m2) + " "
                                            + Long.toString(getFilePointer(mvoDumpOut_), 16));
                }

                public void postprocessM2(MVO.M2 m2) {
                    println
                            (fieldToMvoDumpPosIndexOut_,
                                    " " + getFilePointerStr(mvoDumpOut_));
                }

                private String getFilePointerStr(RandomAccessFile raf) {
                    return Long.toString(getFilePointer(raf), 16);
                }

                private long getFilePointer(RandomAccessFile raf) {
                    try {
                        return raf.getFilePointer();
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                }

                private void println(RandomAccessFile raf, String line) {
                    print(raf, line);
                    println(raf);
                }

                private void println(RandomAccessFile raf) {
                    try {
                        raf.writeBytes("\r\n");
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                }

                private void print(RandomAccessFile raf, String line) {
                    try {
                        raf.writeBytes(line);
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                }

                public void println(String line) {
                    println(mvoDumpOut_, line);
                }
            }

            mvoDumpOut = new RandomAccessFile(DUMP_FILE, "rw");
            fieldToMvoDumpPosIndexOut
                    = new RandomAccessFile
                    (FIELD_TO_MVO_DUMP_POS_INDEX_FILE, "rw");
            mvoToFieldPosIndexOut
                    = TefFileUtils.newFilePrintStream(MVO_TO_FIELD_POS_INDEX_FILE, false);

            MvoDumper.MvoDumpStream dumpStream
                    = new DumpStream
                    (mvoDumpOut, fieldToMvoDumpPosIndexOut, mvoToFieldPosIndexOut);
            new MvoDumper(dumpStream, lastTransactionId).fullDump();

            mvoDumpOut.writeBytes(".\r\n");
            fieldToMvoDumpPosIndexOut.writeBytes(".\r\n");
            mvoToFieldPosIndexOut.println(".");
        } finally {
            try {
                if (mvoDumpOut != null) {
                    mvoDumpOut.close();
                }
            } finally {
                try {
                    if (fieldToMvoDumpPosIndexOut != null) {
                        fieldToMvoDumpPosIndexOut.close();
                    }
                } finally {
                    if (mvoToFieldPosIndexOut != null) {
                        mvoToFieldPosIndexOut.close();
                    }
                }
            }
        }
    }

    public static void dump
            (MvoDumpStream out, TransactionId.W lastTransactionId, MVO target) {
        new MvoDumper(out, lastTransactionId).dump(target);
    }

    private static String getFieldDescriptor(MVO.MvoField field) {
        return field.getParent().getMvoId().getLocalStringExpression()
                + " "
                + Integer.toString(TefService.instance().getMvoMeta().getFieldId(field), 16);
    }

    private void printFieldDescriptor(MVO.MvoField field) {
        println(getFieldDescriptor(field));
    }
}
