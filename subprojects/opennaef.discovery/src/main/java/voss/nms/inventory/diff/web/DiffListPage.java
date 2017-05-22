package voss.nms.inventory.diff.web;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.nms.inventory.constants.LogConstants;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.DiffSet;
import voss.nms.inventory.diff.DiffSetManagerImpl;
import voss.nms.inventory.diff.DiffUnit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiffListPage extends WebPage {
    private final WebPage backPage;
    private final DiffCategory category;
    private final DiffListModel model;
    private final ListView<CheckWrapper> listView;

    public DiffListPage(DiffCategory category, WebPage backPage) {
        this.backPage = backPage;
        this.category = category;
        Link<Void> backLink = new Link<Void>("backLink") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                if (getBackPage() == null) {
                    return;
                }
                setResponsePage(getBackPage());
            }
        };
        add(backLink);

        Link<Void> reloadLink = new Link<Void>("reloadLink") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                getListModel().renew();
            }
        };
        add(reloadLink);

        Form<Void> diffListForm = new Form<Void>("diffListForm");
        add(diffListForm);

        SubmitLink getLockLink = new SubmitLink("getLock") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit() {
                log().debug("lock acquired.");
            }
        };
        diffListForm.add(getLockLink);

        SubmitLink releaseLockLink = new SubmitLink("releaseLock") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit() {
                log().debug("lock released.");
            }
        };
        diffListForm.add(releaseLockLink);

        this.model = new DiffListModel();
        this.listView = new ListView<CheckWrapper>("diffs", this.model) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(ListItem<CheckWrapper> item) {
                final CheckWrapper wrapper = item.getModelObject();
                final DiffUnit unit = wrapper.unit;
                RadioChoice<DiffOperation> operationSelection = new RadioChoice<DiffOperation>("radios",
                        new PropertyModel<DiffOperation>(wrapper, "op"),
                        Arrays.asList(DiffOperation.values()),
                        new ChoiceRenderer<DiffOperation>("description"));
                item.add(operationSelection);
                Label label1 = new Label("nodeName", Model.of(unit.getNodeName()));
                item.add(label1);
                Label label2 = new Label("ifName", Model.of(unit.getLocalName()));
                item.add(label2);
                Label label3 = new Label("content", Model.of(unit.getDescription()));
                item.add(label3);
                Label label4 = new Label("status", Model.of(unit.getStatus()));
                item.add(label4);
                Label label5 = new Label("sourceSystem", Model.of(unit.getSourceSystem()));
                item.add(label5);
            }
        };
        diffListForm.add(listView);

        SubmitLink applyLink = new SubmitLink("apply") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit() {
                log().debug("apply checked diff(s).");
                List<CheckWrapper> items = getListModel().getObject();
                for (CheckWrapper item : items) {
                    log().debug(item.op.name() + "=>[" + item.unit.getDescription() + "]");
                }
                getListModel().renew();
            }
        };
        diffListForm.add(applyLink);

        SubmitLink cancelLink = new SubmitLink("cancel") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit() {
                log().debug("cancel all checks.");
            }
        };
        diffListForm.add(cancelLink);
    }

    public WebPage getBackPage() {
        return this.backPage;
    }


    public DiffListModel getListModel() {
        if (this.model == null) {
            throw new IllegalStateException();
        }
        return this.model;
    }

    public DiffCategory getCategory() {
        return this.category;
    }

    private final Logger log() {
        return LoggerFactory.getLogger(LogConstants.DIFF_SERVICE);
    }

    private static final class CheckWrapper implements Serializable {
        private static final long serialVersionUID = 1L;
        public DiffOperation op = DiffOperation.KEEP;
        public DiffUnit unit = null;
    }

    private class DiffListModel extends AbstractReadOnlyModel<List<CheckWrapper>> {
        private static final long serialVersionUID = 1L;
        private List<CheckWrapper> units = new ArrayList<CheckWrapper>();

        public DiffListModel() {
            renew();
        }

        @Override
        public List<CheckWrapper> getObject() {
            return units;
        }

        public void renew() {
            try {
                List<CheckWrapper> wrappers = new ArrayList<CheckWrapper>();
                DiffSetManagerImpl manager = DiffSetManagerImpl.getInstance();
                DiffSet set = manager.getDiffSet(getCategory());
                for (DiffUnit unit : set.getDiffUnits()) {
                    CheckWrapper wrapper = new CheckWrapper();
                    wrapper.op = DiffOperation.KEEP;
                    wrapper.unit = unit;
                    wrappers.add(wrapper);
                }
                this.units.clear();
                this.units.addAll(wrappers);
            } catch (Exception e) {
                log().debug("failed to get manager.", e);
            }
        }
    }

    ;

    public static enum DiffOperation {
        APPLY("Apply"),
        DISCARD("Discard"),
        KEEP("Keep");
        private final String description;

        private DiffOperation(String descr) {
            this.description = descr;
        }

        public String getDescription() {
            return this.description;
        }
    }
}