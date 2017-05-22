package tef;

import lib38k.io.LineReader;
import lib38k.misc.BinaryStringUtils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

class JournalEntryRestorer {

    static class BrokenJournalException extends TefInitializationFailedException {

        BrokenJournalException(String message) {
            super(message);
        }
    }

    static enum EntryType {

        CLASS_ID("c-id"),
        FIELD_ID("f-id"),
        NEW("new"),
        F0("f0"),
        F1("f1"),
        F2("f2"),
        S0("s0"),
        S1_ADD("s1+"),
        S1_REMOVE("s1-"),
        S2_ADD("s2+"),
        S2_REMOVE("s2-"),
        M1("m1"),
        M2("m2"),
        N2_ADD("n2+"),
        N2_REMOVE("n2-");

        final String id;

        EntryType(final String id) {
            this.id = id;
        }

        static EntryType getById(String id) {
            return entryTypeIds__.get(id);
        }
    }

    private static final Map<String, EntryType> entryTypeIds__;

    static {
        entryTypeIds__ = new HashMap<String, EntryType>();
        for (EntryType entryType : EntryType.values()) {
            if (entryTypeIds__.get(entryType.id) != null) {
                throw new IllegalStateException(
                        entryType.id + " " + entryType + " " + entryTypeIds__.get(entryType.id));
            }
            entryTypeIds__.put(entryType.id, entryType);
        }
    }

    private final TefService tefservice_;
    private final MvoMeta mvoMeta_;
    private final MvoRegistry registry_;
    private final Map<String, Object> valueCache_;

