package voss.multilayernms.inventory.web.parts;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.LoggerFactory;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.MplsNmsLogCategory;
import voss.multilayernms.inventory.web.parts.CustomTransaction.ExtraInput;
import voss.nms.inventory.util.AAAWebUtil;

public class TransactionExecutionConfirmationPage extends WebPage {
    public static final String OPERATION_NAME = "TransactionExecutionConfirmation";
    private final String editorName;

    public TransactionExecutionConfirmationPage(final CustomTransaction transaction) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            Label titleLabel = new Label("title", transaction.getTitle());
            add(titleLabel);
            Label headLabel = new Label("head", transaction.getHead());
            add(headLabel);
            Label messageLabel = new Label("confirmationMessage", transaction.getConfirmationMessage());
            add(messageLabel);
            Form<Void> form = new Form<Void>("form");
            add(form);
            ListView<ExtraInput> extraInputList = new ListView<ExtraInput>("extras", transaction.getExtraInputs()) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<ExtraInput> item) {
                    final ExtraInput extra = item.getModelObject();
                    Label label = new Label("title", new PropertyModel<String>(extra, "title"));
                    item.add(label);
                    TextField<String> tf = new TextField<String>("extra", new PropertyModel<String>(extra, "value"));
                    item.add(tf);
                }
            };
            form.add(extraInputList);
            SubmitLink proceedLink = new SubmitLink("proceed") {
                private static final long serialVersionUID = 1L;

                public void onSubmit() {
                    LoggerFactory.getLogger(MplsNmsLogCategory.LOG_COMMAND).info("proceed by " + editorName);
                    transaction.execute();
                    TransactionExecutionResultPage page = new TransactionExecutionResultPage(transaction);
                    setResponsePage(page);
                }
            };
            form.add(proceedLink);
            Link<Void> abortLink = new Link<Void>("abort") {
                private static final long serialVersionUID = 1L;

                public void onClick() {
                    LoggerFactory.getLogger(MplsNmsLogCategory.LOG_COMMAND).info("abort by " + editorName);
                    setResponsePage(transaction.getBackPage());
                }
            };
            add(abortLink);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }
}