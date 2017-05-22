package voss.multilayernms.inventory.web.customer;

import naef.dto.CustomerInfoDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CustomerInfoCommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.renderer.CustomerInfoRenderer;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.PageUtil;

public class CustomerInfoEditPage extends WebPage {
    public static final String OPERATION_NAME = "CustomerInfoEdit";
    public static final String DATE_PATTERN = "yyyy/MM/dd";
    private final WebPage backPage;
    private final CustomerInfoDto user;
    private final String editorName;

    private String name = null;
    private String companyID = null;
    private boolean active = true;
    private String portalUser = null;
    private String portalPassword = null;

    public CustomerInfoEditPage(WebPage backPage, CustomerInfoDto user) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            if (backPage == null) {
                throw new IllegalArgumentException();
            }
            if (user != null) {
                user.renew();
            }
            this.backPage = backPage;
            this.user = user;
            String nameCaption = null;
            CustomerInfoRenderer renderer = new CustomerInfoRenderer(this.user);
            this.name = renderer.getName();
            if (this.user == null) {
                nameCaption = "New Customer";
            } else {
                nameCaption = this.name;
            }
            this.companyID = renderer.getCompanyID();
            this.active = renderer.isActive();
            this.portalUser = renderer.getPortalUser();
            this.portalPassword = renderer.getPortalPass();

            Link<Void> backLink = new Link<Void>("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    setResponsePage(getBackPage());
                }
            };
            add(backLink);
            add(new FeedbackPanel("feedback"));

            Label userNameLabel = new Label("customerInfo", nameCaption);
            add(userNameLabel);

            Form<Void> form = new Form<Void>("form") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    processUpdate();
                    PageUtil.setModelChanged(getBackPage());
                    setResponsePage(getBackPage());
                }
            };
            add(form);

            TextField<String> nameField = new TextField<String>("name",
                    new PropertyModel<String>(this, "name"), String.class);
            nameField.setEnabled(this.name.isEmpty());
            form.add(nameField);

            TextField<String> companyIDField = new TextField<String>("companyID",
                    new PropertyModel<String>(this, "companyID"), String.class);
            form.add(companyIDField);

            CheckBox activeBox = new CheckBox("active", new PropertyModel<Boolean>(this, "active"));
            form.add(activeBox);

            TextField<String> portalUserField = new TextField<String>("portalUser",
                    new PropertyModel<String>(this, "portalUser"), String.class);
            form.add(portalUserField);

            TextField<String> portalPasswordField = new TextField<String>("portalPassword",
                    new PropertyModel<String>(this, "portalPassword"), String.class);
            form.add(portalPasswordField);

        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void processUpdate() {
        try {
            CustomerInfoCommandBuilder builder;
            if (this.user == null) {
                if (this.name == null || this.name.isEmpty()) {
                    throw new IllegalStateException("No userName.");
                }
                builder = new CustomerInfoCommandBuilder(this.editorName);
                builder.setID(getName());
            } else {
                builder = new CustomerInfoCommandBuilder(this.user, this.editorName);
            }

            builder.setValue("企業ID", this.companyID);
            builder.setValue("active", this.active);
            builder.setValue("FMPortalUser", this.portalUser);
            builder.setValue("FMPortalPass", this.portalPassword);
            BuildResult result = builder.buildCommand();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompanyID() {
        return companyID;
    }

    public void setCompanyID(String companyID) {
        this.companyID = companyID;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getPortalUser() {
        return portalUser;
    }

    public void setPortalUser(String portalUser) {
        this.portalUser = portalUser;
    }

    public String getFMPortalPass() {
        return this.portalPassword;
    }

    public void setFMPortalPass(String portalPassword) {
        this.portalPassword = portalPassword;
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    @Override
    protected void onModelChanged() {
        this.user.renew();
        super.onModelChanged();
    }
}