package tef.ui.http;

import lib38k.net.httpd.*;
import tef.MVO;
import tef.TefService;
import tef.TransactionContext;
import tef.ui.ClassNameMapper;
import tef.ui.ValueRenderer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.PatternSyntaxException;

public class SearchObjectResponse extends TefHttpResponse {

    public static final String ARGNAME_CLASS_NAME = "class";
    public static final String ARGNAME_GETTER_METHOD_NAME = "method";
    public static final String ARGNAME_RESULT_PATTERN = "pattern";
    public static final String ARGNAME_HISTORY_SIZE = "histories";

    static final String TITLE = "MVO Searcher";

    private static class ResultLine {

        MVO object;
        String methodResultString;
        int historySize;

        ResultLine(MVO object, String methodResultString, int historySize) {
            this.object = object;
            this.methodResultString = methodResultString;
            this.historySize = historySize;
        }

        String render(HttpServer httpd) {
            return DumpObjectResponse
                    .getReferenceTag(httpd, object, methodResultString, null, historySize)
                    .getTagString();
        }
    }

    private ValueRenderer valueRenderer_ = new ValueRenderer();

    public SearchObjectResponse() {
    }

    public HttpResponseContents getContents() throws HttpException {
        try {
            String className = getRequest().getParameter(ARGNAME_CLASS_NAME);
            String getterMethodName = getRequest().getParameter(ARGNAME_GETTER_METHOD_NAME);
            String resultPattern = getRequest().getParameter(ARGNAME_RESULT_PATTERN);
            String historySizeStr = getRequest().getParameter(ARGNAME_HISTORY_SIZE);

            if (className == null
                    || getterMethodName == null
                    || resultPattern == null) {
                return getSearchFormHtml();
            }

            int historySize;
            try {
                historySize = historySizeStr == null || historySizeStr.equals("")
                        ? DumpObjectResponse.DEFAULT_HISTORY_SIZE
                        : Integer.parseInt(historySizeStr);
            } catch (NumberFormatException nfe) {
                throw new HttpRequest.RequestException
                        ("invalid number format, " + ARGNAME_HISTORY_SIZE + ": " + historySizeStr);
            }
            if (historySize < 1) {
                throw new HttpRequest.RequestException
                        ("invalid value, " + ARGNAME_HISTORY_SIZE + ": " + historySizeStr);
            }

            Class<?> targetClass = ClassNameMapper.resolveClass(className);
            if (targetClass == null) {
                throw new HttpRequest.RequestException("error", "class not found.");
            }

            Method getterMethod;
            try {
                getterMethod = targetClass.getMethod(getterMethodName, new Class[0]);
            } catch (NoSuchMethodException nsme) {
                throw new HttpRequest.RequestException("error", "method not found.");
            }

            Html result = new Html(UTF8);
            result.setTitle(TITLE);

            String transactionDescription
                    = ((TefHttpConnection) getRequest().getConnection())
                    .getTransactionDescription(getRequest());
            TransactionContext.beginReadTransaction(transactionDescription);

            ResultLine[] searchResult
                    = searchObjects(targetClass, getterMethod, resultPattern, historySize);
            Arrays.sort(searchResult, new Comparator<ResultLine>() {
                public int compare(ResultLine o1, ResultLine o2) {
                    return o1.methodResultString.compareTo(o2.methodResultString);
                }
            });

            printResult(result, searchResult);

            return result;
        } catch (PatternSyntaxException pse) {
            throw new HttpRequest.RequestException
                    ("error", "pattern syntax error: " + pse.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            TransactionContext.close();
        }
    }

    private ResultLine[] searchObjects
            (Class<?> targetClass, Method getterMethod, String resultPattern, int historySize)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        MVO[] objects = getAllObjects(targetClass);

        List<ResultLine> result = new ArrayList<ResultLine>();
        for (int i = 0; i < objects.length; i++) {
            ResultLine line = getLine(getterMethod, resultPattern, objects[i], historySize);
            if (line == null) {
                continue;
            }

            result.add(line);
        }

        return result.toArray(new ResultLine[0]);
    }

    private ResultLine getLine
            (Method getterMethod, String resultPattern, MVO object, int historySize)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Object methodResult = getterMethod.invoke(object, new Object[0]);
        if (methodResult == null) {
            return null;
        }

        String methodResultString = valueRenderer_.render(methodResult);
        if (!methodResultString.matches(resultPattern)) {
            return null;
        }

        methodResultString
                = methodResultString == null || methodResultString.equals("")
                ? " " : methodResultString;

        return new ResultLine(object, methodResultString, historySize);
    }

    private MVO[] getAllObjects(Class<?> targetClass) {
        List<MVO> result = new ArrayList<MVO>();
        for (MVO mvo : TefService.instance().getMvoRegistry().list()) {
            if (targetClass.isAssignableFrom(mvo.getClass())) {
                result.add(mvo);
            }
        }
        return result.toArray(new MVO[0]);
    }

    private void printResult(Html html, ResultLine[] resultLines) {
        html.print("total: " + resultLines.length);
        html.printbr();

        for (int i = 0; i < resultLines.length; i++) {
            html.printbr(resultLines[i].render(httpd()));
        }
    }

    private Html getSearchFormHtml() {
        HtmlTable formTable = new HtmlTable(2);

        HtmlTable.Row classRow = formTable.addRow();
        classRow.getCell(0).setHorizontalAlignment(HtmlTable.HorizontalAlignment.RIGHT);
        classRow.getCell(0).setValue("class");
        classRow.getCell(1)
                .setFormEntry(new HtmlForm.InputTag.Text(ARGNAME_CLASS_NAME, null, 50));

        HtmlTable.Row methodRow = formTable.addRow();
        methodRow.getCell(0).setHorizontalAlignment(HtmlTable.HorizontalAlignment.RIGHT);
        methodRow.getCell(0).setValue("method");
        methodRow.getCell(1)
                .setFormEntry(new HtmlForm.InputTag.Text(ARGNAME_GETTER_METHOD_NAME, null, 50));

        HtmlTable.Row patternRow = formTable.addRow();
        patternRow.getCell(0).setHorizontalAlignment(HtmlTable.HorizontalAlignment.RIGHT);
        patternRow.getCell(0).setValue("pattern");
        patternRow.getCell(1)
                .setFormEntry(new HtmlForm.InputTag.Text(ARGNAME_RESULT_PATTERN, null, 50));

        HtmlTable.Row historySizeRow = formTable.addRow();
        historySizeRow.getCell(0).setHorizontalAlignment(HtmlTable.HorizontalAlignment.RIGHT);
        historySizeRow.getCell(0).setValue("histories");
        historySizeRow.getCell(1)
                .setFormEntry
                        (new HtmlForm.InputTag.Text
                                (ARGNAME_HISTORY_SIZE,
                                        Integer.toString(DumpObjectResponse.DEFAULT_HISTORY_SIZE),
                                        50));

        HtmlForm form = new HtmlForm(url(this.getClass()).urlStr());
        for (String tableLine : formTable.getTagLines()) {
            form.addCaption(tableLine);
        }
        form.addSubmitButtonTag("search");

        Html html = new Html(UTF8);
        html.setTitle(TITLE);
        html.printHtmlTag(form);
        return html;
    }
}
