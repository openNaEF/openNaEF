package voss.multilayernms.inventory.web.parts;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.UrlUtil;

public class TransactionExecutionResultPage extends WebPage {
    public static final String OPERATION_NAME = "TransactionExecutionResult";

    public TransactionExecutionResultPage(final CustomTransaction transaction) {
        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);
            add(UrlUtil.getTopLink("top"));
            Label titleLabel = new Label("title", transaction.getTitle());
            add(titleLabel);
            Label headLabel = new Label("head", transaction.getHead());
            add(headLabel);

            final String msg;
            final WebPage nextPage;
            if (transaction.isSuccess()) {
                msg = transaction.getSuccessResultMessage();
                nextPage = transaction.getForwardPage();
            } else {
                msg = transaction.getFailResultMessage();
                nextPage = transaction.getBackPage();
            }
            Label messageLabel = new Label("resultMessage", msg);
            add(messageLabel);
            Link<Void> backLink = new Link<Void>("back") {
                private static final long serialVersionUID = 1L;

                public void onClick() {
                    setResponsePage(nextPage);
                }
            };
            backLink.setEnabled(nextPage != null);
            backLink.setVisible(nextPage != null);
            add(backLink);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }
}