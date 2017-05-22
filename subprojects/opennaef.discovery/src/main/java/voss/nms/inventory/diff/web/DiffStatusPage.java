package voss.nms.inventory.diff.web;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.slf4j.LoggerFactory;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.constants.LogConstants;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.DiffSetManagerImpl;
import voss.nms.inventory.util.UrlUtil;

public class DiffStatusPage extends WebPage {

    public DiffStatusPage() {
        ExternalLink reloadLink = UrlUtil.getLink("refresh", "diff");
        add(reloadLink);

        try {
            DiffSetManagerImpl manager = DiffSetManagerImpl.getInstance();
            ListView<String> statusTable = new ListView<String>("statusTable", manager.list()) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<String> item) {
                    try {
                        DiffSetManagerImpl manager = DiffSetManagerImpl.getInstance();
                        final String displayName = item.getModelObject();
                        Label nameLabel = new Label("name", Model.of(displayName));
                        item.add(nameLabel);

                        final DiffCategory cat = manager.getCategoryByDisplayName(displayName);

                        final String status = manager.getStatus(cat);
                        Label statusLabel = new Label("status", Model.of(status));
                        item.add(statusLabel);

                        final boolean isRunning = manager.isRunning(cat);
                        Link<Void> operationButton;
                        Label operationNameLabel;
                        if (isRunning) {
                            operationButton = new Link<Void>("operation") {
                                private static final long serialVersionUID = 1L;

                                @Override
                                public void onClick() {
                                    try {
                                        DiffSetManagerImpl.getInstance().abort(cat);
                                    } catch (Exception e) {
                                        throw ExceptionUtils.throwAsRuntime(e);
                                    }
                                    WebPage page = DiffStatusPage.this;
                                    page.modelChanged();
                                    setResponsePage(page);
                                }
                            };
                            operationNameLabel = new Label("operationName", Model.of("Abort"));
                        } else {
                            operationButton = new Link<Void>("operation") {
                                private static final long serialVersionUID = 1L;

                                @Override
                                public void onClick() {
                                    try {
                                        DiffSetManagerImpl.getInstance().createNewDiffInner(cat);
                                    } catch (Exception e) {
                                        throw ExceptionUtils.throwAsRuntime(e);
                                    }
                                    WebPage page = DiffStatusPage.this;
                                    page.modelChanged();
                                    setResponsePage(page);
                                }
                            };
                            operationNameLabel = new Label("operationName", Model.of("Start"));
                        }
                        item.add(operationButton);
                        operationButton.add(operationNameLabel);

                        Link<Void> depriveLockButton = new Link<Void>("deprive") {
                            private static final long serialVersionUID = 1L;

                            public void onClick() {
                                LoggerFactory.getLogger(LogConstants.DIFF_SERVICE).info("lock deprived.");
                            }
                        };
                        item.add(depriveLockButton);
                        String userName = "admin";
                        String userNameCaption = userName == null ? "" : " (" + userName + ")";
                        Label userNameLabel = new Label("userName", userNameCaption);
                        depriveLockButton.add(userNameLabel);

                        Label lastCommittedTimeLabel = new Label("lastCommittedTime", "2010/01/01 11:22:33");
                        item.add(lastCommittedTimeLabel);

                        Label resultLabel = new Label("result", "success");
                        item.add(resultLabel);

                        Link<Void> showLink = new Link<Void>("show") {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void onClick() {
                                WebPage page = new DiffListPage(cat, DiffStatusPage.this);
                                setResponsePage(page);
                            }
                        };
                        item.add(showLink);
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }

            };
            add(statusTable);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}