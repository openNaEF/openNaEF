package voss.multilayernms.inventory.web.config;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.UrlValidator;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.config.MplsNmsConfiguration;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.UrlUtil;

import java.io.IOException;
import java.util.Locale;


public class MplsNmsConfigurationEditPage extends WebPage {
    public static final String OPERATION_NAME = "ConfigurationEdit";
    private Form<Void> form;
    public MplsNmsConfigurationEditPage() throws IOException {

        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);
            getSession().setLocale(Locale.US);
            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            Link<Void> reload = new Link<Void>("reload") {
                private static final long serialVersionUID = 1L;
                public void onClick() {
                    MplsNmsConfiguration config;
                    try {
                        config = MplsNmsConfiguration.getInstance();
                        config.reloadConfiguration();
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                    inpuClear();
                    this.getWebPage().modelChanged();
                }
            };
            add(reload);

            Link<Void> refresh = new Link<Void>("refresh") {
                private static final long serialVersionUID = 1L;

                public void onClick() {
                    inpuClear();
                    this.getPage().modelChanged();
                }
            };
            add(refresh);

            this.form = new Form<Void>("form");

            add(this.form);
            add(new FeedbackPanel("feedback"));

            Button saveButton = new Button("save") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        MplsNmsConfiguration config = MplsNmsConfiguration.getInstance();

                        config.saveConfiguration();
                        config.reloadConfiguration();
                        renew();
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }

            };
            this.form.add(saveButton);

            renew();
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public void renew() throws IOException {
        MplsNmsConfiguration config = MplsNmsConfiguration.getInstance();
    }

    @Override
    protected void onModelChanged() {
        try {
            renew();
            super.onModelChanged();
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }


    private void inpuClear() {
    }

}