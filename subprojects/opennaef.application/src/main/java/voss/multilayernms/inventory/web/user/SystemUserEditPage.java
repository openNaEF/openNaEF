package voss.multilayernms.inventory.web.user;

import naef.dto.SystemUserDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.SystemUserCommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.Util;
import voss.multilayernms.inventory.renderer.SystemUserRenderer;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.PageUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SystemUserEditPage extends WebPage {
    public static final String OPERATION_NAME = "SystemUserEdit";
    public static final String DATE_PATTERN = "yyyy/MM/dd";
    private final WebPage backPage;
    private final SystemUserDto user;
    private final String editorName;

    private String name = null;
    private String caption = null;
    private boolean active = true;
    private String passwordScheme = null;
    private String passwordHash = null;
    private String passwordExpire = null;
    private String externalAuthenticator = null;
    private String note = null;

    public SystemUserEditPage(WebPage backPage, SystemUserDto user) {
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
            SystemUserRenderer renderer = new SystemUserRenderer(this.user);
            this.name = renderer.getName();
            if (this.user == null) {
                nameCaption = "New System User";
            } else {
                nameCaption = this.name;
            }
            this.caption = renderer.getCaption();
            this.active = renderer.isActive();
            this.passwordScheme = renderer.getPasswordScheme();
            this.passwordExpire = renderer.getPasswordExpireString(DATE_PATTERN);
            this.externalAuthenticator = renderer.getExternalAuthenticator();
            this.note = renderer.getNote();

            Link<Void> backLink = new Link<Void>("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    setResponsePage(getBackPage());
                }
            };
            add(backLink);
            add(new FeedbackPanel("feedback"));

            Label userNameLabel = new Label("userName", nameCaption);
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
            nameField.setEnabled(this.name == null);
            form.add(nameField);

            TextField<String> captionField = new TextField<String>("caption",
                    new PropertyModel<String>(this, "caption"), String.class);
            form.add(captionField);

            CheckBox activeBox = new CheckBox("active", new PropertyModel<Boolean>(this, "active"));
            form.add(activeBox);

            TextField<String> noteField = new TextField<String>("note",
                    new PropertyModel<String>(this, "note"), String.class);
            form.add(noteField);

            final List<String> schemes = new ArrayList<String>();
            schemes.add("SHA-256");
            DropDownChoice<String> passwordSchemeChoice = new DropDownChoice<String>("passwordScheme",
                    new PropertyModel<String>(this, "passwordScheme"), schemes);
            passwordSchemeChoice.setNullValid(true);
            form.add(passwordSchemeChoice);

            TextField<String> passwordField = new TextField<String>("passwordHash",
                    new PropertyModel<String>(this, "passwordHash"), String.class);
            form.add(passwordField);

            TextField<String> passwordExpireField = new TextField<String>("passwordExpire",
                    new PropertyModel<String>(this, "passwordExpire"), String.class);
            form.add(passwordExpireField);

            TextField<String> externalAuthenticatorField = new TextField<String>("externalAuthenticator",
                    new PropertyModel<String>(this, "externalAuthenticator"), String.class);
            form.add(externalAuthenticatorField);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void processUpdate() {
        try {
            SystemUserCommandBuilder builder;
            if (this.user == null) {
                if (this.name == null || this.name.isEmpty()) {
                    throw new IllegalStateException("No userName.");
                }
                builder = new SystemUserCommandBuilder(this.name, this.editorName);
            } else {
                builder = new SystemUserCommandBuilder(this.user, this.editorName);
            }
            builder.setActive(this.active);
            builder.setCaption(this.caption);
            builder.setNote(this.note);
            if (this.passwordHash != null && !this.passwordHash.isEmpty()) {
                if (this.passwordScheme != null) {
                    String hash = Util.getDigest(this.passwordScheme, this.passwordHash);
                    builder.setPasswordScheme(this.passwordScheme);
                    builder.setPasswordHash(hash);
                } else {
                    builder.setPasswordScheme(null);
                    builder.setPasswordHash(this.passwordHash);
                }
            } else {
            }
            builder.setPasswordExpire(getPasswordExpireDate());
            builder.setExternalAuthenticator(this.externalAuthenticator);
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

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getPasswordScheme() {
        return passwordScheme;
    }

    public void setPasswordScheme(String passwordScheme) {
        this.passwordScheme = passwordScheme;
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public void setPasswordHash(String hash) {
        this.passwordHash = hash;
    }

    public String getPasswordExpire() {
        return passwordExpire;
    }

    public Date getPasswordExpireDate() {
        if (this.passwordExpire == null || this.passwordExpire.isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
            return sdf.parse(this.passwordExpire);
        } catch (Exception e) {
        }
        return null;
    }

    public void setPasswordExpire(String passwordExpire) {
        this.passwordExpire = passwordExpire;
    }

    public String getExternalAuthenticator() {
        return externalAuthenticator;
    }

    public void setExternalAuthenticator(String extenralAuthenticator) {
        this.externalAuthenticator = extenralAuthenticator;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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