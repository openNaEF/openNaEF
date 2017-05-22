package voss.multilayernms.inventory.web.subnet;

import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import naef.mvo.ip.IpAddress;
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
import voss.nms.inventory.builder.IpSubnetCommandBuilder;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.PageUtil;
import voss.nms.inventory.util.UrlUtil;

public class SubnetEditPage extends WebPage {
    public static final String KEY_NODE_NAME_FILTER = "filter";
    public static final String OPERATION_NAME = "SubnetEdit";
    private final String editorName;
    private final WebPage backPage;
    private final IpSubnetNamespaceDto namespace;
    private final IpSubnetDto subnet;
    private final IpSubnetAddressDto address;
    private String vpnPrefix = null;
    private String startAddress = null;
    private Integer maskLength = null;

    public SubnetEditPage(WebPage backPage, IpSubnetNamespaceDto namespace) {
        this(backPage, namespace, null);
    }

    public SubnetEditPage(WebPage backPage, IpSubnetNamespaceDto namespace, IpSubnetDto subnet) {
        if (namespace == null) {
            throw new IllegalArgumentException("namespace is null.");
        }
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;
            this.namespace = namespace;
            this.subnet = subnet;
            this.vpnPrefix = DtoUtil.getStringOrNull(this.namespace, ATTR.VPN_PREFIX);
            if (this.subnet != null) {
                this.address = this.subnet.getSubnetAddress();
            } else {
                this.address = null;
            }

            ExternalLink topLink = UrlUtil.getTopLink("top");
            add(topLink);
            BookmarkablePageLink<Void> refresh = new BookmarkablePageLink<Void>("refresh", SubnetEditPage.class);
            add(refresh);

            if (this.address != null) {
                IpAddress ip = this.address.getAddress();
                if (ip != null) {
                    this.startAddress = ip.toString();
                    this.maskLength = this.address.getSubnetMask();
                }
            }

            String head = null;
            if (this.address != null) {
                StringBuilder sb = new StringBuilder();
                if (this.vpnPrefix != null) {
                    sb.append(this.vpnPrefix).append("/");
                }
                sb.append(this.startAddress).append("/").append(this.maskLength);
                head = sb.toString();
            } else {
                head = "New IP Subnet Address";
            }
            add(new Label("head", new Model<String>(head)));

            Form<Void> form = new Form<Void>("form");
            add(form);
            form.add(new Label("parentSubnetName", this.namespace.getName()));
            TextField<String> startAddressField = new TextField<String>("startAddress", new PropertyModel<String>(this, "startAddress"));
            startAddressField.setRequired(true);
            form.add(startAddressField);
            TextField<String> maskLengthField = new TextField<String>("maskLength", new PropertyModel<String>(this, "maskLength"));
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
        IpSubnetCommandBuilder builder;
        if (this.subnet != null) {
            builder = new IpSubnetCommandBuilder(this.subnet, this.editorName);
        } else {
            builder = new IpSubnetCommandBuilder(this.namespace, this.editorName);
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