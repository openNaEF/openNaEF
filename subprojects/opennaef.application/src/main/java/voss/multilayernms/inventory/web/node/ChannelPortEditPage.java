package voss.multilayernms.inventory.web.node;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.serial.TdmSerialIfDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.AbstractValidator;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.web.parts.OperationStatusChoiceRenderer;
import voss.nms.inventory.builder.ChannelPortCommandBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChannelPortEditPage extends WebPage {
    public static final String OPERATION_NAME = "ChannelPortEdit";
    private final Form<Void> editInterface;

    private final WebPage backPage;
    private PortDto parentPort;
    private final TdmSerialIfDto channelPort;
    private NodeDto node;
    private String ifName;
    private String operStatus;
    private Long bandwidth;
    private String timeslot;
    private String channelGroup;
    private String purpose;
    private String note;
    private boolean parentPortEditable = false;
    private final String editorName;

    public ChannelPortEditPage(final WebPage backPage, final NodeDto node, final PortDto parentPort, final TdmSerialIfDto channelPort) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;
            if (channelPort != null) {
                this.channelPort = channelPort;
                this.ifName = PortRenderer.getIfName(channelPort);
                this.operStatus = PortRenderer.getOperStatus(channelPort);
                this.bandwidth = PortRenderer.getBandwidthAsLong(channelPort);
                this.timeslot = PortRenderer.getTimeSlot(channelPort);
                this.channelGroup = PortRenderer.getChannelGroup(channelPort);
                this.purpose = PortRenderer.getPurpose(channelPort);
                this.note = PortRenderer.getNote(channelPort);
                this.parentPort = (PortDto) channelPort.getOwner();
                this.parentPortEditable = false;
            } else if (parentPort != null) {
                this.channelPort = null;
                this.operStatus = MPLSNMS_ATTR.UP;
                this.parentPort = parentPort;
                this.parentPortEditable = false;
            } else {
                this.channelPort = null;
                this.operStatus = MPLSNMS_ATTR.UP;
                this.parentPort = null;
                this.parentPortEditable = true;
            }
            this.node = node;

            Label nodeNameLabel = new Label("nodeName", Model.of(NameUtil.getCaption(parentPort)));
            add(nodeNameLabel);

            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();

            Form<Void> deleteChannel = new Form<Void>("deleteChannelPort");
            add(deleteChannel);
            Button proceedButton2 = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        NodeUtil.checkDeletable(channelPort);
                        ChannelPortCommandBuilder builder = new ChannelPortCommandBuilder(channelPort, editorName);
                        builder.buildDeleteCommand();
                        ShellConnector.getInstance().execute(builder);
                        PageUtil.setModelChanged(getBackPage());
                        setResponsePage(getBackPage());
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            proceedButton2.setEnabled(channelPort != null);
            proceedButton2.setVisible(channelPort != null);
            deleteChannel.add(proceedButton2);

            Form<Void> backForm = new Form<Void>("back");
            add(backForm);
            Button backButton2 = new Button("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    setResponsePage(getBackPage());
                }
            };
            backForm.add(backButton2);

            this.editInterface = new Form<Void>("editChannelPort");
            add(this.editInterface);

            editInterface.add(new FeedbackPanel("feedback"));

            Button proceedButton = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    processEdit();
                    PageUtil.setModelChanged(getBackPage());
                    setResponsePage(getBackPage());
                }
            };
            editInterface.add(proceedButton);

            DropDownChoice<PortDto> parentPortList = new DropDownChoice<PortDto>("parentPort",
                    new PropertyModel<PortDto>(this, "parentPort"),
                    ChannelPortUtil.getChannelEnabledPortsOn(node),
                    new IfNameRenderer());
            parentPortList.setEnabled(parentPortEditable);
            this.editInterface.add(parentPortList);

            TextField<String> ifNameField = new TextField<String>("ifName", new PropertyModel<String>(this, "ifName"));
            this.editInterface.add(ifNameField);

            TextField<Integer> timeslotField = new TextField<Integer>("timeslot",
                    new PropertyModel<Integer>(this, "timeslot"));
            this.editInterface.add(timeslotField);

            TextField<Integer> channelGroupField = new TextField<Integer>("channelGroup",
                    new PropertyModel<Integer>(this, "channelGroup"));
            this.editInterface.add(channelGroupField);

            RadioChoice<String> operStatusSelection = new RadioChoice<String>("operStatus",
                    new PropertyModel<String>(this, "operStatus"),
                    conn.getStatusList(),
                    new OperationStatusChoiceRenderer());
            operStatusSelection.setSuffix("");
            editInterface.add(operStatusSelection);

            addTextField(channelPort, "bandwidth", "bandwidth");
            addTextField(channelPort, "purpose", "purpose");
            TextArea<String> noteArea = new TextArea<String>("note", new PropertyModel<String>(this, "note"));
            editInterface.add(noteArea);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private TextField<String> addTextField(PortDto pvc, String id,
                                           String attributeName) {
        return addTextField(pvc, id, attributeName, null, false);
    }

    private TextField<String> addTextField(PortDto channel, String id,
                                           String attributeName, AbstractValidator<String> validator, boolean required) {
        TextField<String> tf = new TextField<String>(id, new PropertyModel<String>(this, attributeName));
        editInterface.add(tf);
        tf.setRequired(required);
        if (validator != null) {
            tf.add(validator);
        }
        return tf;
    }

    private void processEdit() {
        if (parentPort == null) {
            throw new IllegalStateException("No parent port selected.");
        }
        try {
            checkTimeSlotFormat(this.timeslot);
            ChannelPortCommandBuilder builder;
            if (this.channelPort == null) {
                builder = new ChannelPortCommandBuilder(parentPort, editorName);
                builder.setSource(DiffCategory.INVENTORY.name());
            } else {
                builder = new ChannelPortCommandBuilder(channelPort, editorName);
            }
            builder.setIpNeeds(false);
            builder.setIfName(ifName);
            builder.setOperStatus(operStatus);
            builder.setTimeSlot(timeslot);
            builder.setChannelGroup(channelGroup);
            builder.setBandwidth(bandwidth);
            builder.setPurpose(purpose);
            builder.setNote(note);
            builder.buildCommand();
            ShellConnector.getInstance().execute(builder);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private final Pattern timeslotPattern = Pattern.compile("([0-9]+)(-[0-9]+)?");

    private void checkTimeSlotFormat(String timeslot) {
        if (timeslot == null) {
            return;
        }
        Matcher matcher = timeslotPattern.matcher(timeslot);
        if (!matcher.matches()) {
            throw new IllegalStateException("Illegal Format of TimeSlot[" + timeslot + "].");
        }
    }

    public WebPage getBackPage() {
        return backPage;
    }

    public PortDto getParentPort() {
        return parentPort;
    }

    public void setParentPort(PortDto parentPort) {
        this.parentPort = parentPort;
    }

    public TdmSerialIfDto getChannelPort() {
        return channelPort;
    }

    public NodeDto getNode() {
        return node;
    }
}