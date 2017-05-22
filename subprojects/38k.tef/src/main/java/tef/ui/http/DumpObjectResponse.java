package tef.ui.http;

import lib38k.net.httpd.*;
import tef.*;
import tef.ui.ClassNameMapper;
import tef.ui.TefDateFormatter;
import tef.ui.ValueRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.List;

public class DumpObjectResponse extends TefHttpResponse {

    private static abstract class ValueDifferenceComparator {

        private ValueRenderer plainRenderer_ = new ValueRenderer() {

            protected String renderAsMvo(MVO mvo) {
                return mvo.getMvoId().getLocalStringExpression();
            }

            protected String renderAsString(String value) {
                return value == null
                        ? "null"
                        : "'" + value + "'";
            }
        };

        protected abstract Object getValue() throws Exception;

        boolean isEqual(TransactionId.W previousTransactionId) {
            try {
                String targetValue = plainRenderer_.render(getValue());

                TransactionId.W backupedTargetVersion
                        = TransactionContext.getTargetVersion();
                try {
                    TransactionContext.setTargetVersion(previousTransactionId);

                    String previousValue = plainRenderer_.render(getValue());

                    return targetValue.equals(previousValue);
                } finally {
                    TransactionContext.setTargetVersion(backupedTargetVersion);
                }
            } catch (Exception e) {
                return true;
            }
        }
    }

    public static final int DEFAULT_HISTORY_SIZE = 25;

    private static final String ARGNAME_ID = "id";
    private static final String ARGNAME_TRANSACTION = "transaction";
    private static final String ARGNAME_HISTORY_SIZE = "histories";
    private static final String ARGNAME_DETAILED_BINAXESSET_ENTRY_ID = "s2detail";

    public static HtmlATag getReferenceTag
            (HttpServer httpd,
             MVO target,
             String caption,
             TransactionId.W transaction,
             Integer historySize) {
        return getReferenceTag(httpd, target, caption, transaction, historySize, null);
    }

    public static HtmlATag getReferenceTag
            (HttpServer httpd,
             MVO target,
             String caption,
             TransactionId.W transaction,
             Integer historySize,
             String fragmentId) {
        HttpUrl url = httpd.url(DumpObjectResponse.class);
        url.addQueryParam(ARGNAME_ID, target.getMvoId().getLocalStringExpression());
        if (transaction != null) {
            url.addQueryParam(ARGNAME_TRANSACTION, transaction.getIdString());
        }
        if (historySize != null) {
            url.addQueryParam(ARGNAME_HISTORY_SIZE, historySize.toString());
        }
        if (fragmentId != null) {
            url.addQueryParam(ARGNAME_DETAILED_BINAXESSET_ENTRY_ID, fragmentId);
            url.fragmentPart(fragmentId);
        }

        return new HtmlATag(caption).href(url);
    }

    public static interface MethodInvoker {

        public boolean isAdaptive(MVO object);

        public String getName();

        public Object invokeMethod(MVO object);
    }

    private static final List<MethodInvoker> methodInvokers__ = new ArrayList<MethodInvoker>();

    public static void addMethodInvoker(MethodInvoker methodInvoker) {
        methodInvokers__.add(methodInvoker);
    }

    private static class HtmlTagValueRenderer extends ValueRenderer {

        private final HttpServer httpd_;
        private int historySize_ = DEFAULT_HISTORY_SIZE;
        private boolean tagUsed_;
        private String preTag_ = "";
        private String postTag_ = "";

        HtmlTagValueRenderer(HttpServer httpd) {
            httpd_ = httpd;
        }

        void setHistorySize(int historySize) {
            historySize_ = historySize;
        }

        void setTag(String preTag, String postTag) {
            preTag_ = preTag;
            postTag_ = postTag;
        }

        void resetTag() {
            setTag("", "");
        }

        public String render(Object value) {
            try {
                tagUsed_ = false;
                String rendered = super.render(value);
                return tagUsed_
                        ? rendered
                        : preTag_ + rendered + postTag_;
            } finally {
                tagUsed_ = false;
            }
        }

