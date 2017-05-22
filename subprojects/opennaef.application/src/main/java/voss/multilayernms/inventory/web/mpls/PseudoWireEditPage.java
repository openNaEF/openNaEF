package voss.multilayernms.inventory.web.mpls;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.PseudowireStringIdPoolDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspIdPoolDto;
import org.apache.wicket.datetime.DateConverter;
import org.apache.wicket.datetime.PatternDateConverter;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.Util;
import voss.multilayernms.inventory.constants.CustomerConstants;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.constants.MplsNmsPoolConstants;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.GenericRenderer;
import voss.multilayernms.inventory.renderer.PseudoWireRenderer;
import voss.multilayernms.inventory.util.FacilityStatusUtil;
import voss.multilayernms.inventory.web.parts.OperationStatusChoiceRenderer;
import voss.nms.inventory.builder.PseudoWireStringTypeCommandBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.model.PseudoWireStringPoolModel;
import voss.nms.inventory.model.RenewableAbstractReadOnlyModel;
import voss.nms.inventory.util.*;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.*;

public class PseudoWireEditPage extends WebPage {
    public static final String OPERATION_NAME = "PseudoWireEdit";
    private static final Logger log = LoggerFactory.getLogger(PseudoWireEditPage.class);

    private Form<Void> editForm;
    private final WebPage backPage;
    private final PseudowireStringIdPoolDto pool;
    private final PseudoWireStringPoolModel model;
    private final LspSelectionModel model1;
    private final LspSelectionModel model2;
    private final PseudowireDto currentPwDto;
    private RsvpLspDto lsp1;
    private RsvpLspDto lsp2;
    private final List<PseudoWireAcLinkCellPanel> panels = new ArrayList<PseudoWireAcLinkCellPanel>();
    private final DropDownChoice<RsvpLspDto> lsp1List;
    private final DropDownChoice<RsvpLspDto> lsp2List;
    private FacilityStatus facilityStatus;
    private Map<Integer, AcWrapper> wrappers = new HashMap<Integer, AcWrapper>();
    private Map<String, String> attributes = new HashMap<String, String>();
    private String note;
    private String customerName = null;
    private String apName1 = null;
    private String apName2 = null;
    private Date startDate;
    private Date testDate;
    private final String editorName;

    public void renew() {
        if (this.currentPwDto != null) {
            this.currentPwDto.renew();
        }
    }

