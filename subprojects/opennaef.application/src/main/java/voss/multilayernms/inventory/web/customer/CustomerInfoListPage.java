package voss.multilayernms.inventory.web.customer;

import naef.dto.CustomerInfoDto;
import naef.ui.NaefDtoFacade;
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
import pasaran.naef.dto.CustomerInfo2dDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CustomerInfoCommandBuilder;
import voss.core.server.database.ATTR;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.CustomerInfoRenderer;
import voss.nms.inventory.model.RenewableAbstractReadOnlyModel;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.UrlUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CustomerInfoListPage extends WebPage {
    public static final String OPERATION_NAME = "CustomerInfoList";
    private final UserModel model;
    private final String editorName;
    public CustomerInfoListPage() {
        this(new PageParameters());
    }

    public CustomerInfoListPage(PageParameters params) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            BookmarkablePageLink<Void> refresh = new BookmarkablePageLink<Void>("refresh", CustomerInfoListPage.class);
            add(refresh);
            this.model = new UserModel();
            this.model.renew();
            Form<Void> form = new Form<Void>("form");
            add(form);
            Button addCustomerButton = new Button("addCustomer") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    WebPage page = new CustomerInfoEditPage(CustomerInfoListPage.this, null);
                    setResponsePage(page);
                }
            };
            form.add(addCustomerButton);
            ListView<CustomerInfo2dDto> customerInfoList = new ListView<CustomerInfo2dDto>("customerInfos", this.model) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<CustomerInfo2dDto> item) {
                    final CustomerInfo2dDto user = item.getModelObject();
                    final CustomerInfoRenderer renderer = new CustomerInfoRenderer(user);

                    item.add(new Label("name", renderer.getName()));
                    item.add(new Label("companyID", renderer.getCompanyID()));
                    item.add(new Label("accountStatus", renderer.getStatus()));
                    item.add(new Label("portalUser", renderer.getPortalUser()));
                    item.add(new Label("portalPassword", renderer.getPortalPass()));
                    Link<Void> editCustomerInfo = new Link<Void>("editCustomerInfo") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            WebPage page = new CustomerInfoEditPage(CustomerInfoListPage.this, user);
                            setResponsePage(page);
                        }
                    };
                    item.add(editCustomerInfo);
                    Link<Void> delCustomerInfo = new Link<Void>("delCustomerInfo") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            processDelete(user);
                            WebPage page = new CustomerInfoListPage();
                            setResponsePage(page);
                        }
                    };
                    item.add(delCustomerInfo);
                    Link<Void> editResource = new Link<Void>("editResource") {
                        private static final long serialVersionUID = 1L;

                        public void onClick() {
                            WebPage page = new CustomerResourceEditPage(CustomerInfoListPage.this, user, null);
                            setResponsePage(page);
                        }
                    };
                    item.add(editResource);
                }
            };
            add(customerInfoList);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    @Override
    protected void onModelChanged() {
        this.model.renew();
    }

    private class UserModel extends RenewableAbstractReadOnlyModel<List<CustomerInfo2dDto>> {
        private static final long serialVersionUID = 1L;
        private List<CustomerInfo2dDto> users = new ArrayList<CustomerInfo2dDto>();

        @Override
        public List<CustomerInfo2dDto> getObject() {
            return users;
        }

        @Override
        public void renew() {
            try {
                NaefDtoFacade facade = MplsNmsInventoryConnector.getInstance().getDtoFacade();
                Set<CustomerInfoDto> users = facade.selectCustomerInfos(NaefDtoFacade.SearchMethod.REGEXP, ATTR.CUSTOMER_INFO_ID, ".*");
                ArrayList<CustomerInfo2dDto> customers = new ArrayList<>();
                for (CustomerInfoDto user : users){
                    if(DtoUtil.getBoolean(user, "削除フラグ") != Boolean.TRUE){
                        customers.add((CustomerInfo2dDto) user);
                    }
                }
                this.users.clear();
                this.users.addAll(customers);
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }
    }

    private void processDelete(CustomerInfo2dDto user) {
        try {
            CustomerInfoCommandBuilder builder;
            if (user == null) {
                throw new IllegalStateException("No userName.");
            } else {
                builder = new CustomerInfoCommandBuilder(user, this.editorName);
            }
            BuildResult result = builder.buildDeleteCommand();
            switch (result) {
                case SUCCESS:
                    ShellConnector.getInstance().execute(builder);
                    return;
                case NO_CHANGES:
                    return;
                case FAIL:
                default:
                    throw new IllegalStateException("unexpected build result: " + result);
            }
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }
}