    JournalEntryRestorer(final TefService tefservice, boolean isToUseCache) {
        tefservice_ = tefservice;
        mvoMeta_ = tefservice_.getMvoMeta();
        registry_ = tefservice_.getMvoRegistry();
        valueCache_ = isToUseCache
                ? new LinkedHashMap<String, Object>(1000, 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
                return 4096 < size();
            }
        }
                : null;
    }

    final Transaction restore(final JournalEntry journal) throws JournalRestorationException {
        final int txid = journal.getTransactionId();
        final byte[] contents = journal.getContents();

        final LineReader reader;
        final String transactionInfoLine;
        final String journalName = Integer.toString(txid, 16);
        try {
            reader = new LineReader.Array(contents);
            transactionInfoLine = reader.readLine();

            if (transactionInfoLine == null) {
                throw new BrokenJournalException("broken journal: " + journalName);
            }
        } catch (IOException ioe) {
            throw new JournalRestorationException(ioe);
        }

        final Transaction transaction;
        try {
            transaction = restoreTransaction(tokenizeLine(transactionInfoLine));
        } catch (Throwable t) {
            JournalRestorationException jre = new JournalRestorationException("invalid header", t);
            jre.setJournalName(journalName);
            throw jre;
        }

        final TransactionId.W transactionid = (TransactionId.W) transaction.getId();
        if (transactionid.serial != txid) {
            throw new IllegalStateException(
                    "transaction id mismatch: name:" + txid + ", transaction:" + transactionid.serial);
        }

        int lineCount = 1;
        while (true) {
            final String line;
            try {
                line = reader.readLine();
            } catch (IOException ioe) {
                throw new JournalRestorationException(ioe);
            }
            lineCount++;

            if (line == null) {
                TransactionContext.close();
                throw new BrokenJournalException("broken journal: " + journalName);
            }
            if (line.equals(JournalEntry.EOF)) {
                break;
            }
            final String[] tokens = tokenizeLine(line);
            final EntryType type = EntryType.getById(tokens[0]);

            try {
                switch (type) {
                    case CLASS_ID: {
                        processCid(tokens);
                        break;
                    }
                    case FIELD_ID: {
                        processFid(tokens);
                        break;
                    }
                    case NEW: {
                        final int transactionLocalSerial = Integer.parseInt(tokens[1], 16);
                        final int classId = Integer.parseInt(tokens[2], 16);
                        processNew(transactionid, transactionLocalSerial, classId);
                        break;
                    }
                    case F0: {
                        processF0(transactionid, tokens);
                        break;
                    }
                    case F1: {
                        processF1(tokens);
                        break;
                    }
                    case F2: {
                        processF2(tokens);
                        break;
                    }
                    case S0: {
                        processS0(transactionid, tokens);
                        break;
                    }
                    case S1_ADD: {
                        processS1(EntryType.S1_ADD, tokens);
                        break;
                    }
                    case S1_REMOVE: {
                        processS1(EntryType.S1_REMOVE, tokens);
                        break;
                    }
                    case S2_ADD: {
                        processS2(EntryType.S2_ADD, tokens);
                        break;
                    }
                    case S2_REMOVE: {
                        processS2(EntryType.S2_REMOVE, tokens);
                        break;
                    }
                    case M1: {
                        processM1(tokens);
                        break;
                    }
                    case M2: {
                        processM2(tokens);
                        break;
                    }
                    case N2_ADD: {
                        processN2(EntryType.N2_ADD, tokens);
                        break;
                    }
                    case N2_REMOVE: {
                        processN2(EntryType.N2_REMOVE, tokens);
                        break;
                    }
                    default:
                        throw new JournalRestorationException("unknown command line: " + line);
                }
            } catch (JournalRestorationException jre) {
                jre.setJournalName(journalName);
                jre.setLineCount(lineCount);
                throw jre;
            } catch (Throwable t) {
                JournalRestorationException jre = new JournalRestorationException(t);
                jre.setJournalName(journalName);
                jre.setLineCount(lineCount);
                throw jre;
            }
        }

        tefservice_.getTransactionDigestComputer().update(transactionid, contents);

        return transaction;
    }

    final Transaction restoreTransaction(final String[] tokens) {
        final int transactionId = Integer.parseInt(tokens[0], 16);
        final long beginTime = Long.parseLong(tokens[1], 16);
        final long committedTime = Long.parseLong(tokens[2], 16);
        final String transactionDesc = BinaryStringUtils.decodeFromUtf16HexStr(tokens[3]);

        return TransactionContext.restoreTransaction(transactionId, beginTime, committedTime, transactionDesc);
    }

    private final void processCid(String[] tokens) {
        final int classId = Integer.parseInt(tokens[1], 16);
        final String className = tokens[2];
        final Class<? extends MVO> clazz;
        try {
            clazz = (Class<? extends MVO>) Class.forName(className);
        } catch (Exception e) {
            throw new JournalRestorationException("no class found: " + className);
        }
        mvoMeta_.registerClass(clazz, classId);
        if (mvoMeta_.getClassId(clazz) != classId) {
            throw new IllegalStateException();
        }
    }

    private final void processFid(String[] tokens) {
        final int fieldId = Integer.parseInt(tokens[1], 16);
        final int classId = Integer.parseInt(tokens[2], 16);
        final String fieldName = tokens[3];
        final Class<? extends MVO> clazz = mvoMeta_.resolveClass(classId);
        final Field field;
        try {
            field = clazz.getDeclaredField(fieldName);
        } catch (Exception e) {
            throw new JournalRestorationException("no field found: " + clazz.getName() + "." + fieldName);
        }
        mvoMeta_.registerField(field, fieldId);
        if (mvoMeta_.getFieldId(field) != fieldId) {
            throw new IllegalStateException();
        }
    }

    final MVO processNew(final TransactionId.W transactionId, final int transactionLocalSerial, final int classId)
            throws JournalRestorationException {
        final MVO.MvoId mvoid = new MVO.MvoId(transactionId, transactionLocalSerial);
        final MVO result;
        try {
            final Class<? extends MVO> clazz = mvoMeta_.resolveClass(classId);
            final Constructor constructor = mvoMeta_.getMvoConstructor(clazz);
            result = (MVO) constructor.newInstance(new Object[]{mvoid});
        } catch (Exception e) {
            throw new JournalRestorationException(e);
        }

        if (!result.getMvoId().equals(mvoid)) {
            throw new JournalRestorationException("unexpected mvo-id: " + result.getClass().getName());
        }
        return result;
    }

    private final void processF0(final TransactionId.W transactionId, final String[] tokens)
            throws JournalRestorationException {
        final int transactionLocalSerial = Integer.parseInt(tokens[1], 16);
        final MVO.MvoId mvoid = new MVO.MvoId(transactionId, transactionLocalSerial);
        final MVO mvo = resolveMvo(mvoid);
        if (mvo == null) {
            throw new JournalRestorationException("f0-error: mvo not found. " + mvoid.getLocalStringExpression());
        }
        if (transactionLocalSerial != mvo.getTransactionLocalSerial()) {
            throw new JournalRestorationException("f0-error: target mismatch.");
        }

        final int fieldId = Integer.parseInt(tokens[2], 16);
        final Object value = resolveValue(tokens[3]);

        final MVO.F0 f0 = (MVO.F0) mvoMeta_.resolveMvoFieldObject(mvo, fieldId);
        f0.initialize(value);
    }

    private final void processS0(final TransactionId.W transactionId, final String[] tokens)
            throws JournalRestorationException {
        final int transactionLocalSerial = Integer.parseInt(tokens[1], 16);
        final MVO.MvoId mvoid = new MVO.MvoId(transactionId, transactionLocalSerial);
        final MVO mvo = resolveMvo(mvoid);
        if (mvo == null) {
            throw new JournalRestorationException("s0-error: mvo not found. " + mvoid.getLocalStringExpression());
        }
        if (transactionLocalSerial != mvo.getTransactionLocalSerial()) {
            throw new JournalRestorationException("s0-error: target mismatch.");
        }

        final int fieldId = Integer.parseInt(tokens[2], 16);
        final Object value = resolveValue(tokens[3]);

        final MVO.S0 s0 = (MVO.S0) mvoMeta_.resolveMvoFieldObject(mvo, fieldId);
        s0.initializeImpl(value);
    }

    private final void processF2(final String[] tokens)
            throws JournalRestorationException {
        final String mvoid = tokens[1];
        final int fieldId = Integer.parseInt(tokens[2], 16);
        final long when = Long.parseLong(tokens[3], 16);
        final String valueStr = tokens[4];

        final MVO.F2 f2 = (MVO.F2) resolveField(mvoid, fieldId);
        final Object value = resolveValue(valueStr);

        TransactionContext.setTargetTime(when);
        f2.set(value);
    }

    private final void processS2(final EntryType mode, final String[] tokens)
            throws JournalRestorationException {
        final String mvoid = tokens[1];
        final int fieldId = Integer.parseInt(tokens[2], 16);
        final long when = Long.parseLong(tokens[3], 16);
        final String valueStr = tokens[4];

        final MVO.S2 mvs = (MVO.S2) resolveField(mvoid, fieldId);
        final Object value = resolveValue(valueStr);

        TransactionContext.setTargetTime(when);
        switch (mode) {
            case S2_ADD:
                mvs.add(value);
                break;
            case S2_REMOVE:
                mvs.remove(value);
                break;
            default:
                throw new JournalRestorationException(mode.id);
        }
    }

    private final void processM2(final String[] tokens)
            throws JournalRestorationException {
        final String mvoid = tokens[1];
        final int fieldId = Integer.parseInt(tokens[2], 16);
        final long when = Long.parseLong(tokens[3], 16);
        final String keyStr = tokens[4];
        final String valueStr = tokens[5];

        final MVO.M2 m2 = (MVO.M2) resolveField(mvoid, fieldId);
        final Object key = resolveValue(keyStr);
        final Object value = resolveValue(valueStr);

        TransactionContext.setTargetTime(when);
        m2.put(key, value);
    }

    private final void processN2(final EntryType mode, final String[] tokens)
            throws JournalRestorationException {
        final String mvoid = tokens[1];
        final int fieldId = Integer.parseInt(tokens[2], 16);
        final long when = Long.parseLong(tokens[3], 16);
        final String keyStr = tokens[4];
        final String valueStr = tokens[5];

        final MVO.N2 n2 = (MVO.N2) resolveField(mvoid, fieldId);
        final Object key = resolveValue(keyStr);
        final Object value = resolveValue(valueStr);

        TransactionContext.setTargetTime(when);
        switch (mode) {
            case N2_ADD:
                n2.add(key, value);
                break;
            case N2_REMOVE:
                n2.remove(key, value);
                break;
            default:
                throw new JournalRestorationException(mode.id);
        }
    }

    private final void processF1(final String[] tokens)
            throws JournalRestorationException {
        final String mvoid = tokens[1];
        final int fieldId = Integer.parseInt(tokens[2], 16);
        final String valueStr = tokens[3];

        final MVO.F1 f1 = (MVO.F1) resolveField(mvoid, fieldId);
        final Object value = resolveValue(valueStr);

        f1.set(value);
    }

    private final void processS1(final EntryType mode, final String[] tokens)
            throws JournalRestorationException {
        final String mvoid = tokens[1];
        final int fieldId = Integer.parseInt(tokens[2], 16);
        final String valueStr = tokens[3];

        final MVO.S1 s1 = (MVO.S1) resolveField(mvoid, fieldId);
        final Object value = resolveValue(valueStr);

        switch (mode) {
            case S1_ADD:
                s1.add(value);
                break;
            case S1_REMOVE:
                s1.remove(value);
                break;
            default:
                throw new JournalRestorationException(mode.id);
        }
    }

    private final void processM1(final String[] tokens)
            throws JournalRestorationException {
        final String mvoid = tokens[1];
        final int fieldId = Integer.parseInt(tokens[2], 16);
        final String keyStr = tokens[3];
        final String valueStr = tokens[4];

        final MVO.M1 m1 = (MVO.M1) resolveField(mvoid, fieldId);
        final Object key = resolveValue(keyStr);

        if (valueStr.equals("#void")) {
            m1.remove(key);
        } else {
            final Object value = resolveValue(valueStr);

            m1.put(key, value);
        }
    }

    private final AccessOrderCache<String, DateTime> datetimesCache_ = new AccessOrderCache<String, DateTime>();

    Object resolveValue(final String str) throws JournalRestorationException {
        if (valueCache_ == null) {
            return resolveValueImpl(str);
        } else {
            Object result = valueCache_.get(str);
            if (result == null) {
                result = resolveValueImpl(str);
                valueCache_.put(str, result);
            }
            return result;
        }
    }

    private Object resolveValueImpl(final String str) throws JournalRestorationException {
        final char valueType = str.charAt(0);
        final String valueStr = str.substring(1, str.length());

        if (valueStr.equals("null")) {
            return null;
        }

        final Object result;

        switch (valueType) {
            case '&':
                result = resolveMvo(MVO.MvoId.getInstanceByLocalId(valueStr));
                break;
            case '#':
                result = getPrimitive(valueStr);
                break;
            case '$':
                result = BinaryStringUtils.decodeFromUtf16HexStr(valueStr);
                break;
            case '!':
                synchronized (datetimesCache_) {
                    if (datetimesCache_.containsKey(valueStr)) {
                        result = datetimesCache_.get(valueStr);
                    } else {
                        result = new DateTime(Long.parseLong(valueStr, 16));
                        datetimesCache_.put(valueStr, (DateTime) result);
                    }
                }
                break;
            case '%':
                result = getConst(valueStr);
                break;
            case '@':
                result = resolveAsArray(valueStr);
                break;
            case '~':
                final String transactionIdStr = "w" + valueStr;
                if (TransactionContext.getContextTransaction().getId().getIdString().equals(transactionIdStr)) {
                    result = TransactionContext.getContextTransaction().getId();
                } else {
                    result = Transaction
                            .getTransaction((TransactionId.W) TransactionId.getInstance(transactionIdStr))
                            .getId();
                }
                break;
            case '?':
                result = resolveAsExtraObject(valueStr);
                break;
            default:
                throw new JournalRestorationException(str);
        }

        if (result == null) {
            throw new JournalRestorationException(valueStr);
        }

        return result;
    }

    private final AccessOrderCache<String, Byte> bytesCache_ = new AccessOrderCache<String, Byte>();
    private final AccessOrderCache<String, Character> charsCache_ = new AccessOrderCache<String, Character>();
    private final AccessOrderCache<String, Double> doublesCache_ = new AccessOrderCache<String, Double>();
    private final AccessOrderCache<String, Float> floatsCache_ = new AccessOrderCache<String, Float>();
    private final AccessOrderCache<String, Integer> integersCache_ = new AccessOrderCache<String, Integer>();
    private final AccessOrderCache<String, Long> longsCache_ = new AccessOrderCache<String, Long>();
    private final AccessOrderCache<String, Short> shortsCache_ = new AccessOrderCache<String, Short>();

    private final Object getPrimitive(final String valueStr)
            throws JournalRestorationException {
        if (valueStr.equals("t")) {
            return Boolean.TRUE;
        } else if (valueStr.equals("f")) {
            return Boolean.FALSE;
        } else {
            final String valueBody = valueStr.substring(0, valueStr.length() - 1);
            final char suffix = valueStr.charAt(valueStr.length() - 1);

            switch (suffix) {
                case 'B':
                    synchronized (bytesCache_) {
                        if (bytesCache_.containsKey(valueBody)) {
                            return bytesCache_.get(valueBody);
                        } else {
                            Byte result = Byte.valueOf(valueBody, 16);
                            bytesCache_.put(valueBody, result);
                            return result;
                        }
                    }
                case 'C':
                    synchronized (charsCache_) {
                        if (charsCache_.containsKey(valueBody)) {
                            return charsCache_.get(valueBody);
                        } else {
                            Character result = new Character((char) Integer.parseInt(valueBody, 16));
                            charsCache_.put(valueBody, result);
                            return result;
                        }
                    }
                case 'D':
                    synchronized (doublesCache_) {
                        if (doublesCache_.containsKey(valueBody)) {
                            return doublesCache_.get(valueBody);
                        } else {
                            Double result = new Double(Double.longBitsToDouble(Long.parseLong(valueBody, 16)));
                            doublesCache_.put(valueBody, result);
                            return result;
                        }
                    }
                case 'F':
                    synchronized (floatsCache_) {
                        if (floatsCache_.containsKey(valueBody)) {
                            return floatsCache_.get(valueBody);
                        } else {
                            Float result = new Float(Float.intBitsToFloat(Integer.parseInt(valueBody, 16)));
                            floatsCache_.put(valueBody, result);
                            return result;
                        }
                    }
                case 'I':
                    synchronized (integersCache_) {
                        if (integersCache_.containsKey(valueBody)) {
                            return integersCache_.get(valueBody);
                        } else {
                            Integer result = Integer.valueOf(valueBody, 16);
                            integersCache_.put(valueBody, result);
                            return result;
                        }
                    }
                case 'L':
                    synchronized (longsCache_) {
                        if (longsCache_.containsKey(valueBody)) {
                            return longsCache_.get(valueBody);
                        } else {
                            Long result = Long.valueOf(valueBody, 16);
                            longsCache_.put(valueBody, result);
                            return result;
                        }
                    }
                case 'S':
                    synchronized (shortsCache_) {
                        if (shortsCache_.containsKey(valueBody)) {
                            return shortsCache_.get(valueBody);
                        } else {
                            Short result = Short.valueOf(valueBody, 16);
                            shortsCache_.put(valueBody, result);
                            return result;
                        }
                    }
            }
        }

        throw new JournalRestorationException(valueStr);
    }

    private final Object resolveAsArray(final String str) throws JournalRestorationException {
        if (str.startsWith("[B")) {
            return resolveAsByteArray(str);
        }
        final Class arrayClass;
        final String valuesStr;
        if (str.startsWith("[I")) {
            arrayClass = int.class;
            valuesStr = str.substring(3, str.length() - 1);
        } else {
            try {
                arrayClass = Class.forName(str.substring(2, str.indexOf(";")));
            } catch (Exception e) {
                if (getArrayDimension(str) > 1) {
                    throw new JournalRestorationException(e);
                } else {
                    throw new JournalRestorationException(e);
                }
            }
            valuesStr = str.substring(str.indexOf(";") + 2, str.length() - 1);
        }
        final String[] values = tokenizeLine(valuesStr, ',');

        final Object result;
        try {
            result = Array.newInstance(arrayClass, values.length);
        } catch (Exception e) {
            throw new JournalRestorationException(e);
        }

        for (int i = 0; i < values.length; i++) {
            final Object value = resolveValue(values[i]);
            try {
                Array.set(result, i, value);
            } catch (Exception e) {
                throw new JournalRestorationException(e);
            }
        }
        return result;
    }

    private final int getArrayDimension(String arrayComponentTypeDescr) {
        int index = 0;
        char c;
        while ((c = arrayComponentTypeDescr.charAt(index)) != 'L') {
            if (c != '[') {
                throw new IllegalStateException(arrayComponentTypeDescr);
            }
            index++;
        }
        return index;
    }

    private final byte[] resolveAsByteArray(final String str) {
        final String valuesStr = str.substring(3, str.length() - 1);
        return BinaryStringUtils.parseHexExpression(valuesStr);
    }

    private final Map<String, Enum> enumCache_ = new HashMap<String, Enum>();

    private final Enum getConst(final String enumDescription)
            throws JournalRestorationException {
        if (enumDescription.equals("null")) {
            return null;
        }
        Enum enumObject = enumCache_.get(enumDescription);
        if (enumObject != null) {
            return enumObject;
        }

        final int delimiterIndex = enumDescription.indexOf("-$");
        final String enumClassName = enumDescription.substring(0, delimiterIndex);
        final String enumId
                = BinaryStringUtils
                .decodeFromUtf16HexStr(enumDescription.substring(delimiterIndex + 2, enumDescription.length()));

        final Class<?> enumClass;
        try {
            enumClass = Class.forName(enumClassName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new JournalRestorationException(e);
        }

        if (!Enum.class.isAssignableFrom(enumClass)) {
            throw new JournalRestorationException("not a enum: " + enumClassName);
        }

        enumObject = Enum.valueOf(enumClass.asSubclass(Enum.class), enumId);

        enumCache_.put(enumDescription, enumObject);
        return enumObject;
    }

    private Object resolveAsExtraObject(String str) throws JournalRestorationException {
        final int delimiterIndex = str.indexOf(ExtraObjectCoder.ID_CONTENTS_DELIMITER);
        final String eocId = str.substring(0, delimiterIndex);
        final String valueStr = str.substring(delimiterIndex + 1);
        return tefservice_.getExtraObjectCoders().getById(eocId).decode(valueStr);
    }

    private final MVO.MvoField resolveField(final String mvoidStr, final int fieldId)
            throws JournalRestorationException {
        final MVO.MvoId mvoid = MVO.MvoId.getInstanceByLocalId(mvoidStr);
        final MVO mvo = resolveMvo(mvoid);
        if (mvo == null) {
            throw new JournalRestorationException("object not found: id=" + mvoidStr);
        }

        return mvoMeta_.resolveMvoFieldObject(mvo, fieldId);
    }

    static final String[] tokenizeLine(String journalLine) {
        return tokenizeLine(journalLine, ' ');
    }

    static final String[] tokenizeLine(String journalLine, char delimiter) {
        if (journalLine.length() == 0) {
            return new String[0];
        }
        final int numDelimiters = countOccurrence(journalLine, delimiter);
        final String[] result = new String[numDelimiters + 1];
        int counter = 0;
        for (int i = 0; i < numDelimiters; i++) {
            int index = journalLine.indexOf(delimiter, counter);
            result[i] = journalLine.substring(counter, index);
            counter = index + 1;
        }
        result[result.length - 1] = journalLine.substring(counter, journalLine.length());
        return result;
    }

    private static final int countOccurrence(String str, char c) {
        int result = 0;
        final int length = str.length();
        for (int i = 0; i < length; i++) {
            if (str.charAt(i) == c) {
                result++;
            }
        }
        return result;
    }

    private MVO resolveMvo(MVO.MvoId mvoid) {
        return registry_.get(mvoid);
    }
}