    public PseudoWireEditPage(WebPage backPage, final PseudowireDto pw) {
        this.customerName = PseudoWireRenderer.getCustomerName(pw);
        this.apName1 = PseudoWireRenderer.getApName1(pw);
        this.apName2 = PseudoWireRenderer.getApName2(pw);
        if (pw == null) {
            throw new IllegalArgumentException();
        }
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;
            this.pool = pw.getStringIdPool();
            if (pool == null) {
                throw new IllegalStateException("pool not found.");
            }
            this.model = new PseudoWireStringPoolModel(pool);
            this.currentPwDto = pw;
            if (pw != null) {
                NodeDto from = (pw.getAc1() != null ? pw.getAc1().getNode() : null);
                NodeDto to = (pw.getAc2() != null ? pw.getAc2().getNode() : null);
                Set<RsvpLspDto> lsps = new HashSet<RsvpLspDto>(pw.getRsvpLsps());
                for (RsvpLspDto lsp : lsps) {
                    NodeDto ingress = RsvpLspUtil.getIngressNode(lsp);
                    if (DtoUtil.isSameMvoEntity(from, ingress)) {
                        this.lsp1 = lsp;
                    } else if (DtoUtil.isSameMvoEntity(to, ingress)) {
                        this.lsp2 = lsp;
                    }
                }
                this.facilityStatus = FacilityStatusUtil.getStatus(pw);
                this.testDate = PseudoWireRenderer.getOpenedTimeAsDate(pw);
                this.startDate = PseudoWireRenderer.getOperationBeginTimeAsDate(pw);
            }

            add(new FeedbackPanel("feedback"));
            Label poolName = new Label("poolName", new PropertyModel<String>(model, "poolName"));
            add(poolName);
            Label range = new Label("range", new PropertyModel<String>(model, "range"));
            add(range);
            Label remainsLabel = new Label("remains", new PropertyModel<String>(model, "remains"));
            add(remainsLabel);
            Label used = new Label("used", new PropertyModel<String>(model, "used"));
            add(used);
            Label purpose = new Label("purpose", new PropertyModel<String>(model, "usage"));
            add(purpose);
            Label lastEditor = new Label("lastEditor", new Model<String>(GenericRenderer.getLastEditor(pool)));
            add(lastEditor);
            Label lastEditTime = new Label("lastEditTime", new Model<String>(GenericRenderer.getLastEditTime(pool)));
            add(lastEditTime);
            Label version = new Label("version", new PropertyModel<String>(model, "version"));
            add(version);

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

            editForm = new Form<Void>("editForm");
            add(editForm);

            Button proceedButton = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        processUpdate();
                        PageUtil.setModelChanged(getBackPage());
                        setResponsePage(getBackPage());
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            editForm.add(proceedButton);

            Label vcIdLabel = new Label("vcId", new Model<String>(currentPwDto.getStringId()));
            editForm.add(vcIdLabel);

            TextField<String> pwNameField = new TextField<String>("pwName",
                    new PropertyModel<String>(attributes, MPLSNMS_ATTR.PSEUDOWIRE_NAME));
            DtoUtil.putValues(attributes, MPLSNMS_ATTR.PSEUDOWIRE_NAME, currentPwDto);
            editForm.add(pwNameField);

            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            RadioChoice<String> statusSelection = new RadioChoice<String>("operStatus",
                    new PropertyModel<String>(attributes, MPLSNMS_ATTR.OPER_STATUS),
                    conn.getStatusList(),
                    new OperationStatusChoiceRenderer());
            DtoUtil.putValuesWithDefault(attributes, MPLSNMS_ATTR.OPER_STATUS, currentPwDto, "Unknown");
            editForm.add(statusSelection);

            populateAcView(1, editForm, currentPwDto);
            populateAcView(2, editForm, currentPwDto);

            this.model1 = new LspSelectionModel(1, 2);
            this.model1.renew();
            this.lsp1List = new DropDownChoice<RsvpLspDto>("lsp1",
                    new PropertyModel<RsvpLspDto>(this, "lsp1"),
                    this.model1,
                    new ChoiceRenderer<RsvpLspDto>("name"));
            this.lsp1List.setNullValid(true);
            this.lsp1List.setOutputMarkupId(true);
            this.editForm.add(this.lsp1List);

            this.model2 = new LspSelectionModel(2, 1);
            this.model2.renew();
            this.lsp2List = new DropDownChoice<RsvpLspDto>("lsp2",
                    new PropertyModel<RsvpLspDto>(this, "lsp2"),
                    this.model2,
                    new ChoiceRenderer<RsvpLspDto>("name"));
            this.lsp2List.setNullValid(true);
            this.lsp2List.setOutputMarkupId(true);
            this.editForm.add(this.lsp2List);

            DropDownChoice<FacilityStatus> facilityStatusList = new DropDownChoice<FacilityStatus>("facilityStatus",
                    new PropertyModel<FacilityStatus>(this, "facilityStatus"),
                    Arrays.asList(FacilityStatus.values()),
                    new ChoiceRenderer<FacilityStatus>("displayString"));
            editForm.add(facilityStatusList);

            DropDownChoice<String> pseudoWireTypeList = new DropDownChoice<String>("pseudoWireType",
                    new PropertyModel<String>(attributes, MPLSNMS_ATTR.PSEUDOWIRE_TYPE),
                    conn.getPseudoWireTypeList());
            DtoUtil.putValuesWithDefault(attributes, MPLSNMS_ATTR.PSEUDOWIRE_TYPE, currentPwDto, "EPIPE");
            editForm.add(pseudoWireTypeList);

            TextField<Long> bandwidthField = new TextField<Long>("bandwidth",
                    new PropertyModel<Long>(attributes, MPLSNMS_ATTR.CONTRACT_BANDWIDTH));
            DtoUtil.putValues(attributes, MPLSNMS_ATTR.CONTRACT_BANDWIDTH, currentPwDto);
            editForm.add(bandwidthField);

            TextField<String> serviceTypeField = new TextField<String>("serviceType",
                    new PropertyModel<String>(attributes, MPLSNMS_ATTR.SERVICE_TYPE));
            DtoUtil.putValues(attributes, MPLSNMS_ATTR.SERVICE_TYPE, currentPwDto);
            editForm.add(serviceTypeField);

            TextField<String> accommodationServiceTypeField = new TextField<String>(
                    "accommodationServiceType",
                    new PropertyModel<String>(attributes, MPLSNMS_ATTR.ACCOMMODATION_SERVICE_TYPE));
            DtoUtil.putValues(attributes, MPLSNMS_ATTR.ACCOMMODATION_SERVICE_TYPE, currentPwDto);
            editForm.add(accommodationServiceTypeField);

            DateConverter conv = new PatternDateConverter("yyyy/MM/dd HH:mm", false);
            DateTextField startDateField = new DateTextField("startDate",
                    new PropertyModel<Date>(this, "startDate"), conv);
            editForm.add(startDateField);

            DateTextField testDateField = new DateTextField("testDate",
                    new PropertyModel<Date>(this, "testDate"), conv);
            editForm.add(testDateField);

            TextArea<String> noteArea = new TextArea<String>("noteArea", new PropertyModel<String>(attributes, MPLSNMS_ATTR.NOTE));

            DtoUtil.putValues(attributes, MPLSNMS_ATTR.NOTE, currentPwDto);
            editForm.add(noteArea);

            editForm.add(new TextField<String>("customerName", new PropertyModel<String>(this, "customerName"), String.class));
            editForm.add(new TextField<String>("apName1", new PropertyModel<String>(this, "apName1"), String.class));
            editForm.add(new TextField<String>("apName2", new PropertyModel<String>(this, "apName2"), String.class));

        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void populateAcView(final int suffix, final Form<Void> form, final PseudowireDto pw) {
        final AcWrapper wrapper = new AcWrapper(pw, suffix);
        this.wrappers.put(suffix, wrapper);
        SubmitLink acSelectorLink = new SubmitLink("selectAC_" + suffix) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit() {
                WebPage page = new NodePseudoWireAcPortSelectionPage(PseudoWireEditPage.this,
                        wrapper.getPort(), suffix, null);
                setResponsePage(page);
            }
        };
        form.add(acSelectorLink);
        SubmitLink acDeleteLink = new SubmitLink("deleteAC_" + suffix) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit() {
                AcWrapper wrapper = wrappers.get(suffix);
                wrapper.setPort(null);
                PseudoWireEditPage.this.modelChanged();
                setResponsePage(PseudoWireEditPage.this);
            }
        };
        form.add(acDeleteLink);

