package voss.multilayernms.inventory.web.subnet;

import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.BuildResult;
import voss.core.server.database.ATTR;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.builder.RegularIpSubnetNamespaceCommandBuilder;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.PageUtil;
import voss.nms.inventory.util.UrlUtil;

public class SubnetNamespaceEditPage extends WebPage {
    public static final String KEY_NODE_NAME_FILTER = "filter";
    public static final String OPERATION_NAME = "SubnetNamespaceEdit";
    private final String editorName;
    private final WebPage backPage;
    private final IpSubnetNamespaceDto parent;
    private final IpSubnetNamespaceDto subnet;
    private final IpSubnetAddressDto address;
    private String vpnPrefix = null;
    private String startAddress = "0.0.0.0";
    private Integer maskLength = Integer.valueOf(0);
    private boolean isTop = true;
    private final TextField<String> nameField;

    public SubnetNamespaceEditPage(WebPage backPage) {
        this(backPage, null, null);
    }

    public SubnetNamespaceEditPage(WebPage backPage, IpSubnetNamespaceDto subnet) {
        this(backPage, subnet, null);
    }

    public SubnetNamespaceEditPage(WebPage backPage, IpSubnetNamespaceDto subnet, IpSubnetNamespaceDto parent) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;
            this.parent = parent;
            this.subnet = subnet;
            if (this.subnet != null) {
                this.address = this.subnet.getIpSubnetAddress();
                this.vpnPrefix = DtoUtil.getStringOrNull(this.subnet, ATTR.VPN_PREFIX);
                this.isTop = subnet.getParent() == null;
            } else {
                this.address = null;
                this.vpnPrefix = null;
                this.isTop = this.parent == null;
            }
            if (this.address != null) {
                this.startAddress = this.address.getAddress().toString();
                this.maskLength = this.address.getSubnetMask();
            } else {
                this.startAddress = "0.0.0.0";
                this.maskLength = Integer.valueOf(0);
            }
            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            BookmarkablePageLink<Void> refresh = new BookmarkablePageLink<Void>("refresh", SubnetNamespaceEditPage.class);
            add(refresh);

            String head = null;
            if (this.address != null) {
                head = this.startAddress + "/" + this.maskLength;
            } else {
                head = "New IP Subnet Address";
            }
            add(new Label("head", new Model<String>(head)));

            Form<Void> form = new Form<Void>("form");
            add(form);
            this.nameField = new TextField<String>("vpnPrefix", new PropertyModel<String>(this, "vpnPrefix"));
            this.nameField.setEnabled(isTop);
            this.nameField.setVisible(isTop);
            form.add(this.nameField);
            TextField<String> startAddressField = new TextField<String>("startAddress", new PropertyModel<String>(this, "startAddress"));
            startAddressField.setRequired(true);
            form.add(startAddressField);
            TextField<Integer> maskLengthField = new TextField<Integer>("maskLength", new PropertyModel<Integer>(this, "maskLength"));
            maskLengthField.setRequired(true);
            form.add(maskLengthField);

            Button submit = new Button("submit") {
                private static final long serialVersionUID = 1L;

                public void onSubmit() {
                    log().debug("submit");
                    try {
                        processCommit();
                        PageUtil.setModelChanged(getBackPage());
                        setResponsePage(getBackPage());
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            form.add(submit);
            Button back = new Button("back") {
                private static final long serialVersionUID = 1L;

                public void onSubmit() {
                    log().debug("back");
                    setResponsePage(getBackPage());
                }
            };
            form.add(back);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public void processCommit() throws Exception {
        RegularIpSubnetNamespaceCommandBuilder builder;
        if (this.subnet == null) {
            builder = new RegularIpSubnetNamespaceCommandBuilder(this.editorName);
            builder.setParent(this.parent);
        } else {
            builder = new RegularIpSubnetNamespaceCommandBuilder(this.subnet, this.editorName);
        }
        builder.setStartAddress(this.startAddress);
        builder.setMaskLength(this.maskLength);
        BuildResult result = builder.buildCommand();
        if (result == BuildResult.NO_CHANGES) {
            return;
        } else if (result == BuildResult.FAIL) {
            throw new IllegalStateException("command build failed.");
        }
        ShellConnector.getInstance().execute(builder);
    }

    public String getVpnPrefix() {
        return this.vpnPrefix;
    }

    public void setVpnPrefix(String vpnPrefix) {
        this.vpnPrefix = vpnPrefix;
    }

    public String getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(String startAddress) {
        this.startAddress = startAddress;
    }

    public Integer getMaskLength() {
        return maskLength;
    }

    public void setMaskLength(Integer maskLength) {
        this.maskLength = maskLength;
    }

    public WebPage getBackPage() {
        return backPage;
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }
}