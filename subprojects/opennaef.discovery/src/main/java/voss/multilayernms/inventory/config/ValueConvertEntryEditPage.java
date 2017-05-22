package voss.multilayernms.inventory.config;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.config.MplsNmsDiffConfiguration.FromToValueHolder;
import voss.multilayernms.inventory.diff.util.ConfigUtil;
import voss.nms.inventory.util.PageUtil;

import java.util.ArrayList;
import java.util.List;

public class ValueConvertEntryEditPage extends WebPage {

    private WebPage backPage;
    private String from = null;
    private String to = null;

    private TextField<String> fromField;
    private TextField<String> toField;

    public ValueConvertEntryEditPage(WebPage backPage, final FromToValueHolder fromToValueHolder, final List<FromToValueHolder> allHolders) {
        this.backPage = backPage;
        if (fromToValueHolder != null) {
            this.from = fromToValueHolder.from;
            this.to = fromToValueHolder.to;
        }

        Form<Void> form = new Form<Void>("edit");
        add(form);

        this.fromField = new TextField<String>("from", new PropertyModel<String>(this, "from"), String.class);
        form.add(fromField);
        this.toField = new TextField<String>("to", new PropertyModel<String>(this, "to"), String.class);
        form.add(toField);

        Button applyButton = new Button("apply") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit() {
                if (getFrom() == null || getFrom().equals("")) {
                    throw new IllegalArgumentException("From is mandatory.");
                }
                if (getTo() == null || getTo().equals("")) {
                    throw new IllegalArgumentException("To is mandatory.");
                }
                try {

                    FromToValueHolder newHolder = new FromToValueHolder();
                    newHolder.from = getFrom();
                    newHolder.to = getTo();
                    List<FromToValueHolder> holders = new ArrayList<FromToValueHolder>();
                    if (fromToValueHolder != null) {
                        for (FromToValueHolder hol : allHolders) {
                            if (hol.from.equals(fromToValueHolder.from) && hol.to.equals(fromToValueHolder.to)) {
                                holders.add(newHolder);
                            } else {
                                holders.add(hol);
                            }
                        }
                    } else {
                        holders.addAll(allHolders);
                        holders.add(newHolder);
                    }

                    MplsNmsDiffConfiguration con = MplsNmsDiffConfiguration.getInstance();
                    con.setValueMappings(holders);
                    con.saveConfiguration();
                    con.reloadConfiguration();
                    ConfigUtil.getInstance().reload();
                    getBackPage().modelChanged();
                    PageUtil.setModelChanged(getBackPage());
                    setResponsePage(getBackPage());
                } catch (Exception e) {
                    throw ExceptionUtils.throwAsRuntime(e);
                }
            }
        };
        form.add(applyButton);

        Button backButton = new Button("back") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit() {
                setResponsePage(getBackPage());
            }
        };
        form.add(backButton);

    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public WebPage getBackPage() {
        return backPage;
    }


}