package tef.skelton.http;

import lib38k.misc.CommandlineParser;
import lib38k.net.httpd.Html;
import lib38k.net.httpd.HtmlForm;
import lib38k.net.httpd.HtmlTable;
import lib38k.net.httpd.HttpException;
import lib38k.net.httpd.HttpResponseContents;
import lib38k.parser.Ast;
import lib38k.parser.ParseException;
import lib38k.parser.StringToken;
import lib38k.parser.TokenStream;
import tef.MVO;
import tef.skelton.ObjectQueryExpression;
import tef.ui.http.DumpObjectResponse;
import tef.ui.http.TefHttpResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ObjectQueryExpressionHttp extends TefHttpResponse {

    private static final String ARGNAME_QUERY = "query";

    public ObjectQueryExpressionHttp() {
    }

    public HttpResponseContents getContents() throws HttpException {
        String query = getRequest().getParameter(ARGNAME_QUERY);
        if (query == null) {
            return buildQueryPage("");
        } else {
            return buildResultPage(query);
        }
    }

    private Html buildQueryPage(String defaultQuery) {
        Html html = new Html(UTF8);
        html.setTitle("tef object query expression");

        HtmlForm form = new HtmlForm(url(this.getClass()).urlStr());
        form.addCaption("query");
        form.addTextFieldTag(ARGNAME_QUERY, defaultQuery, 100);
        form.addSubmitButtonTag("execute");

        html.printHtmlTag(form);

        return html;
    }

    private Html buildResultPage(String query) {
        beginReadTransaction();

        Html result = buildQueryPage(query);
        result.printbr();

        List<ObjectQueryExpression.Row> queryresult;
        try {
            TokenStream tokens = StringToken.newTokenStream(Arrays.asList(CommandlineParser.parse(query)));
            queryresult
                = (List<ObjectQueryExpression.Row>) Ast.parse(ObjectQueryExpression.COLLECTION, tokens).evaluate();
        } catch (ParseException pe) {
            result.printbr("[error] " + pe.getMessage());
            return result;
        } catch (ObjectQueryExpression.EvaluationException ee) {
            result.printbr("[error] " + ee.getMessage());
            return result;
        }

        result.printbr("result: " + Integer.toString(queryresult.size()) + " rows.");

        if (queryresult.size() == 0) {
            return result;
        }

        int columnsCount = queryresult.get(0).columns().size();
        String[] headerColumns = new String[columnsCount];
        for (int i = 0; i < columnsCount; i++) {
            headerColumns[i] = Integer.toString(i);
        }

        HtmlTable table = new HtmlTable(headerColumns);
        table.setBorder(1);
        for (ObjectQueryExpression.Row row : queryresult) {
            List<String> columns = new ArrayList<String>();
            for (Object columnValue : row.columns()) {
                String columnStr;
                if (columnValue == null) {
                    columnStr = "";
                } else if (columnValue instanceof MVO) {
                    MVO mvo = (MVO) columnValue;
                    columnStr = DumpObjectResponse.getReferenceTag(
                                httpd(), mvo, mvo.getMvoId().getLocalStringExpression(), null, null)
                        .getTagString();
                } else {
                    columnStr = columnValue.toString();
                }
                columns.add(columnStr);
            }
            table.addRow(columns.toArray(new String[0]));
        }

        result.printHtmlTag(table);

        return result;
    }
}
