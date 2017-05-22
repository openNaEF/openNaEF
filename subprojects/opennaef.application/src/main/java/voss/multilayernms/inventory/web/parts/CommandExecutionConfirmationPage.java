package voss.multilayernms.inventory.web.parts;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.Model;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.MplsNmsLogCategory;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.PageUtil;
import voss.nms.inventory.util.UrlUtil;

import java.util.List;

public class CommandExecutionConfirmationPage extends WebPage {
    public static final String OPERATION_NAME = "CommandExecutionConfirmation";
    private final String editorName;

    public CommandExecutionConfirmationPage(final WebPage backPage, final String message, final List<? extends CommandBuilder> builders) {
        this(backPage, backPage, message, builders);
    }

    public CommandExecutionConfirmationPage(final WebPage backPage, final WebPage forwardPage, final String message, final List<? extends CommandBuilder> builders) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            add(UrlUtil.getTopLink("top"));
            Label messageLabel = new Label("confirmationMessage", Model.of(message));
            add(messageLabel);
            Link<Void> proceedLink = new Link<Void>("proceed") {
                private static final long serialVersionUID = 1L;

                public void onClick() {
                    LoggerFactory.getLogger(MplsNmsLogCategory.LOG_COMMAND).info("proceed by " + editorName);
                    CommandBuilder[] builders_ = builders.toArray(new CommandBuilder[0]);
                    try {
                        ShellConnector.getInstance().executes(builders_);
                        PageUtil.setModelChanged(forwardPage);
                        setResponsePage(forwardPage);
                        LoggerFactory.getLogger(MplsNmsLogCategory.LOG_COMMAND).info("proceeding completed successfully.");
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            add(proceedLink);
            Link<Void> abortLink = new Link<Void>("abort") {
                private static final long serialVersionUID = 1L;

                public void onClick() {
                    LoggerFactory.getLogger(MplsNmsLogCategory.LOG_COMMAND).info("abort by " + editorName);
                    setResponsePage(backPage);
                }
            };
            add(abortLink);

        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }
}