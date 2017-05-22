package tef;

import lib38k.io.IoUtils;
import lib38k.io.LineReader;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class MvoBulkDump {

    private static final class FileRegion {

        long position;
        int length;

        FileRegion(long position, int length) {
            this.position = position;
            this.length = length;
        }
    }

    static final File DUMP_DIR
            = new File(TefService.instance().getLogs().getJournalsDirectory(), "dump");

    static final String DUMP_FILE_NAME = "mvo-bulk.dump";
    static final String FIELD_TO_MVO_DUMP_POS_INDEX_FILE_NAME
            = "field-to-mvo-dump-pos.index";
    static final String MVO_TO_FIELD_POS_INDEX_FILE_NAME = "mvo-to-field-pos.index";

    private static final File DUMP_FILE = new File(DUMP_DIR, DUMP_FILE_NAME);
    private static final File FIELD_TO_MVO_DUMP_POS_INDEX_FILE
            = new File(DUMP_DIR, FIELD_TO_MVO_DUMP_POS_INDEX_FILE_NAME);
    private static final File MVO_TO_FIELD_POS_INDEX_FILE
            = new File(DUMP_DIR, MVO_TO_FIELD_POS_INDEX_FILE_NAME);

    private static final RandomAccessFile dumpIn__;
    private static final RandomAccessFile fieldIndexIn__;
    private static final Map<MVO, FileRegion> mvoToFieldPosIndex__
            = new HashMap<MVO, FileRegion>();

    private static final BinaxesEngine.BinaxesArgsCache binaxesArgsCache__
            = new BinaxesEngine.BinaxesArgsCache();

    static {
        File DUMP_DIR
                = IoUtils.initializeDirectory(TefService.instance().getWorkingDirectory(), "dump");

        if (DUMP_FILE.exists()) {
            try {
                dumpIn__ = new RandomAccessFile(DUMP_FILE, "r");
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        } else {
            dumpIn__ = null;
        }

        if (FIELD_TO_MVO_DUMP_POS_INDEX_FILE.exists()) {
            try {
                fieldIndexIn__
                        = new RandomAccessFile(FIELD_TO_MVO_DUMP_POS_INDEX_FILE, "r");
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        } else {
            fieldIndexIn__ = null;
        }
    }

    static boolean isMvoBulkDumpExisting() {
        if (DUMP_FILE.exists()) {
            if (!DUMP_FILE.isFile()) {
                throw new IllegalStateException(DUMP_FILE.getName() + " is not a file.");
            }
            if ((!FIELD_TO_MVO_DUMP_POS_INDEX_FILE.exists())
                    || (!FIELD_TO_MVO_DUMP_POS_INDEX_FILE.isFile())) {
                throw new IllegalStateException("illegal index-1 file.");
            }
            if ((!MVO_TO_FIELD_POS_INDEX_FILE.exists())
                    || (!MVO_TO_FIELD_POS_INDEX_FILE.isFile())) {
                throw new IllegalStateException("illegal index-2 file.");
            }

            return true;
        } else {
            return false;
        }
    }

    static void restore() throws IOException {
        Map<Transaction, String> transactionSpecObjectStrs = new HashMap<Transaction, String>();

        JournalEntryRestorer restorer = new JournalEntryRestorer(TefService.instance(), false);

        BufferedReader dumpFileReader = null;
        try {
            dumpFileReader = new BufferedReader(new FileReader(DUMP_FILE));

            restoreTransactions(dumpFileReader, restorer, transactionSpecObjectStrs);
            restoreMvos(dumpFileReader, restorer);
            restoreMvoF0Fields(dumpFileReader);
        } finally {
            if (dumpFileReader != null) {
                dumpFileReader.close();
            }
        }

        restoreTransactionSpecificObjects(transactionSpecObjectStrs);
        readIndexFile();

        readLeftJournals(restorer);
    }

    private static void restoreTransactions
            (BufferedReader dumpFileReader,
             JournalEntryRestorer restorer,
             Map<Transaction, String> transactionSpecObjectStrs)
            throws IOException {
        String line;
        while (!(line = dumpFileReader.readLine()).equals(".")) {
            String[] tokens = JournalEntryRestorer.tokenizeLine(line);

            String transactionSpecObjectStr = tokens[3];
            tokens[3] = "null";

            restorer.restoreTransaction(tokens);
            transactionSpecObjectStrs.put
                    (TransactionContext.getContextTransaction(), transactionSpecObjectStr);

            TransactionContext.commit();
        }
    }

    private static void restoreMvos(BufferedReader dumpFileReader, JournalEntryRestorer restorer)
            throws IOException {
        String line;
        while (!(line = dumpFileReader.readLine()).equals(".")) {
            String[] tokens = JournalEntryRestorer.tokenizeLine(line);

            TransactionId.W transactionId
                    = (TransactionId.W) TransactionId.getInstance(tokens[0]);
            int transactionLocalSerial = Integer.parseInt(tokens[1], 16);
            int classId = Integer.parseInt(tokens[2], 16);

            restorer.processNew(transactionId, transactionLocalSerial, classId);
        }
    }

    private static void restoreMvoF0Fields(final BufferedReader dumpFileReader)
            throws IOException {
        try {
            String line;
            while (!(line = dumpFileReader.readLine()).equals(".")) {
                String[] tokens = JournalEntryRestorer.tokenizeLine(line);
                if (!tokens[1].equals("f0")) {
                    throw new IllegalStateException();
                }

                MVO.MvoId mvoId = MVO.MvoId.getInstanceByLocalId(tokens[0]);
                MVO mvo = TefService.instance().getMvoRegistry().get(mvoId);
                String[] f0Tokens = JournalEntryRestorer.tokenizeLine(tokens[2], '/');

                for (int i = 0; i < f0Tokens.length; i++) {
                    String[] f0EntryTokens
                            = JournalEntryRestorer.tokenizeLine(f0Tokens[i], '=');

                    MVO.F0 f0
                            = (MVO.F0) TefService.instance().getMvoMeta()
                            .resolveMvoFieldObject(mvo, Integer.parseInt(f0EntryTokens[0], 16));
                    Object value = resolveValue(f0EntryTokens[1]);
                    f0.initialize(value);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void restoreTransactionSpecificObjects
            (Map<Transaction, String> transactionSpecObjectStrs) {
        Transaction[] transactions = Transaction.getCommittedTransactions();
        for (int i = 0; i < transactions.length; i++) {
            Transaction transaction = transactions[i];
            String transactionSpecObjectStr = transactionSpecObjectStrs.get(transaction);
            transaction.setTransactionDescription(transactionSpecObjectStr);
        }
    }

    private static void readIndexFile() throws IOException {
        BufferedReader mvoToFieldPosIndexReader = null;
        try {
            mvoToFieldPosIndexReader
                    = new BufferedReader(new FileReader(MVO_TO_FIELD_POS_INDEX_FILE));

            String indexLine;
            while (!(indexLine = mvoToFieldPosIndexReader.readLine()).equals(".")) {
                String[] indexTokens = JournalEntryRestorer.tokenizeLine(indexLine);
                MVO.MvoId mvoId = MVO.MvoId.getInstanceByLocalId(indexTokens[0]);
                MVO mvo = TefService.instance().getMvoRegistry().get(mvoId);

                long filePos = Long.parseLong(indexTokens[1], 16);
                long endPos = Long.parseLong(indexTokens[2], 16);
                long length = endPos - filePos;
                if (length > Integer.MAX_VALUE) {
                    throw new RuntimeException();
                }
                mvoToFieldPosIndex__.put(mvo, new FileRegion(filePos, (int) length));
            }
        } finally {
            if (mvoToFieldPosIndexReader != null) {
                mvoToFieldPosIndexReader.close();
            }
        }
    }

    private static void readLeftJournals(JournalEntryRestorer restorer) throws IOException {
        Transaction[] restoredTransactions = Transaction.getCommittedTransactions();
        List<JournalEntry> journalEntries = new ArrayList<JournalEntry>();
        FullJournalEntries entries = new FullJournalEntries(TefService.instance());
        for (JournalEntry entry : entries.getJournalEntryIterable()) {
            journalEntries.add(entry);
        }
        if (restoredTransactions.length > journalEntries.size()) {
            throw new IllegalStateException
                    ("dumped-transactions:" + restoredTransactions.length
                            + ", journals:" + journalEntries.size());
        }
        int counter = 0;
        for (; counter < restoredTransactions.length; counter++) {
            Transaction restoredTransaction = restoredTransactions[counter];
            JournalEntry journalEntry = journalEntries.get(counter);
            if (restoredTransaction.getId().serial != journalEntry.getTransactionId()) {
                throw new IllegalStateException
                        ("sequence error: dump=" + restoredTransaction.getId().serial
                                + ", log=" + journalEntry.getTransactionId());
            }
        }
        for (; counter < journalEntries.size(); counter++) {
            restorer.restore(journalEntries.get(counter));
        }
        entries.close();
    }

    static synchronized void restoreF1(MVO.F1 f1) {
        String[] historyElementLines = getMvoFieldLines(f1);
        MonaxisEngine.BulkSetInfo[] bulkSetInfos
                = new MonaxisEngine.BulkSetInfo[historyElementLines.length];
        for (int i = 0; i < historyElementLines.length; i++) {
            String[] historyElementLineTokens
                    = JournalEntryRestorer.tokenizeLine(historyElementLines[i].trim());
            Transaction transaction
                    = Transaction.getTransaction
                    ((TransactionId.W) TransactionId
                            .getInstance(historyElementLineTokens[0]));
            Object value = resolveValue(historyElementLineTokens[1]);

            bulkSetInfos[i] = new MonaxisEngine.BulkSetInfo(transaction, value);
        }
        f1.bulkSet(bulkSetInfos);
    }

    static synchronized void restoreF2(MVO.F2 f2) {
        String[] historyElementLines = getMvoFieldLines(f2);
        BinaxesEngine.BulkSetInfo[] bulkSetInfos
                = new BinaxesEngine.BulkSetInfo[historyElementLines.length];
        for (int i = 0; i < historyElementLines.length; i++) {
            String[] historyElementLineTokens
                    = JournalEntryRestorer.tokenizeLine(historyElementLines[i].trim());
            Transaction transaction
                    = Transaction.getTransaction
                    ((TransactionId.W) TransactionId
                            .getInstance(historyElementLineTokens[0]));
            long targetTime = Long.parseLong(historyElementLineTokens[1], 16);
            Object value = resolveValue(historyElementLineTokens[2]);

            bulkSetInfos[i]
                    = new BinaxesEngine.BulkSetInfo(transaction, targetTime, value);
        }
        f2.bulkSet(bulkSetInfos, binaxesArgsCache__);
    }

    static synchronized void restoreS1(MVO.S1 s1) {
        restoreMonaxisMap(s1, MonaxisMapValueResolver.S1_RESOLVER);
    }

    static synchronized void restoreM1(MVO.M1 m1) {
        restoreMonaxisMap(m1, MonaxisMapValueResolver.M1_RESOLVER);
    }

    private static abstract class MonaxisMapValueResolver {

        abstract Object resolve(String valueStr);

        static final MonaxisMapValueResolver S1_RESOLVER
                = new MonaxisMapValueResolver() {
            final Object resolve(final String valueStr) {
                if (valueStr.equals("+")) {
                    return Boolean.TRUE;
                } else if (valueStr.equals("-")) {
                    return Boolean.FALSE;
                } else {
                    throw new RuntimeException
                            ("unknown operator-id: '" + valueStr + "'");
                }
            }
        };

        static final MonaxisMapValueResolver M1_RESOLVER
                = new MonaxisMapValueResolver() {
            final Object resolve(String valueStr) {
                return resolveValue(valueStr);
            }
        };
    }

    private static synchronized void restoreMonaxisMap
            (final MonaxisMapEngine monaxisMap, final MonaxisMapValueResolver valueResolver) {
        String[] historyElementLines = getMvoFieldLines((MVO.MvoField) monaxisMap);
        List<MonaxisMapEngine.MonaxisMapBulkSetInfo> bulkSetInfos
                = new ArrayList<MonaxisMapEngine.MonaxisMapBulkSetInfo>();
        for (int i = 0; i < historyElementLines.length; ) {
            String line = historyElementLines[i];
            Object key = resolveValue(line.trim());

            i++;

            int sequenceCount = 0;
            for (int j = i; j < historyElementLines.length; j++) {
                if (historyElementLines[j].charAt(1) != ' ') {
                    break;
                }
                sequenceCount++;
            }

            MonaxisEngine.BulkSetInfo[] valueBulkSetInfos
                    = new MonaxisEngine.BulkSetInfo[sequenceCount];
            for (int j = 0; j < sequenceCount; j++) {
                String[] historyElementLineTokens
                        = JournalEntryRestorer.tokenizeLine(historyElementLines[i + j].trim());
                Transaction transaction
                        = Transaction.getTransaction
                        ((TransactionId.W) TransactionId
                                .getInstance(historyElementLineTokens[0]));
                String valueStr = historyElementLineTokens[1];
                Object value = valueResolver.resolve(valueStr);

                valueBulkSetInfos[j]
                        = new MonaxisEngine.BulkSetInfo(transaction, value);
            }

            bulkSetInfos.add
                    (new MonaxisMapEngine.MonaxisMapBulkSetInfo(key, valueBulkSetInfos));

            i += sequenceCount;
        }

        monaxisMap.bulkSet
                (bulkSetInfos.toArray(new MonaxisMapEngine.MonaxisMapBulkSetInfo[0]));
    }

    static synchronized void restoreS2(MVO.S2 s2) {
        restoreBinaxesMap(s2, BinaxesMapValueResolver.S2_RESOLVER);
    }

    static synchronized void restoreM2(MVO.M2 m2) {
        restoreBinaxesMap(m2, BinaxesMapValueResolver.M2_RESOLVER);
    }

    private static abstract class BinaxesMapValueResolver {

        abstract Object resolve(String valueStr);

        static final BinaxesMapValueResolver S2_RESOLVER
                = new BinaxesMapValueResolver() {
            final Object resolve(final String valueStr) {
                if (valueStr.equals("+")) {
                    return Boolean.TRUE;
                } else if (valueStr.equals("-")) {
                    return Boolean.FALSE;
                } else {
                    throw new RuntimeException
                            ("unknown operator-id: '" + valueStr + "'");
                }
            }
        };

        static final BinaxesMapValueResolver M2_RESOLVER
                = new BinaxesMapValueResolver() {
            final Object resolve(String valueStr) {
                return resolveValue(valueStr);
            }
        };
    }

    static synchronized void restoreBinaxesMap
            (final BinaxesMapEngine binaxesMap, final BinaxesMapValueResolver valueResolver) {
        String[] historyElementLines = getMvoFieldLines((MVO.MvoField) binaxesMap);
        List<BinaxesMapEngine.BinaxesMapBulkSetInfo> bulkSetInfos
                = new ArrayList<BinaxesMapEngine.BinaxesMapBulkSetInfo>();
        for (int i = 0; i < historyElementLines.length; ) {
            String line = historyElementLines[i];
            Object key = resolveValue(line.trim());

            i++;

            int sequenceCount = 0;
            for (int j = i; j < historyElementLines.length; j++) {
                if (historyElementLines[j].charAt(1) != ' ') {
                    break;
                }
                sequenceCount++;
            }

            BinaxesEngine.BulkSetInfo[] valueBulkSetInfos
                    = new BinaxesEngine.BulkSetInfo[sequenceCount];
            for (int j = 0; j < sequenceCount; j++) {
                String[] historyElementLineTokens
                        = JournalEntryRestorer.tokenizeLine(historyElementLines[i + j].trim());
                Transaction transaction
                        = Transaction.getTransaction
                        ((TransactionId.W) TransactionId
                                .getInstance(historyElementLineTokens[0]));

                String valueStr = historyElementLineTokens[1];
                Object value = valueResolver.resolve(valueStr);

                long targetTime = Long.parseLong(historyElementLineTokens[2], 16);

                valueBulkSetInfos[j]
                        = new BinaxesEngine.BulkSetInfo(transaction, targetTime, value);
            }

            bulkSetInfos.add
                    (new BinaxesMapEngine.BinaxesMapBulkSetInfo(key, valueBulkSetInfos));

            i += sequenceCount;
        }

        binaxesMap.bulkSet
                (bulkSetInfos.toArray(new BinaxesMapEngine.BinaxesMapBulkSetInfo[0]),
                        binaxesArgsCache__);
    }

    private static synchronized String[] getMvoFieldLines(MVO.MvoField field) {
        try {
            MVO parent = field.getParent();
            String targetOid = parent.getMvoId().getLocalStringExpression();
            Integer targetFieldId = TefService.instance().getMvoMeta().getFieldId(field);

            FileRegion fileRegion = (FileRegion) getFilePos(field);
            if (fileRegion == null || fileRegion.length == 0) {
                return new String[0];
            }

            dumpIn__.seek(fileRegion.position);
            byte[] contents = new byte[fileRegion.length];
            dumpIn__.read(contents);
            LineReader lineReader = new LineReader.Array(contents);

            String fieldDescriptorLine = lineReader.readLine();
            String[] fieldDescriptorLineTokens
                    = JournalEntryRestorer.tokenizeLine(fieldDescriptorLine);
            String oid = fieldDescriptorLineTokens[0];
            String fieldId = fieldDescriptorLineTokens[1];
            if (!oid.equals(targetOid)) {
                return new String[0];
            }
            if (!fieldId.equals(targetFieldId)) {
                return new String[0];
            }

            List<String> result = new ArrayList<String>();
            String line;
            while ((line = lineReader.readLine()) != null) {
                if (line.charAt(0) != ' ') {
                    break;
                }
                result.add(line);
            }

            return result.toArray(new String[0]);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static synchronized FileRegion getFilePos(MVO.MvoField field)
            throws IOException {
        MVO parent = field.getParent();

        String oid = parent.getMvoId().getLocalStringExpression();
        Integer fieldId = TefService.instance().getMvoMeta().getFieldId(field);

        FileRegion index2FilePos = mvoToFieldPosIndex__.get(parent);
        fieldIndexIn__.seek(index2FilePos.position);
        byte[] fieldIndexContents = new byte[index2FilePos.length];
        fieldIndexIn__.read(fieldIndexContents);

        LineReader reader = new LineReader.Array(fieldIndexContents);

        while (true) {
            String line = reader.readLine();
            if (line.charAt(0) == '.') {
                return null;
            }

            String[] tokens = JournalEntryRestorer.tokenizeLine(line);

            String tokenOid = tokens[0];
            if (!tokenOid.equals(oid)) {
                return null;
            }

            String tokenFieldId = tokens[1];
            if (tokenFieldId.equals(fieldId)) {
                long position = Long.parseLong(tokens[2], 16);
                long endPosition = Long.parseLong(tokens[3], 16);
                long length = endPosition - position;
                if (length > Integer.MAX_VALUE) {
                    throw new RuntimeException();
                }
                return new FileRegion(position, (int) length);
            }
        }
    }

    private static JournalEntryRestorer restorer__;

    private static Object resolveValue(final String str) throws JournalRestorationException {
        if (restorer__ == null) {
            restorer__ = new JournalEntryRestorer(TefService.instance(), false);
        }
        return restorer__.resolveValue(str);
    }
}