        PseudoWireAcLinkCellPanel acLinkPanel = new PseudoWireAcLinkCellPanel("acLinkCell" + suffix, wrapper);
        form.add(acLinkPanel);
        this.panels.add(acLinkPanel);

        Label ifName = new Label("ifName_" + suffix, new PropertyModel<String>(wrapper, "portName"));
        form.add(ifName);
    }

    private void processUpdate() throws InventoryException, IOException, ExternalServiceException {
        AcWrapper aw1 = this.wrappers.get(1);
        AcWrapper aw2 = this.wrappers.get(2);
        if (aw1 != null && aw2 != null) {
            if (NodeUtil.isSamePort(aw1.getPort(), aw2.getPort())) {
                throw new IllegalStateException("Attachment Circuit 1 and 2 is pointing same port.");
            }
        }
        if (this.facilityStatus == null) {
            throw new IllegalStateException("facility status is not set.");
        }
        DateFormat df = GenericRenderer.getMvoDateFormat();
        if (this.startDate != null) {
            attributes.put(MPLSNMS_ATTR.OPERATION_BEGIN_DATE, df.format(this.startDate));
        }
        if (this.testDate != null) {
            attributes.put(MPLSNMS_ATTR.SETUP_DATE, df.format(this.testDate));
        }
        PseudoWireStringTypeCommandBuilder builder = new PseudoWireStringTypeCommandBuilder(currentPwDto, editorName);
        builder.setPool(pool);
        builder.setAttachmentCircuit1(aw1.getPort());
        builder.setAttachmentCircuit2(aw2.getPort());

        builder.setRsvpLsp1(getLsp1());
        builder.setRsvpLsp2(getLsp2());
        builder.setFacilityStatus(facilityStatus.getDisplayString());
        attributes.put(CustomerConstants.CUSTOMER_NAME, customerName);
        attributes.put(CustomerConstants.AP_NAME1, apName1);
        attributes.put(CustomerConstants.AP_NAME2, apName2);
        builder.setValues(attributes);
        builder.buildCommand();
        ShellConnector.getInstance().execute(builder);
    }

    public PseudowireDto getCurrentPwDto() {
        return this.currentPwDto;
    }

    public PortDto getAc(int suffix) {
        if (this.currentPwDto == null) {
            return null;
        }
        if (suffix == 1) {
            return this.currentPwDto.getAc1();
        } else if (suffix == 2) {
            return this.currentPwDto.getAc2();
        } else {
            throw new IllegalArgumentException("invalid suffix: " + suffix);
        }
    }

    public void setAc(int suffix, PortDto port) {
        log.debug("*" + suffix + "->" + NameUtil.getNodeIfName(port));
        AcWrapper ac = this.wrappers.get(suffix);
        if (ac == null) {
            ac = new AcWrapper(this.currentPwDto, suffix);
            this.wrappers.put(suffix, ac);
        }
        ac.setPort(port);
    }

    public static class AcWrapper implements Serializable {
        private static final long serialVersionUID = 1L;
        private PseudowireDto pw;
        private int suffix;
        private PortDto port;

        public AcWrapper(PseudowireDto pw, int suffix) {
            this.pw = pw;
            this.suffix = suffix;
            if (pw == null) {
                return;
            }
            if (suffix == 1) {
                this.port = pw.getAc1();
            } else if (suffix == 2) {
                this.port = pw.getAc2();
            } else {
                throw new IllegalArgumentException();
            }
        }

        public PseudowireDto getPseudoWireDto() {
            return this.pw;
        }

        public int getSuffix() {
            return suffix;
        }

        public PortDto getPort() {
            return this.port;
        }

        public void setPort(PortDto port) {
            this.port = port;
        }

        public String getNodeName() {
            if (port == null) {
                return null;
            }
            return port.getNode().getName();
        }

        public String getPortName() {
            return NameUtil.getIfName(port);
        }
    }

    @Override
    protected void onModelChanged() {
        this.currentPwDto.renew();
        this.model1.renew();
        this.lsp1List.modelChanged();
        this.model2.renew();
        this.lsp2List.modelChanged();
        for (PseudoWireAcLinkCellPanel panel : this.panels) {
            panel.modelChanged();
        }
        super.onModelChanged();
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public String getNote() {
        return this.note;
    }

    public void setNote(String s) {
        this.note = s;
    }

    public RsvpLspDto getLsp1() {
        return lsp1;
    }

    public void setLsp1(RsvpLspDto lsp1) {
        this.lsp1 = lsp1;
    }

    public RsvpLspDto getLsp2() {
        return lsp2;
    }

    public void setLsp2(RsvpLspDto lsp2) {
        this.lsp2 = lsp2;
    }

    public FacilityStatus getFacilityStatus() {
        return facilityStatus;
    }

    public void setFacilityStatus(FacilityStatus facilityStatus) {
        this.facilityStatus = facilityStatus;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getTestDate() {
        return testDate;
    }

    public void setTestDate(Date testDate) {
        this.testDate = testDate;
    }

    private class LspSelectionModel extends RenewableAbstractReadOnlyModel<List<RsvpLspDto>> {
        private static final long serialVersionUID = 1L;
        private final Integer upper;
        private final Integer lower;
        private final List<RsvpLspDto> result = new ArrayList<RsvpLspDto>();

        public LspSelectionModel(Integer upper, Integer lower) {
            if (Util.isNull(upper, lower)) {
                throw new IllegalArgumentException("Arg upper or lower has null.");
            }
            this.upper = upper;
            this.lower = lower;
        }

        @Override
        public List<RsvpLspDto> getObject() {
            return result;
        }

        public void renew() {
            try {
                this.result.clear();
                NodeDto node1 = (wrappers.get(upper) != null && wrappers.get(upper).port != null
                        ? wrappers.get(upper).port.getNode() : null);
                if (node1 == null) {
                    return;
                }
                NodeDto node2 = (wrappers.get(lower) != null && wrappers.get(lower).port != null
                        ? wrappers.get(lower).port.getNode() : null);
                if (node2 == null) {
                    return;
                }
                MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
                RsvpLspIdPoolDto pool = conn.getRsvpLspIdPool(MplsNmsPoolConstants.DEFAULT_RSVPLSP_POOL);
                for (RsvpLspDto lsp : pool.getUsers()) {
                    if (RsvpLspUtil.isLspBetween(lsp, node1, node2)) {
                        this.result.add(lsp);
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException("failed to renew.", e);
            }
        }
    }
}