        protected String renderAsMvo(MVO mvo) {
            try {
                String renderedValue = super.renderAsMvo(mvo);
                return getReferenceTag
                        (httpd_,
                                mvo,
                                preTag_ + renderedValue + postTag_,
                                TransactionContext.getTargetVersion(),
                                historySize_)
                        .getTagString();
            } finally {
                tagUsed_ = true;
            }
        }

        protected String renderAsString(String value) {
            return "'" + value + "'";
        }
    }

    private HtmlTagValueRenderer valueRenderer_;

    private static final Set<Field> fieldsToHide__ = new HashSet<Field>();

    public static void addFieldToHide(Class<?> clazz, String fieldName) {
        Field field;
        try {
            field = clazz.getDeclaredField(fieldName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (field == null) {
            throw new IllegalArgumentException
                    ("no such field: class=" + clazz.getName()
                            + ", field=" + fieldName);
        }

        fieldsToHide__.add(field);
    }

    public DumpObjectResponse() {
    }

    public HttpResponseContents getContents() throws HttpException {
        valueRenderer_ = new HtmlTagValueRenderer(httpd());

        String mvoIdStr = getRequest().getParameter(ARGNAME_ID);
        String transactionStr = getRequest().getParameter(ARGNAME_TRANSACTION);
        valueRenderer_.setHistorySize(getHistorySize());

        String transactionDescription
                = ((TefHttpConnection) getRequest().getConnection())
                .getTransactionDescription(getRequest());
        TransactionContext.beginReadTransaction(transactionDescription);

        try {
            TransactionId.W version;
            if (transactionStr == null) {
                version = null;
            } else {
                version = (TransactionId.W) TransactionId.getInstance(transactionStr);
                if (TransactionContext.getTransactionCommittedTime(version) == null) {
                    throw new HttpRequest.RequestException
                            ("no such committed transaction: " + transactionStr);
                }
            }

            MVO object = getMvo(mvoIdStr);
            if (object == null) {
                throw new ResponseException("error", "no such object: " + mvoIdStr);
            }

            Html result = new Html(UTF8);
            result.setTitle("MVO Browser: " + mvoIdStr);

            writeBody(result, object, version);

            return result;
        } finally {
            TransactionContext.close();
        }
    }

    private MVO getMvo(String mvoIdStr) {
        MVO.MvoId mvoId = MVO.MvoId.getInstanceByLocalId(mvoIdStr);
        return TefService.instance().getMvoRegistry().get(mvoId);
    }

    private Integer getHistorySize() throws HttpException {
        String historySizeStr = getRequest().getParameter(ARGNAME_HISTORY_SIZE);
        int historySize;
        try {
            historySize = historySizeStr == null
                    ? DEFAULT_HISTORY_SIZE
                    : Integer.parseInt(historySizeStr);
        } catch (NumberFormatException nfe) {
            return DEFAULT_HISTORY_SIZE;
        }
        return historySize < 1
                ? DEFAULT_HISTORY_SIZE
                : historySize;
    }

    private void writeBody(Html html, MVO object, TransactionId.W version)
            throws HttpException {
        TransactionId.W[] transactionIds = TransactionIdAggregator.getTransactionIds(object);

        if (version != null) {
            TransactionContext.setTargetVersion(version);
        }

        TransactionId.W previousTransactionId
                = getPredecessor(transactionIds, TransactionContext.getTargetVersion());

        printBodyHeader(html, object, transactionIds);

        html.printbr();

        HtmlTable table = new HtmlTable(2);
        table.setCyclicBackgroundColors(HtmlTable.DEFAULT_CYCLIC_BACKGROUND_COLORS);
        addMethodRows(table, object, previousTransactionId);
        addFieldRows(table, object, previousTransactionId);
        html.printHtmlTag(table);

        html.printbr();
    }

    private TransactionId.W getPredecessor(TransactionId.W[] txIds, TransactionId.W target) {
        TransactionId.W last = null;
        for (TransactionId.W txId : txIds) {
            if (txId.serial == target.serial) {
                return last;
            }
            last = txId;
        }
        return null;
    }

    private void printBodyHeader(Html html, MVO object, TransactionId.W[] txIds)
            throws HttpException {
        HtmlTable table = new HtmlTable(2);

        HtmlTable.Row classNameRow
                = table.addRow("class: ", ClassNameMapper.getRenderedClassName(object.getClass()));
        classNameRow.getCell(0).setHorizontalAlignment(HtmlTable.HorizontalAlignment.RIGHT);

        HtmlTable.Row objectIdRow
                = table.addRow("id: ", object.getMvoId().getLocalStringExpression());
        objectIdRow.getCell(0).setHorizontalAlignment(HtmlTable.HorizontalAlignment.RIGHT);

        HtmlTable.Row historyRow
                = table.addRow("history: ", getHistoryCellValue(object, txIds));
        historyRow.getCell(0).setHorizontalAlignment(HtmlTable.HorizontalAlignment.RIGHT);
        historyRow.getCell(0).setVerticalAlignment(HtmlTable.VerticalAlignment.TOP);

        html.printHtmlTag(table);

        html.printbr();

        html.printHtmlTag(getReferenceTag(httpd(), object, "latest", null, getHistorySize()));
        html.printbr();
    }

    private String getHistoryCellValue(MVO object, TransactionId.W[] txIds)
            throws HttpException {
        TransactionId.W targetVersion = TransactionContext.getTargetVersion();
        int targetVersionIndex = getIndex(txIds, targetVersion);
        boolean isTargetVersionInHistory = 0 <= targetVersionIndex;
        int historySize = getHistorySize();

        StringBuilder formerPart = new StringBuilder();
        int formerPartUpperIndex
                = (isTargetVersionInHistory ? targetVersionIndex : -targetVersionIndex - 1)
                - 1;
        int formerPartLowerIndex = Math.max(0, formerPartUpperIndex - historySize + 1);
        boolean hasMoreFormers = 0 < formerPartLowerIndex;
        if (hasMoreFormers) {
            formerPart.append
                    (getReferenceTag(httpd(), object, "&lt;&lt;first", txIds[0], historySize)
                            .getTagString());
            formerPart.append(" ");
            formerPart.append
                    (getReferenceTag
                            (httpd(), object, "&lt;more", txIds[formerPartLowerIndex - 1], historySize)
                            .getTagString());
            formerPart.append(" ");
        }
        for (int i = formerPartLowerIndex; i <= formerPartUpperIndex; i++) {
            TransactionId.W txid = txIds[i];
            formerPart.append
                    (getReferenceTag
                            (httpd(), object, getTransactionTimeStr(txid), txid, historySize)
                            .getTagString());
            formerPart.append(" ");
        }
        if (0 < formerPart.length()) {
            formerPart.append("<br>");
        }

        StringBuilder targetPart = new StringBuilder();
        targetPart.append("@");
        targetPart.append
                (isTargetVersionInHistory
                        ? (getReferenceTag
                        (httpd(),
                                object,
                                "<font color=red>" + getTransactionTimeStr(targetVersion) + "</font>",
                                targetVersion,
                                historySize)
                        .getTagString())
                        : getTransactionTimeStr(targetVersion));
        targetPart.append("(");
        targetPart.append
                (isTargetVersionInHistory
                        ? PrintStackTraceLinesResponse
                        .getReferenceTag(httpd(), targetVersion).getTagString()
                        : targetVersion.getIdString());
        targetPart.append(")");
        targetPart.append("<br>");

        StringBuilder latterPart = new StringBuilder();
        int latterPartLowerIndex
                = isTargetVersionInHistory ? targetVersionIndex + 1 : -targetVersionIndex - 1;
        int latterPartUpperIndex
                = Math.min(txIds.length - 1, latterPartLowerIndex + historySize - 1);
        boolean hasMoreLaters = latterPartUpperIndex < txIds.length - 1;
        for (int i = latterPartLowerIndex; i <= latterPartUpperIndex; i++) {
            TransactionId.W txid = txIds[i];
            latterPart.append
                    (getReferenceTag
                            (httpd(), object, getTransactionTimeStr(txid), txid, historySize)
                            .getTagString());
            latterPart.append(" ");
        }
        if (hasMoreLaters) {
            latterPart.append(" ");
            latterPart.append
                    (getReferenceTag
                            (httpd(), object, "more&gt;", txIds[latterPartUpperIndex + 1], historySize)
                            .getTagString());
            latterPart.append(" ");
            latterPart.append
                    (getReferenceTag
                            (httpd(), object, "last&gt;&gt;", txIds[txIds.length - 1], historySize)
                            .getTagString());
        }

        return formerPart.toString() + targetPart.toString() + latterPart.toString();
    }

    private int getIndex(TransactionId.W[] values, TransactionId.W value) {
        return Arrays.binarySearch(values, value, null);
    }

    private String getTransactionTimeStr(TransactionId.W txId) {
        long time = TransactionContext.getTransactionCommittedTime(txId).longValue();
        return DateTimeFormat.YMDHMSS_DOT.format(time);
    }

    private void addMethodRows
            (HtmlTable table, final MVO object, TransactionId.W previousTransactionId)
            throws HttpException {
        List<MethodInvoker> adaptiveMethods = new ArrayList<MethodInvoker>();
        for (MethodInvoker methodInvoker : methodInvokers__) {
            if (!methodInvoker.isAdaptive(object)) {
                continue;
            }

            adaptiveMethods.add(methodInvoker);
        }
        Collections.sort(adaptiveMethods, new Comparator<MethodInvoker>() {
            public int compare(MethodInvoker o1, MethodInvoker o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (final MethodInvoker methodInvoker : adaptiveMethods) {
            String methodName = methodInvoker.getName();
            String methodResultValueString
                    = getMethodResultValueString
                    (methodInvoker, object, previousTransactionId);

            if (previousTransactionId != null) {
                boolean hasModified
                        = !new ValueDifferenceComparator() {
                    protected Object getValue() throws Exception {
                        return methodInvoker.invokeMethod(object);
                    }
                }.isEqual(previousTransactionId);
                if (hasModified) {
                    methodName = "<font color=red>" + methodName + "</font>";
                }
            }

            HtmlTable.Row row = table.addRow(methodName, methodResultValueString);
            row.setVerticalAlignment(HtmlTable.VerticalAlignment.TOP);
        }
    }

    private void addFieldRows
            (HtmlTable table, final MVO object, TransactionId.W previousTransactionId)
            throws HttpException {
        Field[] fields = getDumpTargetFields(object);
        Arrays.sort(fields, new Comparator<Field>() {
            public int compare(Field o1, Field o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (int i = 0; i < fields.length; i++) {
            final Field field = fields[i];

            String fieldName = field.getName();
            String fieldValueString
                    = getFieldValueString(field, object, previousTransactionId);

            if (previousTransactionId != null) {
                boolean hasModified
                        = !new ValueDifferenceComparator() {
                    protected Object getValue() throws Exception {
                        return field.get(object);
                    }
                }.isEqual(previousTransactionId);
                if (hasModified) {
                    fieldName = "<font color=red>" + fieldName + "</font>";
                }
            }

            HtmlTable.Row row = table.addRow(fieldName, fieldValueString);
            row.setVerticalAlignment(HtmlTable.VerticalAlignment.TOP);
        }
    }

    private String getMethodResultValueString
            (MethodInvoker methodInvoker, MVO object, TransactionId.W previousTransactionId)
            throws HttpException {
        Object methodInvokedResultValue;
        try {
            methodInvokedResultValue = methodInvoker.invokeMethod(object);
        } catch (Exception e) {
            StringBuffer resultAtError = new StringBuffer();
            resultAtError.append("<pre>");
            resultAtError.append("<font color=\"red\">");

            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(byteOut));
            try {
                byteOut.close();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            resultAtError.append(new String(byteOut.toByteArray()));

            resultAtError.append("</font>");
            resultAtError.append("</pre>");

            return resultAtError.toString();
        }

        return getEntryString(methodInvokedResultValue, previousTransactionId);
    }

    private String getFieldValueString
            (Field field, MVO object, TransactionId.W previousTransactionId)
            throws HttpException {
        Object value;
        try {
            value = field.get(object);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }

        return getEntryString(value, previousTransactionId);
    }

    private String getEntryString(Object value, TransactionId.W previousTransactionId)
            throws HttpException {
        if (value instanceof MonaxisMap) {
            return getMonaxisMapString((MonaxisMap<?, ?>) value, previousTransactionId);
        } else if (value instanceof BinaxesMap) {
            return getBinaxesMapString((BinaxesMap<?, ?>) value, previousTransactionId);
        } else if (value instanceof BinaxesField) {
            return getBinaxesFieldChangesString((BinaxesField<?>) value);
        } else if (value instanceof BinaxesSet) {
            return getBinaxesSetChangesString((BinaxesSet<?>) value);
        } else {
            return getValueString(value);
        }
    }

    private <K, V> String getMonaxisMapString
            (final MonaxisMap<K, V> map, TransactionId.W previousTransactionId) {
        StringBuffer result = new StringBuffer();

        result.append("<ul>");
        for (final K key : map.getKeys()) {
            boolean hasModified = false;
            if (previousTransactionId != null) {
                hasModified
                        = !new ValueDifferenceComparator() {
                    protected Object getValue() throws Exception {
                        return map.get(key);
                    }
                }.isEqual(previousTransactionId);
            }

            result.append("<li>");
            result.append
                    (hasModified ? getRedValueString(key) : getValueString(key));
            result.append(": ");
            result.append(getValueString(map.get(key)));
            result.append("</li>");
        }
        result.append("</ul>");

        return result.toString();
    }

    private <K, V> String getBinaxesMapString
            (final BinaxesMap<K, V> map, TransactionId.W previousTransactionId) {
        StringBuffer result = new StringBuffer();

        result.append("<ul>");
        List<K> keys = map.getKeys();
        for (final K key : keys) {
            boolean hasModified = false;
            if (previousTransactionId != null) {
                hasModified
                        = !new ValueDifferenceComparator() {

                    protected Object getValue() throws Exception {
                        return map.getValueChanges(key);
                    }
                }.isEqual(previousTransactionId);
            }

            result.append("<li>");
            result.append(hasModified ? getRedValueString(key) : getValueString(key));
            result.append(": ");
            result.append("</li>");
            result.append(getBinaxesFieldChangesString(map.getValueChanges(key)));
        }
        result.append("</ul>");

        return result.toString();
    }

    private <T> String getBinaxesFieldChangesString(BinaxesField<T> value) {
        return getBinaxesFieldChangesString(value.getChanges());
    }

    private <T> String getBinaxesFieldChangesString(SortedMap<Long, T> changes) {
        StringBuffer result = new StringBuffer();

        result.append("<ul>");
        for (Map.Entry<Long, T> entry : changes.entrySet()) {
            Long time = entry.getKey();
            T value = entry.getValue();

            result.append("<li>");
            result.append(getAsFormattedDateString(time));
            result.append(": ");
            result.append(getValueString(value));
            result.append("</li>");
        }
        result.append("</ul>");

        return result.toString();
    }

    private <T> String getBinaxesSetChangesString(BinaxesSet<T> value)
            throws HttpException {
        Integer fieldId = value instanceof MVO.MvoField
                ? TefService.instance().getFieldId((MVO.MvoField) value)
                : null;

        List<String> detailedIds
                = getRequest().getParameterValues(ARGNAME_DETAILED_BINAXESSET_ENTRY_ID);

        StringBuffer result = new StringBuffer();

        result.append("<ul>");

        SortedMap<Long, List<T>> changes = value.getChanges();
        List<T> previousValues = new ArrayList<T>();
        for (Long time : changes.keySet()) {
            String fragmentId = fieldId == null
                    ? null
                    : fieldId.toString() + "." + Long.toHexString(time.longValue());
            List<T> values = changes.get(time);

            result.append
                    ("<li"
                            + (fragmentId == null ? "" : " id=\"" + fragmentId + "\"")
                            + ">");

            result.append(getAsFormattedDateString(time));
            result.append("(");
            result.append
                    (getReferenceTag
                            (httpd(),
                                    getMvo(getRequest().getParameter(ARGNAME_ID)),
                                    Integer.toString(values.size()),
                                    TransactionContext.getTargetVersion(),
                                    getHistorySize(),
                                    fragmentId)
                            .getTagString());
            result.append(")");

            result.append("<ul>");

            result.append(getChangeDiffString(previousValues, values));

            if (detailedIds != null && detailedIds.contains(fragmentId)) {
                result.append("<li>");
                result.append("values: ");
                result.append(getValueString(values));
                result.append("</li>");
            }

            result.append("</ul>");

            result.append("</li>");

            previousValues = values;
        }

        result.append("</ul>");

        return result.toString();
    }

    private <T> String getChangeDiffString(List<T> pre, List<T> post) {
        StringBuffer result = new StringBuffer();

        ObjectComparator objectComparator = new ObjectComparator();
        Collections.sort(pre, objectComparator);
        Collections.sort(post, objectComparator);

        List<T> increased = new ArrayList<T>();
        for (int i = 0; i < post.size(); i++) {
            if (Collections.binarySearch(pre, post.get(i), objectComparator) < 0) {
                increased.add(post.get(i));
            }
        }

        if (increased.size() > 0) {
            result.append("<li>");
            result.append("+" + increased.size() + ": ");
            result.append(getValueString(increased));
            result.append("</li>");
        }

        List<T> decreased = new ArrayList<T>();
        for (int i = 0; i < pre.size(); i++) {
            if (Collections.binarySearch(post, pre.get(i), objectComparator) < 0) {
                decreased.add(pre.get(i));
            }
        }

        if (decreased.size() > 0) {
            result.append("<li>");
            result.append("-" + decreased.size() + ": ");
            result.append(getValueString(decreased));
            result.append("</li>");
        }

        return result.toString();
    }

    private String getAsFormattedDateString(long time) {
        return TefDateFormatter.formatWithRawValue(new Date(time));
    }

    protected String getValueString(Object value) {
        return getValueString(value, "", "");
    }

    protected String getValueString(Object value, String preTag, String postTag) {
        synchronized (valueRenderer_) {
            try {
                valueRenderer_.setTag(preTag, postTag);
                String renderedValue = valueRenderer_.render(value);
                return renderedValue;
            } finally {
                valueRenderer_.resetTag();
            }
        }
    }

    private String getRedValueString(Object value) {
        return getValueString(value, "<font color=red>", "</font>");
    }

    protected Field[] getDumpTargetFields(MVO object) {
        List<Field> result = new ArrayList<Field>();
        for (Field field : getReflectionFields(object.getClass())) {
            if ((field.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
                continue;
            }

            if (fieldsToHide__.contains(field)) {
                continue;
            }

            Class type = field.getType();
            if (!isDumpTargetClass(type)) {
                continue;
            }

            field.setAccessible(true);

            result.add(field);
        }

        return result.toArray(new Field[0]);
    }

    private List<Field> getReflectionFields(Class<? extends MVO> clazz) {
        List<Field> result = new ArrayList<Field>();
        List<Field> fields = Arrays.asList(clazz.getDeclaredFields());
        if (clazz == MVO.class) {
            return fields;
        }
        result.addAll(getReflectionFields((Class<? extends MVO>) clazz.getSuperclass()));
        result.addAll(fields);
        return result;
    }

    private boolean isDumpTargetClass(Class type) {
        return tef.MVO.class.isAssignableFrom(type)
                || tef.MVO.MvoField.class.isAssignableFrom(type)
                || type.isPrimitive()
                || type == String.class
                || java.lang.Number.class.isAssignableFrom(type)
                || (type.isArray() && isDumpTargetClass(type.getComponentType()));
    }
}
