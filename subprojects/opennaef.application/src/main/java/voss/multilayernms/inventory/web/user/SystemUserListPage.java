package voss.multilayernms.inventory.web.user;

import naef.dto.SystemUserDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.SystemUserRenderer;
import voss.nms.inventory.model.RenewableAbstractReadOnlyModel;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;

public class SystemUserListPage extends WebPage {
    public static final String OPERATION_NAME = "SystemUserList";
    private final UserModel model;

    public SystemUserListPage() {
        this(new PageParameters());
    }

    public SystemUserListPage(PageParameters params) {
        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);
            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            BookmarkablePageLink<Void> refresh = new BookmarkablePageLink<Void>("refresh", SystemUserListPage.class);
            add(refresh);
            this.model = new UserModel();
            this.model.renew();
            Form<Void> form = new Form<Void>("form");
            add(form);
            Button addUserButton = new Button("addUser") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    WebPage page = new SystemUserEditPage(SystemUserListPage.this, null);
                    setResponsePage(page);
                }
            };
            form.add(addUserButton);
            ListView<SystemUserDto> nodeList = new ListView<SystemUserDto>("users", this.model) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<SystemUserDto> item) {
                    final SystemUserDto user = item.getModelObject();
                    final SystemUserRenderer renderer = new SystemUserRenderer(user);
                    item.add(new Label("userName", renderer.getName()));
                    item.add(new Label("caption", renderer.getCaption()));
                    item.add(new Label("accountStatus", renderer.getStatus()));
                    item.add(new Label("passwordHash", renderer.getPasswordHash()));
                    item.add(new Label("passwordExpire", renderer.getPasswordExpireString()));
                    item.add(new Label("lastLoginTime", "N/A"));
                    item.add(new Label("note", renderer.getNote()));
                    Link<Void> editUser = new Link<Void>("editUser") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            WebPage page = new SystemUserEditPage(SystemUserListPage.this, user);
                            setResponsePage(page);
                        }
                    };
                    item.add(editUser);
                }
            };
            add(nodeList);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    @Override
    protected void onModelChanged() {
        this.model.renew();
    }

    private class UserModel extends RenewableAbstractReadOnlyModel<List<SystemUserDto>> {
        private static final long serialVersionUID = 1L;
        private List<SystemUserDto> users = new ArrayList<SystemUserDto>();

        @Override
        public List<SystemUserDto> getObject() {
            return users;
        }

        @Override
        public void renew() {
            try {
                final MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
                List<SystemUserDto> _users = conn.getSystemUsers();
                this.users.clear();
                this.users.addAll(_users);
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }
    }
}