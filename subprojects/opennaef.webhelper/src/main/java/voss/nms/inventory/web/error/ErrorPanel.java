package voss.nms.inventory.web.error;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import voss.nms.inventory.util.UrlUtil;

import java.util.List;


public class ErrorPanel extends Panel {
    private static final long serialVersionUID = 1L;

    public ErrorPanel(final String id, final WebPage backPage, String title, String header,
                      List<ErrorItem> items) {
        super(id);
        Label labelTitle = new Label("title", Model.of(title));
        add(labelTitle);
        Label labelHeader = new Label("header", Model.of(header));
        add(labelHeader);
        ListView<ErrorItem> messagesList = new ListView<ErrorItem>
                ("messages", items) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ErrorItem> item) {
                ErrorItem errorItem = item.getModelObject();
                item.add(new Label("key", Model.of(errorItem.title)));
                item.add(new ListView<String>("lines", errorItem.lines) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(ListItem<String> item) {
                        item.add(new Label("line", Model.of(item.getModelObject())));
                    }
                });
            }
        };
        add(messagesList);
        Link<Void> backLink = new Link<Void>("backLink") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                setResponsePage(backPage);
            }
        };
        backLink.setEnabled(backPage != null);
        add(backLink);
        ExternalLink topLink = UrlUtil.getTopLink("top");
        add(topLink);
    }
}