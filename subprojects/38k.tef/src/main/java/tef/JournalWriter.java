package tef;

import lib38k.misc.BinaryStringUtils;
import tef.JournalEntryRestorer.EntryType;

import java.io.*;
import java.lang.reflect.Field;

import static tef.TefUtils.hexInt;
import static tef.TefUtils.hexLong;

final class JournalWriter {

    final class F1Logger {

        void write(MVO.F1 field, Object value) {
            String fieldDescriptor = getFieldDescriptor(EntryType.F1, field);
            String valueStr = encodeValue(value);

            loggingStream_.println(concat(fieldDescriptor, valueStr));
        }
    }

    final class F2Logger {

        public void write(MVO.F2 field, long time, Object value) {
            String fieldDescriptor = getFieldDescriptor(EntryType.F2, field);
            String timeStr = hexLong(time);
            String valueStr = encodeValue(value);

            loggingStream_.println(concat(fieldDescriptor, timeStr, valueStr));
        }
    }

    abstract class MonaxisMapLogger {

        abstract void write(MVO.MvoField field, Object key, Object value);

        abstract void writeKeyVoid(MVO.MvoField field, Object key);
    }

    final class S1Logger extends MonaxisMapLogger {

        @Override
        void write(MVO.MvoField field, Object key, Object value) {
            boolean mode = ((Boolean) value).booleanValue();
            String fieldDescriptor
                    = getFieldDescriptor(mode ? EntryType.S1_ADD : EntryType.S1_REMOVE, field);
            String valueStr = encodeValue(key);

            loggingStream_.println(concat(fieldDescriptor, valueStr));
        }

        @Override
        void writeKeyVoid(MVO.MvoField field, Object key) {
            throw new RuntimeException();
        }
    }

    final class M1Logger extends MonaxisMapLogger {

        @Override
        void write(MVO.MvoField field, Object key, Object value) {
            String fieldDescriptor = getFieldDescriptor(EntryType.M1, field);
            String keyStr = encodeValue(key);
            String valueStr = encodeValue(value);

            loggingStream_.println(concat(fieldDescriptor, keyStr, valueStr));
        }

        @Override
        void writeKeyVoid(MVO.MvoField field, Object key) {
            String fieldDescriptor = getFieldDescriptor(EntryType.M1, field);
            String keyStr = encodeValue(key);
            String valueStr = "#void";

            loggingStream_.println(concat(fieldDescriptor, keyStr, valueStr));
        }
    }

    abstract class BinaxesMapLogger {

        public abstract void write(MVO.MvoField field, long time, Object key, Object value);
    }

    final class S2Logger extends BinaxesMapLogger {

        @Override
        public void write(MVO.MvoField field, long time, Object key, Object value) {
            boolean mode = ((Boolean) value).booleanValue();
            String fieldDescriptor
                    = getFieldDescriptor(mode ? EntryType.S2_ADD : EntryType.S2_REMOVE, field);
            String timeStr = hexLong(time);
            String valueStr = encodeValue(key);

            loggingStream_.println(concat(fieldDescriptor, timeStr, valueStr));
        }
    }

    final class M2Logger extends BinaxesMapLogger {

        @Override
        public void write(MVO.MvoField field, long time, Object key, Object value) {
            String fieldDescriptor = getFieldDescriptor(EntryType.M2, field);
            String timeStr = hexLong(time);
            String keyStr = encodeValue(key);
            String valueStr = encodeValue(value);

            loggingStream_.println(concat(fieldDescriptor, timeStr, keyStr, valueStr));
        }
    }

    abstract class BinaxesNMapLogger {

        private final BinaxesMapLogger entryLogger_ = new BinaxesMapLogger() {

            @Override
            public void write
                    (MVO.MvoField field, long time, Object key, Object value) {
                boolean mode = ((Boolean) value).booleanValue();
                processUnit(field, time, mode, processingKey_, key);
            }
        };

        private volatile Object processingKey_;

        final BinaxesMapLogger getEntryLogger() {
            return entryLogger_;
        }

        final void preprocessKey(Object key) {
            processingKey_ = key;
        }

        final void postprocessKey(Object key) {
            if (processingKey_ != key) {
                throw new RuntimeException();
            }

            processingKey_ = null;
        }

        abstract void processUnit
                (MVO.MvoField field, long time, boolean mode, Object key, Object value);
    }

    final class N2Logger extends BinaxesNMapLogger {

