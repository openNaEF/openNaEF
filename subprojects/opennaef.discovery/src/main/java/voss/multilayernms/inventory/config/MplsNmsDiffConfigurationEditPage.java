package voss.multilayernms.inventory.config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.PatternValidator;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.config.MplsNmsDiffConfiguration.FromToValueHolder;
import voss.multilayernms.inventory.diff.util.ConfigUtil;
import voss.nms.inventory.model.RenewableAbstractReadOnlyModel;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.UrlUtil;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MplsNmsDiffConfigurationEditPage extends WebPage {

    public static final String OPERATION_NAME = "ConfigurationEdit";

    private final String DATE_STRING_FORMAT2 = "yyyy/MM/dd HH:mm";

    private Form<Void> form;
    private TextField<String> externalInventoryDBDisplayNameField;
    private TextField<String> externalInventoryDBConnectionStringField;
    private DateTextField externalInventoryDBSchedulerBaseTimeField;
    private TextField<Integer> externalInventoryDBSchedulerIntervalMinutesField;
    private CheckBox externalInventoryDBAutoApplyField;
    private TextField<String> discoveryDisplayNameField;
    private TextField<String> defaultSnmpCommunityStringField;
    private TextField<Integer> discoveryMaxThreadSizeField;
    private TextField<Integer> discoverySnmpTimeoutSecondsField;
    private TextField<Integer> discoverySnmpRetryTimesField;
    private TextField<Integer> discoveryTelnetTimeoutSecondsField;
    private DateTextField discoverySchedulerBaseTimeField;
    private TextField<Integer> discoverySchedulerIntervalMinutesField;
    private TextField<String> diffsetStoreDirectoryNameField;
    private ListView<FromToValueHolder> discoveryTypeMappingsListView;
    private ListView<FromToValueHolder> valueMappingsListView;

    private String discoveryDisplayName = null;
    private String defaultSnmpCommunityString = null;
    private int discoveryMaxThreadSize;
    private int discoverySnmpTimeoutSeconds;
    private int discoverySnmpRetryTimes;
    private int discoveryTelnetTimeoutSeconds;
    private Date discoverySchedulerBaseTime = null;
    private int discoverySchedulerIntervalMinutes;
    private String diffsetStoreDirectoryName = null;
    private RenewableAbstractReadOnlyModel<List<FromToValueHolder>> discoveryTypeMappings;
    private RenewableAbstractReadOnlyModel<List<FromToValueHolder>> valueMappings;
    public MplsNmsDiffConfigurationEditPage() throws IOException {

        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);

            getSession().setLocale(Locale.US);

            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);

            Link<Void> reload = new Link<Void>("reload") {
                private static final long serialVersionUID = 1L;

                public void onClick() {

                    MplsNmsDiffConfiguration config;
                    try {
                        config = MplsNmsDiffConfiguration.getInstance();
                        config.reloadConfiguration();
                        ConfigUtil.getInstance().reload();
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw ExceptionUtils.throwAsRuntime(e);
                    } catch (ConfigurationException e) {
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

            this.discoveryDisplayNameField = new TextField<String>("discoveryDisplayName", new PropertyModel<String>(this, "discoveryDisplayName"), String.class);
            discoveryDisplayNameField.add(new PatternValidator("[ -~]*"));
            this.form.add(discoveryDisplayNameField);
            this.defaultSnmpCommunityStringField = new TextField<String>("defaultSnmpCommunityString", new PropertyModel<String>(this, "defaultSnmpCommunityString"), String.class);
            defaultSnmpCommunityStringField.add(new PatternValidator("[ -~]*"));
            this.form.add(defaultSnmpCommunityStringField);
            this.discoveryMaxThreadSizeField = new TextField<Integer>("discoveryMaxThreadSize", new PropertyModel<Integer>(this, "discoveryMaxThreadSize"), Integer.class);
            this.form.add(discoveryMaxThreadSizeField);
            this.discoverySnmpTimeoutSecondsField = new TextField<Integer>("discoverySnmpTimeoutSeconds", new PropertyModel<Integer>(this, "discoverySnmpTimeoutSeconds"), Integer.class);
            this.form.add(discoverySnmpTimeoutSecondsField);
            this.discoverySnmpRetryTimesField = new TextField<Integer>("discoverySnmpRetryTimes", new PropertyModel<Integer>(this, "discoverySnmpRetryTimes"), Integer.class);
            this.form.add(discoverySnmpRetryTimesField);
            this.discoveryTelnetTimeoutSecondsField = new TextField<Integer>("discoveryTelnetTimeoutSeconds", new PropertyModel<Integer>(this, "discoveryTelnetTimeoutSeconds"), Integer.class);
            this.form.add(discoveryTelnetTimeoutSecondsField);
            this.discoverySchedulerBaseTimeField = new DateTextField("discoverySchedulerBaseTime", new PropertyModel<Date>(this, "discoverySchedulerBaseTime"), this.DATE_STRING_FORMAT2);
            this.form.add(discoverySchedulerBaseTimeField);
            this.discoverySchedulerIntervalMinutesField = new TextField<Integer>("discoverySchedulerIntervalMinutes", new PropertyModel<Integer>(this, "discoverySchedulerIntervalMinutes"), Integer.class);
            this.form.add(discoverySchedulerIntervalMinutesField);

            Button saveButton = new Button("save") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        MplsNmsDiffConfiguration config = MplsNmsDiffConfiguration.getInstance();

                        config.setDiscoveryDisplayName(getDiscoveryDisplayName());
                        config.setDefaultSnmpCommunityString(getDefaultSnmpCommunityString());
                        config.setDiscoveryMaxThreadSize(getDiscoveryMaxThreadSize());
                        config.setDiscoverySnmpTimeoutSeconds(getDiscoverySnmpTimeoutSeconds());
                        config.setDiscoverySnmpRetryTimes(getDiscoverySnmpRetryTimes());
                        config.setDiscoveryTelnetTimeoutSeconds(getDiscoveryTelnetTimeoutSeconds());
                        config.setDiscoverySchedulerBaseTime(getDiscoverySchedulerBaseTime());
                        config.setDiscoverySchedulerIntervalMinutes(getDiscoverySchedulerIntervalMinutes());

                        config.saveConfiguration();
                        config.reloadConfiguration();
                        ConfigUtil.getInstance().reload();
                        renew();
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw ExceptionUtils.throwAsRuntime(e);
                    } catch (ConfigurationException e) {
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
        MplsNmsDiffConfiguration config = MplsNmsDiffConfiguration.getInstance();
        this.discoveryDisplayName = config.getDiscoveryDisplayName();
        this.defaultSnmpCommunityString = config.getDefaultSnmpCommunityString();
        this.discoveryMaxThreadSize = config.getDiscoveryMaxThreadSize();
        this.discoverySnmpTimeoutSeconds = config.getDiscoverySnmpTimeoutSeconds();
        this.discoverySnmpRetryTimes = config.getDiscoverySnmpRetryTimes();
        this.discoveryTelnetTimeoutSeconds = config.getDiscoveryTelnetTimeoutSeconds();
        this.discoverySchedulerBaseTime = config.getDiscoverySchedulerBaseTime();
        this.discoverySchedulerIntervalMinutes = config.getDiscoverySchedulerIntervalMinutes();
        this.diffsetStoreDirectoryName = config.getDiffsetStoreDirectoryName();
    }

    public String getDiscoveryDisplayName() {
        return discoveryDisplayName;
    }

    public void setDiscoveryDisplayName(String discoveryDisplayName) {
        this.discoveryDisplayName = discoveryDisplayName;
    }

    public String getDefaultSnmpCommunityString() {
        return defaultSnmpCommunityString;
    }

    public void setDefaultSnmpCommunityString(String defaultSnmpCommunityString) {
        this.defaultSnmpCommunityString = defaultSnmpCommunityString;
    }

    public int getDiscoveryMaxThreadSize() {
        return discoveryMaxThreadSize;
    }

    public void setDiscoveryMaxThreadSize(int discoveryMaxThreadSize) {
        this.discoveryMaxThreadSize = discoveryMaxThreadSize;
    }

    public int getDiscoverySnmpTimeoutSeconds() {
        return discoverySnmpTimeoutSeconds;
    }

    public void setDiscoverySnmpTimeoutSeconds(int discoverySnmpTimeoutSeconds) {
        this.discoverySnmpTimeoutSeconds = discoverySnmpTimeoutSeconds;
    }

    public int getDiscoverySnmpRetryTimes() {
        return discoverySnmpRetryTimes;
    }

    public void setDiscoverySnmpRetryTimes(int discoverySnmpRetryTimes) {
        this.discoverySnmpRetryTimes = discoverySnmpRetryTimes;
    }

    public int getDiscoveryTelnetTimeoutSeconds() {
        return discoveryTelnetTimeoutSeconds;
    }

    public void setDiscoveryTelnetTimeoutSeconds(
            int discoveryTelnetTimeoutSeconds) {
        this.discoveryTelnetTimeoutSeconds = discoveryTelnetTimeoutSeconds;
    }

    public Date getDiscoverySchedulerBaseTime() {
        return discoverySchedulerBaseTime;
    }

    public void setDiscoverySchedulerBaseTime(Date discoverySchedulerBaseTime) {
        this.discoverySchedulerBaseTime = discoverySchedulerBaseTime;
    }

    public int getDiscoverySchedulerIntervalMinutes() {
        return discoverySchedulerIntervalMinutes;
    }

    public void setDiscoverySchedulerIntervalMinutes(
            int discoverySchedulerIntervalMinutes) {
        this.discoverySchedulerIntervalMinutes = discoverySchedulerIntervalMinutes;
    }

    public String getDiffsetStoreDirectoryName() {
        return diffsetStoreDirectoryName;
    }

    public void setDiffsetStoreDirectoryName(String diffsetStoreDirectoryName) {
        this.diffsetStoreDirectoryName = diffsetStoreDirectoryName;
    }

    private List<FromToValueHolder> getDiscoveryTypeMappings() throws IOException {
        return MplsNmsDiffConfiguration.getInstance().getDiscoveryTypeMappings();
    }

    private List<FromToValueHolder> getValueMappings() throws IOException {
        return MplsNmsDiffConfiguration.getInstance().getValueMappings();

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
        externalInventoryDBDisplayNameField.clearInput();
        externalInventoryDBConnectionStringField.clearInput();
        externalInventoryDBSchedulerBaseTimeField.clearInput();
        externalInventoryDBSchedulerIntervalMinutesField.clearInput();
        externalInventoryDBAutoApplyField.clearInput();
        discoveryDisplayNameField.clearInput();
        defaultSnmpCommunityStringField.clearInput();
        discoveryMaxThreadSizeField.clearInput();
        discoverySnmpTimeoutSecondsField.clearInput();
        discoverySnmpRetryTimesField.clearInput();
        discoveryTelnetTimeoutSecondsField.clearInput();
        discoverySchedulerBaseTimeField.clearInput();
        discoverySchedulerIntervalMinutesField.clearInput();
        diffsetStoreDirectoryNameField.clearInput();
    }

}