package voss.multilayernms.inventory.web.vlan;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vlan.VlanSegmentDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.builder.VlanCommandBuilder;
import voss.nms.inventory.builder.VlanIfBindingCommandBuilder;
import voss.nms.inventory.builder.VlanIfCommandBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.model.RenewableAbstractReadOnlyModel;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.PageUtil;
import voss.nms.inventory.util.VlanUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VlanEditPage extends WebPage {
    public static final String OPERATION_NAME = "VlanEdit";
    private final VlanDto vlan;
    private final VlanMemberPortsModel portsModel;
    private Integer id;
    private String operStatus;
    private String facilityStatus;
    private String note;
    private String endUser;
    private String purpose;
    private final String editorName;
    private final WebPage backPage;

    public VlanEditPage(WebPage backPage, VlanDto vlan) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.vlan = vlan;
            this.portsModel = new VlanMemberPortsModel(vlan);
            this.portsModel.renew();
            this.backPage = backPage;
            this.id = vlan.getVlanId();
            this.operStatus = DtoUtil.getStringOrNull(this.vlan, MPLSNMS_ATTR.OPER_STATUS);
            this.facilityStatus = DtoUtil.getStringOrNull(this.vlan, MPLSNMS_ATTR.FACILITY_STATUS);
            this.purpose = DtoUtil.getStringOrNull(this.vlan, MPLSNMS_ATTR.PURPOSE);
            this.note = DtoUtil.getStringOrNull(this.vlan, MPLSNMS_ATTR.NOTE);
            this.endUser = DtoUtil.getStringOrNull(this.vlan, MPLSNMS_ATTR.END_USER);

            add(new FeedbackPanel("feedback"));

            Form<Void> backForm = new Form<Void>("backForm");
            add(backForm);
            Button backButton = new Button("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    setResponsePage(getBackPage());
                }
            };
            backForm.add(backButton);

            Form<Void> form = new Form<Void>("editForm");
            add(form);
            Button processButton = new Button("process") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    processUpdate();
                    PageUtil.setModelChanged(getBackPage());
                    setResponsePage(getBackPage());
                }
            };
            form.add(processButton);

            Label idLabel = new Label("id", Model.of(this.id));
            form.add(idLabel);
            TextField<String> operStatusField = new TextField<String>("operStatus", new PropertyModel<String>(this, "operStatus"));
            form.add(operStatusField);
            TextField<String> facilityStatusField = new TextField<String>("facilityStatus", new PropertyModel<String>(this, "facilityStatus"));
            form.add(facilityStatusField);
            TextField<String> endUserField = new TextField<String>("endUser", new PropertyModel<String>(this, "endUser"));
            form.add(endUserField);
            TextField<String> purposeField = new TextField<String>("purpose", new PropertyModel<String>(this, "purpose"));
            form.add(purposeField);
            TextArea<String> noteArea = new TextArea<String>("noteArea", new PropertyModel<String>(this, "note"));
            form.add(noteArea);

            Link<Void> addBindLink = new Link<Void>("addBind") {
                private static final long serialVersionUID = 1L;

                public void onClick() {
                    NodeVlanPortSelectionPage page = new NodeVlanPortSelectionPage(getBackPage(), getVlan(), null);
                    setResponsePage(page);
                }
            };
            add(addBindLink);

            ListView<PortRow> portsTable = new ListView<PortRow>("ports", this.portsModel) {
                private static final long serialVersionUID = 1L;

                protected void populateItem(ListItem<PortRow> item) {
                    final PortRow row = item.getModelObject();
                    item.add(new Label("nodeName", new PropertyModel<String>(row, "nodeName")));
                    item.add(new Label("ifName", new PropertyModel<String>(row, "ifName")));
                    item.add(new Label("portMode", new PropertyModel<String>(row, "portMode")));
                    item.add(new Label("switchPortMode", new PropertyModel<String>(row, "switchPortMode")));
                    item.add(new Label("bindType", new PropertyModel<String>(row, "bindType")));
                    item.add(new Label("flags", new PropertyModel<String>(row, "flags")));
                    Link<Void> unbindLink = new Link<Void>("unbind") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick() {
                            log().debug("unbind: " + row.getNodeName() + ":" + row.getIfName());
                            processUnbind(row);
                            PageUtil.setModelChanged(VlanEditPage.this);
                            setResponsePage(VlanEditPage.this);
                        }
                    };
                    item.add(unbindLink);
                }
            };
            add(portsTable);

        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void processUpdate() {
        try {
            VlanCommandBuilder builder = new VlanCommandBuilder(this.vlan, this.editorName);
            builder.initialize();
            builder.setOperStatus(this.operStatus);
            builder.setFacilityStatus(this.facilityStatus);
            builder.setEndUserName(this.endUser);
            builder.setPurpose(this.purpose);
            builder.setNote(this.note);
            BuildResult result = builder.buildCommand();
            if (BuildResult.NO_CHANGES == result) {
                return;
            }
            if (BuildResult.SUCCESS != result) {
                throw new IllegalStateException();
            }
            ShellConnector.getInstance().execute(builder);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void processUnbind(PortRow row) {
        if (row.boundPort == null) {
            processVlanIfDelete(row);
        } else {
            processSwitchUnbind(row);
        }
    }

    private void processVlanIfDelete(PortRow row) {
        try {
            VlanIfCommandBuilder builder;
            if (VlanUtil.isRouterVlanIf(row.vif)) {
                builder = new VlanIfCommandBuilder((PortDto) row.vif.getOwner(), row.vif, this.editorName);
            } else {
                builder = new VlanIfCommandBuilder((NodeDto) row.vif.getOwner(), row.vif, this.editorName);
            }
            BuildResult result = builder.buildDeleteCommand();
            if (BuildResult.NO_CHANGES == result) {
                return;
            }
            if (BuildResult.SUCCESS != result) {
                throw new IllegalStateException();
            }
            ShellConnector.getInstance().execute(builder);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void processSwitchUnbind(PortRow row) {
        try {
            BuildResult result;
            CommandBuilder builder;
            if (row.isLastBind()) {
                VlanIfCommandBuilder builder1 = new VlanIfCommandBuilder(row.vif.getNode(), row.vif, this.editorName);
                result = builder1.buildDeleteCommand();
                builder = builder1;
            } else {
                VlanIfBindingCommandBuilder builder2 = new VlanIfBindingCommandBuilder(row.vif, this.editorName);
                if (row.isTagged) {
                    builder2.removeTaggedPort(row.boundPort);
                } else {
                    builder2.removeUntaggedPort(row.boundPort);
                }
                result = builder2.buildCommand();
                builder = builder2;
            }
            if (BuildResult.NO_CHANGES == result) {
                return;
            }
            if (BuildResult.SUCCESS != result) {
                throw new IllegalStateException();
            }
            ShellConnector.getInstance().execute(builder);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    @Override
    protected void onModelChanged() {
        this.vlan.renew();
        this.portsModel.renew();
        super.onModelChanged();
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public String getOperStatus() {
        return operStatus;
    }

    public void setOperStatus(String operStatus) {
        this.operStatus = operStatus;
    }

    public String getFacilityStatus() {
        return facilityStatus;
    }

    public void setFacilityStatus(String facilityStatus) {
        this.facilityStatus = facilityStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getEndUser() {
        return endUser;
    }

    public void setEndUser(String endUser) {
        this.endUser = endUser;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public VlanDto getVlan() {
        return vlan;
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }

    private static class VlanMemberPortsModel extends RenewableAbstractReadOnlyModel<List<PortRow>> {
        private static final long serialVersionUID = 1L;
        private final VlanDto vlan;
        private final List<PortRow> result = new ArrayList<PortRow>();

        public VlanMemberPortsModel(VlanDto vlan) {
            if (vlan == null) {
                throw new IllegalArgumentException();
            }
            this.vlan = vlan;
        }

        public List<PortRow> getObject() {
            return this.result;
        }

        public void renew() {
            this.vlan.renew();
            this.result.clear();
            for (VlanIfDto vif : this.vlan.getMemberVlanifs()) {
                log().debug("renew: " + vif.getAbsoluteName());
                if (VlanUtil.isRouterVlanIf(vif)) {
                    PortRow row = new PortRow();
                    row.bindType = "Sub I/F";
                    row.vif = vif;
                    row.boundPort = null;
                    this.result.add(row);
                } else {
                    int bounds = 0;
                    for (PortDto tagged : vif.getTaggedVlans()) {
                        PortRow row = new PortRow();
                        row.bindType = "TRUNK";
                        row.vif = vif;
                        row.boundPort = tagged;
                        row.isTagged = true;
                        this.result.add(row);
                        bounds++;
                    }
                    for (PortDto untagged : vif.getUntaggedVlans()) {
                        PortRow row = new PortRow();
                        row.bindType = "ACCESS";
                        row.vif = vif;
                        row.boundPort = untagged;
                        row.isTagged = false;
                        this.result.add(row);
                        bounds++;
                    }
                    if (bounds == 0) {
                        PortRow row = new PortRow();
                        row.bindType = "VLAN I/F";
                        row.vif = vif;
                        row.boundPort = null;
                        row.isTagged = false;
                        this.result.add(row);
                    }
                }
            }
        }

        private Logger log() {
            return LoggerFactory.getLogger(this.getClass());
        }
    }

    @SuppressWarnings("unused")
    private static class PortRow implements Serializable {
        private static final long serialVersionUID = 1L;
        public VlanIfDto vif;
        public PortDto boundPort;
        public String bindType;
        public boolean isTagged = false;

        public String getNodeName() {
            return this.vif.getNode().getName();
        }

        public String getIfName() {
            return DtoUtil.getIfName(boundPort);
        }

        public String getPortMode() {
            return DtoUtil.getStringOrNull(boundPort, MPLSNMS_ATTR.ATTR_PORT_MODE);
        }

        public String getSwitchPortMode() {
            return DtoUtil.getStringOrNull(boundPort, MPLSNMS_ATTR.ATTR_SWITCHPORT_MODE);
        }

        public String getBindType() {
            return this.bindType;
        }

        public String getFlags() {
            StringBuilder flags = new StringBuilder();
            if (isLastBind()) {
                flags.append("*");
            }
            VlanSegmentDto link;
            if (boundPort == null) {
                Collection<VlanSegmentDto> links = this.vif.getVlanLinks();
                if (links != null && links.size() > 0) {
                    link = links.iterator().next();
                } else {
                    link = null;
                }
            } else {
                link = VlanUtil.getVlanLinkOver(this.vif, this.boundPort);
            }
            if (link != null) {
                flags.append("x");
            }
            return flags.toString();
        }

        public boolean isLastBind() {
            if (this.boundPort == null) {
                return true;
            }
            int num = this.vif.getTaggedVlans().size();
            num += this.vif.getUntaggedVlans().size();
            return num <= 1;
        }
    }
}