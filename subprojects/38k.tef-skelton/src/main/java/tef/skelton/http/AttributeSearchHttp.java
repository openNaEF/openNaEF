package tef.skelton.http;

import lib38k.net.httpd.Html;
import lib38k.net.httpd.HtmlForm;
import lib38k.net.httpd.HtmlTable;
import lib38k.net.httpd.HttpException;
import lib38k.net.httpd.HttpResponseContents;
import tef.MVO;
import tef.TefService;
import tef.skelton.Attribute;
import tef.skelton.Model;
import tef.skelton.SkeltonTefService;
import tef.skelton.UiTypeName;
import tef.ui.ValueRenderer;
import tef.ui.http.DumpObjectResponse;
import tef.ui.http.TefHttpResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AttributeSearchHttp extends TefHttpResponse {

    private static class ConditionEntry {

        final String key;
        final String value;

        ConditionEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public static final String ARGNAME_TYPE = "type";
    public static final String ARGNAME_KEY = "key";
    public static final String ARGNAME_PATTERN = "pattern";
    public static final String ARGNAME_NULL_RENDERING = "null-rendering";
    public static final String ARGNAME_BUTTON_TYPE = "button-type";
    public static final String BUTTON_NAME_ADD_CONDITION = "+";
    public static final String BUTTON_NAME_EXECUTE_QUERY = "実行";

    public AttributeSearchHttp() {
    }

    public HttpResponseContents getContents() throws HttpException {
        if (getArgType(false) == null) {
            return generateRequestTypeResponse();
        }

        beginReadTransaction();

        String buttonName = getRequest().getParameter(ARGNAME_BUTTON_TYPE);

        List<ConditionEntry> conditions = getConditionEntries();
        if (buttonName == null || buttonName.equals(BUTTON_NAME_ADD_CONDITION)) {
            return generateSearchConditionResponse();
        } else if (buttonName.equals(BUTTON_NAME_EXECUTE_QUERY)) {
            return generateSearchResultResponse();
        } else {
            throw new ResponseException("unknown operation: " + ARGNAME_BUTTON_TYPE + " " + buttonName);
        }
    }

    private List<ConditionEntry> getConditionEntries() throws HttpException {
        List<ConditionEntry> result = new ArrayList<ConditionEntry>();
        for (int i = 0; ; i++) {
            String key = getRequest().getParameter(ARGNAME_KEY + Integer.toString(i));
            String pattern = getRequest().getParameter(ARGNAME_PATTERN + Integer.toString(i));
            if (key == null) {
                break;
            }
            result.add(new ConditionEntry(key, pattern));
        }
        return result;
    }

    private HttpResponseContents generateRequestTypeResponse() {
        Html html = new Html(UTF8);
        html.setTitle("attribute search");

        HtmlForm.Selection typeSelection = new HtmlForm.Selection(ARGNAME_TYPE);
        for (UiTypeName typename : getUiTypeNames()) {
            typeSelection.addOption(typename.name(), typename.name());
        }

        HtmlForm form = new HtmlForm(url(this.getClass()).urlStr());
        form.addFormEntry(typeSelection);
        form.addSubmitButtonTag("ok");

        html.printHtmlTag(form);

        return html;
    }

    private List<UiTypeName> getUiTypeNames() {
        List<UiTypeName> result = new ArrayList<UiTypeName>();
        for (UiTypeName typename : SkeltonTefService.instance().uiTypeNames().instances()) {
            if (Model.class.isAssignableFrom(typename.type())) {
                result.add(typename);
            }
        }
        Collections.sort(result, new Comparator<UiTypeName>() {

            @Override public int compare(UiTypeName o1, UiTypeName o2) {
                return o1.name().compareTo(o2.name());
            }
        });
        return result;
    }

    private UiTypeName getArgType(boolean isRequired) throws HttpException {
        String typeName = getRequest().getParameter(ARGNAME_TYPE);
        if (typeName == null) {
            if (isRequired) {
                throw new ResponseException("エラー", "引数 " + ARGNAME_TYPE + " は必須です.");
            } else {
                return null;
            }
        }

        UiTypeName type = SkeltonTefService.instance().uiTypeNames().getByName(typeName);
        if (type == null) {
            throw new ResponseException("エラー", "型が見つかりません.");
        }

        if (! Model.class.isAssignableFrom(type.type())) {
            throw new ResponseException("エラー", "指定された型 " + typeName + " は対象外です.");
        }

        return type;
    }

    private HttpResponseContents generateSearchConditionResponse() throws HttpException {
        UiTypeName type = getArgType(true);

        Html html = new Html(UTF8);
        html.setTitle("attribute search: " + type.name());

        html.printbr("型 " + type.name());

        HtmlForm form = new HtmlForm(url(this.getClass()).urlStr());
        form.addHiddenTag(ARGNAME_TYPE, type.name());

        HtmlTable table = new HtmlTable("属性", "値パターン", "");
        List<ConditionEntry> conditions = getConditionEntries();
        for (int i = 0; i < conditions.size(); i++) {
            addConditionRow(table, type, i, conditions.get(i));
        }
        addConditionRow(table, type, conditions.size(), null);
        form.addHtmlTag(table);

        form.addCaption("null rendering");
        form.addTextFieldTag(ARGNAME_NULL_RENDERING, "", 25);

        form.addCaption("<br>");

        form.addSubmitButtonTag(ARGNAME_BUTTON_TYPE, BUTTON_NAME_EXECUTE_QUERY);

        html.printHtmlTag(form);

        return html;
    }

    private void addConditionRow(HtmlTable table, UiTypeName type, int index, ConditionEntry condition) {
        HtmlTable.Row conditionRow = table.addRow();

        HtmlForm.Selection keySelection = new HtmlForm.Selection(
            ARGNAME_KEY + Integer.toString(index),
            condition == null ? null : condition.key);
        for (Attribute<?, ?> attr
            : Attribute.getAttributes((Class<? extends Model>) type.type()))
        {
            keySelection.addOption(attr.getName(), attr.getName());
        }
        conditionRow.getCell(0).setFormEntry(keySelection);

        conditionRow.getCell(1).setFormEntry(
            new HtmlForm.InputTag.Text(
                ARGNAME_PATTERN + Integer.toString(index),
                condition == null ? null : condition.value, 
                50));

        if (condition != null) {
            conditionRow.getCell(2).setValue("");
        } else {
            conditionRow.getCell(2).setFormEntry(
                new HtmlForm.InputTag.Submit(ARGNAME_BUTTON_TYPE, BUTTON_NAME_ADD_CONDITION));
        }
    }

    private HttpResponseContents generateSearchResultResponse() throws HttpException {
        UiTypeName type = getArgType(true);

        Html html = new Html(UTF8);
        html.setTitle("attribute search: " + type.name());

        List<MVO> resultMvos = executeQuery();
        html.printbr("該当 " + resultMvos.size() + "件");

        html.printbr();

        html.printbr("型: " + type.name());
        html.printbr(Html.escape("null rendering: '" + getRequest().getParameter(ARGNAME_NULL_RENDERING) + "'"));

        ValueRenderer renderer = newValueRenderer();
        List<ConditionEntry> conditions = getConditionEntries();
        List<String> header = new ArrayList<String>();
        header.add("");
        for (ConditionEntry condition : conditions) {
            header.add(condition.key + ": " + condition.value);
        }
        HtmlTable table = new HtmlTable(header.toArray(new String[0]));
        table.setBorder(1);
        for (int i = 0; i < resultMvos.size(); i++) {
            MVO mvo = resultMvos.get(i);

            List<String> columns = new ArrayList<String>();
            columns.add(
                DumpObjectResponse.getReferenceTag(httpd(), mvo, Integer.toString(i + 1), null, null)
                .getTagString());
            for (ConditionEntry condition : conditions) {
                String valueStr = renderer.render(((Model) mvo).getValue(condition.key));
                columns.add(valueStr == null ? "" : Html.escape(valueStr));
            }
            table.addRow(columns.toArray(new String[0]));
        }
        html.printHtmlTag(table);

        return html;
    }

    private List<MVO> executeQuery() throws HttpException {
        UiTypeName type = getArgType(true);
        List<ConditionEntry> conditions = getConditionEntries();
        ValueRenderer renderer = newValueRenderer();

        List<MVO> result = new ArrayList<MVO>();
        for (MVO mvo : TefService.instance().getMvoRegistry().list()) {
            if (matches(type, conditions, renderer, mvo)) {
                result.add(mvo);
            }
        }
        return result;
    }

    private boolean matches(UiTypeName type, List<ConditionEntry> conditions, ValueRenderer renderer, MVO mvo) {
        if (! type.type().isInstance(mvo)) {
            return false;
        }

        for (ConditionEntry condition : conditions) {
            Object value = ((Model) mvo).getValue(condition.key);
            String renderedValue = renderer.render(value);

            if (condition.value == null
                ? renderedValue != null 
                : ! renderedValue.matches(condition.value))
            {
                return false;
            }
        }

        return true;
    }

    private ValueRenderer newValueRenderer() throws HttpException {
        return new ValueRenderer() {

            private final String nullValue_;
            {
                String nullValue = getRequest().getParameter(ARGNAME_NULL_RENDERING);
                nullValue_ = nullValue == null ? "" : nullValue;
            }

            @Override protected String renderAsNull() {
                return nullValue_;
            }
        };
    }
}