        @Override
        void processUnit
                (MVO.MvoField field, long time, boolean mode, Object key, Object value) {
            String fieldDescriptor
                    = getFieldDescriptor(mode ? EntryType.N2_ADD : EntryType.N2_REMOVE, field);
            String timeStr = hexLong(time);
            String keyStr = encodeValue(key);
            String valueStr = encodeValue(value);

            loggingStream_.println(concat(fieldDescriptor, timeStr, keyStr, valueStr));
        }
    }

    static final String encodeValue(Object o) {
        return ValueEncoder.encode(o);
    }

    private String getFieldDescriptor(EntryType entryType, MVO.MvoField field) {
        return concat
                (entryType.id,
                        field.getParent().getMvoId().getLocalStringExpression(),
                        hexInt(mvoMeta_.getFieldId(field)));
    }

    private static final class LoggingStream {

        private ByteArrayOutputStream contents_;
        private PrintStream bufferOut_;

        private PrintStream fileOut_;

        LoggingStream(File logFile) throws IOException {
            contents_ = new ByteArrayOutputStream();
            bufferOut_ = new PrintStream(new BufferedOutputStream(contents_));
            fileOut_ = TefFileUtils.newFilePrintStream(logFile, false);
        }

        final void print(String log) {
            bufferOut_.print(log);
            fileOut_.print(log);
        }

        final void println(String log) {
            bufferOut_.println(log);
            fileOut_.println(log);
        }

        final void flush() {
            bufferOut_.flush();
            fileOut_.flush();
        }

        final void close() {
            bufferOut_.close();
            fileOut_.close();
        }

        byte[] getContents() {
            return contents_.toByteArray();
        }
    }

    private MvoMeta mvoMeta_;

    private Transaction transaction_;

    private File logFile_;
    private LoggingStream loggingStream_;

    private F1Logger f1Logger_ = new F1Logger();
    private S1Logger s1Logger_ = new S1Logger();
    private M1Logger m1Logger_ = new M1Logger();
    private F2Logger f2Logger_ = new F2Logger();
    private S2Logger s2Logger_ = new S2Logger();
    private M2Logger m2Logger_ = new M2Logger();
    private N2Logger n2Logger_ = new N2Logger();

    JournalWriter(Transaction transaction) throws IOException {
        TefService tefservice = TefService.instance();
        mvoMeta_ = tefservice.getMvoMeta();

        transaction_ = transaction;

        logFile_ = new File
                (tefservice.getLogs().getJournalsDirectory(),
                        hexInt(transaction_.getId().serial)
                                + JournalEntry.RUNNING_JOURNAL_FILE_NAME_SUFFIX);
        if (logFile_.exists()) {
            throw new Error("log file already exists: " + logFile_.getAbsolutePath());
        }

        loggingStream_ = new LoggingStream(logFile_);
    }

    final void phase1Commit() {
        loggingStream_.print(JournalEntry.PHASE1_COMMIT_MARK);
        loggingStream_.flush();
    }

    final void phase2Commit() {
        loggingStream_.println(JournalEntry.PHASE2_COMMIT_MARK);
        loggingStream_.flush();

        loggingStream_.close();

        renameLogFileNameSuffix
                (transaction_.getCommittedTime(), JournalEntry.COMMITTED_JOURNAL_FILE_NAME_SUFFIX);
    }

    final void rollback() {
        loggingStream_.flush();
        loggingStream_.close();

        renameLogFileNameSuffix
                (System.currentTimeMillis(), JournalEntry.ROLLBACKED_JOURNAL_FILE_NAME_SUFFIX);
    }

    private void renameLogFileNameSuffix(long time, String suffix) {
        File file
                = JournalFileUtils.getJournalFilePath(transaction_.getId().serial, time, suffix);
        if (file.exists()) {
            throw new Error("log file already exists: " + file.getAbsolutePath());
        }

        if (!logFile_.renameTo(file)) {
            throw new Error();
        }

        logFile_.setReadOnly();
    }

    final byte[] getContents() {
        return loggingStream_.getContents();
    }

    final void close() {
        mvoMeta_ = null;

        transaction_ = null;

        loggingStream_ = null;

        f1Logger_ = null;
        s1Logger_ = null;
        m1Logger_ = null;
        f2Logger_ = null;
        s2Logger_ = null;
        m2Logger_ = null;
        n2Logger_ = null;
    }

    final F1Logger getF1Logger() {
        return f1Logger_;
    }

    final S1Logger getS1Logger() {
        return s1Logger_;
    }

    final M1Logger getM1Logger() {
        return m1Logger_;
    }

    final F2Logger getF2Logger() {
        return f2Logger_;
    }

    final S2Logger getS2Logger() {
        return s2Logger_;
    }

    final M2Logger getM2Logger() {
        return m2Logger_;
    }

    final N2Logger getN2Logger() {
        return n2Logger_;
    }

