package voss.multilayernms.inventory.lock;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.MplsNmsLogCategory;
import voss.multilayernms.inventory.lock.LockManager.LockedElement;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.UrlUtil;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class LockEditPage extends WebPage {
    private static final String OPERATION_NAME = "LockEdit";
    private final String editorName;
    private final List<Selector> checks = new ArrayList<Selector>();

    public LockEditPage() {
        try {
            final SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            final LockManager manager = LockManager.getInstance();
            for (LockedElement lock : manager.listLocks()) {
                Selector selector = new Selector();
                selector.element = lock;
                this.checks.add(selector);
            }

            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            BookmarkablePageLink<Void> refresh = new BookmarkablePageLink<Void>("refresh", LockEditPage.class);
            add(refresh);

            Form<Void> lockEditForm = new Form<Void>("editLock");
            add(lockEditForm);

            FeedbackPanel feedback = new FeedbackPanel("feedback");
            lockEditForm.add(feedback);

            ListView<Selector> lockList = new ListView<Selector>("locks", this.checks) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<Selector> item) {
                    Selector selector = item.getModelObject();
                    LockedElement elem = selector.element;
                    CheckBox cbox = new CheckBox("selected", new PropertyModel<Boolean>(selector, "checked"));
                    item.add(cbox);
                    item.add(new Label("name", elem.getCaption()));
                    item.add(new Label("time", df.format(elem.getLockedDate())));
                    item.add(new Label("owner", elem.getEditorName()));
                }
            };
            lockEditForm.add(lockList);

            SubmitLink proceed = new SubmitLink("unlock") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    Logger log = LoggerFactory.getLogger(MplsNmsLogCategory.LOG_COMMAND);
                    LockManager manager = LockManager.getInstance();
                    log.info("unlocked by " + editorName);
                    for (Selector selector : checks) {
                        if (!selector.checked) {
                            continue;
                        }
                        LockedElement elem = selector.element;
                        log.info("unlocking: " + elem.getTarget().toString() + " (" + elem.getCaption() + ")");
                        manager.unlock(elem.getTarget());
                        if (null == manager.getLockUser(elem.getTarget())) {
                            log.info("unlock success.");
                        } else {
                            log.info("unlock fail.");
                        }
                    }
                    setResponsePage(LockEditPage.class);
                }
            };
            lockEditForm.add(proceed);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private static class Selector implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean checked = false;
        public LockedElement element = null;
    }

}