    final void printTransactionInfo() {
        loggingStream_.println(getTransactionDescriptor(transaction_));
    }

    final void printNewClass(Class<? extends MVO> clazz) {
        if (!mvoMeta_.isNewClass(clazz)) {
            throw new IllegalArgumentException();
        }

        Class<?> superclass = clazz.getSuperclass();
        if (MVO.class.isAssignableFrom(superclass)) {
            if (mvoMeta_.isNewClass((Class<? extends MVO>) superclass)) {
                printNewClass((Class<? extends MVO>) superclass);
            }
        }

        mvoMeta_.registerClass(clazz, null);

        int classId = mvoMeta_.getClassId(clazz);
        loggingStream_.println
                (concat(EntryType.CLASS_ID.id, hexInt(classId), clazz.getName()));
    }

    final void printNewField(Field field) {
        if (!mvoMeta_.isNewField(field)) {
            throw new IllegalArgumentException();
        }

        mvoMeta_.registerField(field, null);

        Class<? extends MVO> declaringClass = (Class<? extends MVO>) field.getDeclaringClass();
        if (mvoMeta_.isNewClass(declaringClass)) {
            printNewClass(declaringClass);
        }

        int classId = mvoMeta_.getClassId(declaringClass);
        int fieldId = mvoMeta_.getFieldId(field);
        loggingStream_.println
                (concat
                        (EntryType.FIELD_ID.id, hexInt(fieldId), hexInt(classId), field.getName()));
    }

    final void printNewObject(MVO mvo) {
        loggingStream_.println
                (concat
                        (EntryType.NEW.id,
                                hexInt(mvo.getTransactionLocalSerial()),
                                hexInt(mvoMeta_.getClassId(mvo.getClass()))));
    }

    final void printF0s(MVO mvo) {
        String mvoId = mvo.getMvoId().getLocalStringExpression();

        for (MVO.F0 f0Field : MvoMeta.selectMvoFieldObjects(mvo, MVO.F0.class)) {
            if (!TransactionContext.getContextTransaction().hasInitialized(f0Field)) {
                throw new IllegalStateException
                        ("f0 field not initialized: "
                                + f0Field.getParent().getClass().getName()
                                + "." + f0Field.getFieldName());
            }
            loggingStream_.println
                    (concat
                            (EntryType.F0.id,
                                    hexInt(mvo.getTransactionLocalSerial()),
                                    hexInt(mvoMeta_.getFieldId(f0Field)),
                                    encodeValue(f0Field.get())));
        }
    }

    final void printS0s(MVO mvo) {
        String mvoId = mvo.getMvoId().getLocalStringExpression();

        for (MVO.S0 s0Field : MvoMeta.selectMvoFieldObjects(mvo, MVO.S0.class)) {
            if (!TransactionContext.getContextTransaction().hasInitialized(s0Field)) {
                throw new IllegalStateException
                        ("s0 field not initialized: "
                                + s0Field.getParent().getClass().getName()
                                + "." + s0Field.getFieldName());
            }
            loggingStream_.println
                    (concat
                            (EntryType.S0.id,
                                    hexInt(mvo.getTransactionLocalSerial()),
                                    hexInt(mvoMeta_.getFieldId(s0Field)),
                                    encodeValue(s0Field.getAsArray())));
        }
    }

    static final String getTransactionDescriptor(Transaction transaction) {
        return concat
                (hexInt(transaction.getId().serial),
                        hexLong(transaction.getBeginTime()),
                        hexLong(transaction.getCommittedTime()),
                        BinaryStringUtils.encodeToUtf16HexStr(transaction.getTransactionDescription()));
    }

    static final String getFieldDescriptors(MVO o) {
        MvoMeta mvoMeta = TefService.instance().getMvoMeta();

        StringBuffer result = new StringBuffer();
        for (MVO.F0 field : MvoMeta.selectMvoFieldObjects(o, MVO.F0.class)) {
            result.append(result.length() == 0 ? "" : "/");
            result.append
                    (hexInt(mvoMeta.getFieldId(field))
                            + "=" + encodeValue(field.get()));
        }
        for (MVO.S0 field : MvoMeta.selectMvoFieldObjects(o, MVO.S0.class)) {
            result.append(result.length() == 0 ? "" : "/");
            result.append
                    (hexInt(mvoMeta.getFieldId(field))
                            + "=" + encodeValue(field.get()));
        }
        return result.toString();
    }

    private static String concat(String... strs) {
        StringBuilder result = new StringBuilder();
        for (String str : strs) {
            result.append(result.length() == 0 ? "" : " ");
            result.append(str);
        }
        return result.toString();
    }
}
