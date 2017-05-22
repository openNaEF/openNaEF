package voss.multilayernms.inventory.web.mpls;

import naef.dto.NodeDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import naef.dto.mpls.RsvpLspHopSeriesIdPoolDto;
import naef.dto.mpls.RsvpLspIdPoolDto;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.TaskUtil;
import voss.multilayernms.inventory.constants.MplsNmsPoolConstants;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.GenericRenderer;
import voss.multilayernms.inventory.renderer.RsvpLspRenderer;
import voss.multilayernms.inventory.web.parts.OperationStatusChoiceRenderer;
import voss.nms.inventory.builder.RsvpLspCommandBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.model.RsvpLspPoolModel;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.PageUtil;
import voss.nms.inventory.util.RsvpLspUtil;

import java.io.IOException;
import java.util.*;

public class RsvpLspEditPage extends WebPage {
    public static final String OPERATION_NAME = "RsvpLspEdit";

    private Form<Void> editForm;
    private final WebPage backPage;
    private final RsvpLspIdPoolDto pool;
    private final RsvpLspPoolModel model;
    private final RsvpLspDto lsp;
    private Map<String, String> attributes = new HashMap<String, String>();
    private Map<String, Long> long_attributes = new HashMap<String, Long>();
    private NodeDto ingress;
    private NodeDto egress;
    private RsvpLspHopSeriesDto primaryPath;
    private RsvpLspHopSeriesDto secondaryPath;
    private String primaryPathOperStatus;
    private String secondaryPathOperStatus;
    private String activePathTarget;
    private Long bandwidth;
    private String note;
    private Date setupDate;
    private Date operationBeginDate;
    private final String editorName;

    public void renew() {
        if (this.lsp != null) {
            this.lsp.renew();
        }
    }

    public RsvpLspEditPage(WebPage backPage, final RsvpLspDto lsp) {
        this(backPage, lsp, null);
    }

    public RsvpLspEditPage(WebPage backPage, final RsvpLspDto lsp, final NodeDto ingress) {
        if (lsp == null) {
            throw new IllegalArgumentException();
        }
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;
            this.pool = lsp.getIdPool();
            if (pool == null) {
                throw new IllegalStateException("pool not found.");
            }
            this.model = new RsvpLspPoolModel(pool);
            this.lsp = lsp;
            if (ingress != null) {
                this.ingress = ingress;
            } else if (lsp != null) {
                this.ingress = RsvpLspUtil.getIngressNode(lsp);
            }
            this.egress = RsvpLspUtil.getEgressNode(lsp);
            this.bandwidth = DtoUtil.getLong(lsp,MPLSNMS_ATTR.BANDWIDTH) == null ? Long.getLong("0") : DtoUtil.getLong(lsp,MPLSNMS_ATTR.BANDWIDTH);

            add(new FeedbackPanel("feedback"));
            Label poolName = new Label("poolName", new PropertyModel<String>(model, "poolName"));
            add(poolName);
            Label range = new Label("range", new PropertyModel<String>(model, "range"));
            add(range);
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

            boolean noTask = !TaskUtil.hasTask(lsp);

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

            Label lspNameLabel = new Label("lspName", Model.of(RsvpLspRenderer.getLspName(lsp)));
            editForm.add(lspNameLabel);
            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            DropDownChoice<String> statusSelection = new DropDownChoice<String>("operStatus",
                    new PropertyModel<String>(attributes, MPLSNMS_ATTR.OPER_STATUS),
                    conn.getStatusList(),
                    new OperationStatusChoiceRenderer());
            DtoUtil.putValuesWithDefault(attributes, MPLSNMS_ATTR.OPER_STATUS, lsp, "down");
            editForm.add(statusSelection);

            DropDownChoice<String> primaryPathStatusSelection = new DropDownChoice<String>("primaryPathOperStatus",
                    new PropertyModel<String>(this, "primaryPathOperStatus"),
                    conn.getStatusList(),
                    new OperationStatusChoiceRenderer());
            this.primaryPathOperStatus = RsvpLspRenderer.getMainPathOperationStatus(lsp);
            editForm.add(primaryPathStatusSelection);

            DropDownChoice<String> secondaryPathStatusSelection = new DropDownChoice<String>("secondaryPathOperStatus",
                    new PropertyModel<String>(this, "secondaryPathOperStatus"),
                    conn.getStatusList(),
                    new OperationStatusChoiceRenderer());
            this.secondaryPathOperStatus = RsvpLspRenderer.getBackupPathOperationStatus(lsp);
            editForm.add(secondaryPathStatusSelection);

            DropDownChoice<String> facilityStatusList = new DropDownChoice<String>("facilityStatus",
                    new PropertyModel<String>(attributes, MPLSNMS_ATTR.FACILITY_STATUS),
                    conn.getFacilityStatusList());
            DtoUtil.putValuesWithDefault(attributes, MPLSNMS_ATTR.FACILITY_STATUS, lsp, "In Use");
            editForm.add(facilityStatusList);

            DropDownChoice<NodeDto> ingressNodeList = new DropDownChoice<NodeDto>("ingressNodes",
                    new PropertyModel<NodeDto>(this, "ingress"),
                    new ArrayList<NodeDto>(conn.getNodes()),
                    new ChoiceRenderer<NodeDto>("name"));
            ingressNodeList.setRequired(true);
            ingressNodeList.setOutputMarkupId(true);
            editForm.add(ingressNodeList);

            DropDownChoice<NodeDto> egressNodeList = new DropDownChoice<NodeDto>("egressNodes",
                    new PropertyModel<NodeDto>(this, "egress"),
                    new ArrayList<NodeDto>(conn.getNodes()),
                    new ChoiceRenderer<NodeDto>("name"));
            egressNodeList.setRequired(true);
            egressNodeList.setOutputMarkupId(true);
            editForm.add(egressNodeList);

            final IModel<List<RsvpLspHopSeriesDto>> pathChoices = new AbstractReadOnlyModel<List<RsvpLspHopSeriesDto>>() {
                private static final long serialVersionUID = 1L;

                @Override
                public List<RsvpLspHopSeriesDto> getObject() {
                    try {
                        List<RsvpLspHopSeriesDto> result = new ArrayList<RsvpLspHopSeriesDto>();
                        NodeDto egress = getEgress();
                        if (egress == null) {
                            return result;
                        }
                        NodeDto ingress = getIngress();
                        if (ingress == null) {
                            return result;
                        }
                        MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
                        RsvpLspHopSeriesIdPoolDto pool = conn.getRsvpLspHopSeriesIdPool(
                                MplsNmsPoolConstants.DEFAULT_RSVPLSP_PATH_POOL);
                        for (RsvpLspHopSeriesDto path : pool.getUsers()) {
                            if (RsvpLspUtil.isPathBetween(path, ingress, egress)) {
                                result.add(path);
                            }
                        }
                        return result;
                    } catch (Exception e) {
                        throw new IllegalStateException("failed to get areas.", e);
                    }
                }
            };

            final DropDownChoice<RsvpLspHopSeriesDto> primaryPathList = new DropDownChoice<RsvpLspHopSeriesDto>("path1",
                    new PropertyModel<RsvpLspHopSeriesDto>(this, "primaryPath"),
                    pathChoices,
                    new ChoiceRenderer<RsvpLspHopSeriesDto>("name"));
            primaryPathList.setNullValid(true);
            primaryPathList.setOutputMarkupId(true);
            editForm.add(primaryPathList);
            this.primaryPath = this.lsp.getHopSeries1();

            final DropDownChoice<RsvpLspHopSeriesDto> secondaryPathList = new DropDownChoice<RsvpLspHopSeriesDto>("path2",
                    new PropertyModel<RsvpLspHopSeriesDto>(this, "secondaryPath"),
                    pathChoices,
                    new ChoiceRenderer<RsvpLspHopSeriesDto>("name"));
            secondaryPathList.setNullValid(true);
            secondaryPathList.setOutputMarkupId(true);
            editForm.add(secondaryPathList);
            this.secondaryPath = this.lsp.getHopSeries2();

            ingressNodeList.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    setPrimaryPath(null);
                    setSecondaryPath(null);
                    target.addComponent(primaryPathList);
                    target.addComponent(secondaryPathList);
                }
            });

            egressNodeList.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    setPrimaryPath(null);
                    setSecondaryPath(null);
                    target.addComponent(primaryPathList);
                    target.addComponent(secondaryPathList);
                }
            });

            DropDownChoice<String> activePathList = new DropDownChoice<String>("actives",
                    new PropertyModel<String>(this, "activePathTarget"),
                    RsvpLspCommandBuilder.getActivePathCondidates());
            activePathList.setNullValid(true);
            editForm.add(activePathList);
            this.activePathTarget = getActivePathName();

            TextArea<String> noteArea = new TextArea<String>("noteArea",
                    new PropertyModel<String>(attributes, MPLSNMS_ATTR.NOTE));
            DtoUtil.putValues(attributes, MPLSNMS_ATTR.NOTE, lsp);
            editForm.add(noteArea);

            TextField<Date> setupDateField = new TextField<Date>("setupDate",
                    new PropertyModel<Date>(this, "setupDate"));
            setupDateField.setOutputMarkupId(true);
            editForm.add(setupDateField);

            TextField<Date> operationStartDateField = new TextField<Date>("operationBeginDate",
                    new PropertyModel<Date>(this, "operationBeginDate"));
            operationStartDateField.setOutputMarkupId(true);
            editForm.add(operationStartDateField);

            TextField<Long> bandwidth = new TextField<>("bandwidth",
                    new PropertyModel<Long>(this, "bandwidth"));
            bandwidth.setOutputMarkupId(true);
            bandwidth.setType(Long.class);
            editForm.add(bandwidth);

        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void processUpdate() throws InventoryException, IOException, ExternalServiceException {
        if (this.primaryPath != null && primaryPath != null) {
            if (DtoUtil.isSameMvoEntity(primaryPath, secondaryPath)) {
                throw new IllegalStateException("You selected same path as primary and backup.");
            }
        }
        RsvpLspCommandBuilder builder = new RsvpLspCommandBuilder(this.lsp, editorName);
        if (this.primaryPath != null) {
            builder.setPrimaryPathHopName(this.primaryPath.getAbsoluteName());
            builder.setPrimaryPathOperStatus(this.primaryPathOperStatus);
        } else {
            builder.setPrimaryPathHopName(null);
            builder.setPrimaryPathOperStatus(null);
        }
        if (this.secondaryPath != null) {
            builder.setSecondaryPathHopName(this.secondaryPath.getAbsoluteName());
            builder.setSecondaryPathOperStatus(this.secondaryPathOperStatus);
        } else {
            builder.setSecondaryPathHopName(null);
            builder.setSecondaryPathOperStatus(null);
        }
        String setupDateValue = null;
        if (this.setupDate != null) {
            setupDateValue = InventoryBuilder.getInventoryDateString(setupDate);
        }
        attributes.put(MPLSNMS_ATTR.SETUP_DATE, setupDateValue);
        String operationBeginDateValue = null;
        if (this.operationBeginDate != null) {
            operationBeginDateValue = InventoryBuilder.getInventoryDateString(operationBeginDate);
        }
        attributes.put(MPLSNMS_ATTR.OPERATION_BEGIN_DATE, operationBeginDateValue);
        builder.setActivePath(this.activePathTarget);
        builder.setValues(attributes);
        builder.setValue(MPLSNMS_ATTR.BANDWIDTH,this.bandwidth);
        builder.buildCommand();
        ShellConnector.getInstance().execute(builder);
    }

    private boolean isPathChanged(RsvpLspHopSeriesDto before, RsvpLspHopSeriesDto after) {
        return DtoUtil.isSameMvoEntity(before, after);
    }

    private String getPathName(RsvpLspHopSeriesDto path) {
        if (path == null) {
            return null;
        }
        return path.getAbsoluteName();
    }

    private String getActivePathName() {
        if (this.lsp.getActiveHopSeries() == null) {
            return null;
        } else {
            RsvpLspHopSeriesDto path = this.lsp.getActiveHopSeries();
            if (this.lsp.getHopSeries1() != null
                    && DtoUtil.isSameMvoEntity(path, this.lsp.getHopSeries1())) {
                return RsvpLspCommandBuilder.ACTIVE_PATH_IS_PRIMARY;
            } else if (this.lsp.getHopSeries2() != null
                    && DtoUtil.isSameMvoEntity(path, this.lsp.getHopSeries2())) {
                return RsvpLspCommandBuilder.ACTIVE_PATH_IS_SECONDARY;
            }
        }
        return null;
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public NodeDto getEgress() {
        return this.egress;
    }

    public NodeDto getIngress() {
        return this.ingress;
    }

    public RsvpLspHopSeriesDto getPrimaryPath() {
        return this.primaryPath;
    }

    public void setPrimaryPath(RsvpLspHopSeriesDto primaryPath) {
        this.primaryPath = primaryPath;
    }

    public RsvpLspHopSeriesDto getSecondaryPath() {
        return this.secondaryPath;
    }

    public void setSecondaryPath(RsvpLspHopSeriesDto secondaryPath) {
        this.secondaryPath = secondaryPath;
    }

    public String getActivePathTarget() {
        return this.activePathTarget;
    }

    public void setActivePathTarget(String activePathTarget) {
        this.activePathTarget = activePathTarget;
    }

    public RsvpLspDto getLsp() {
        return lsp;
    }

    public void setEgress(NodeDto egress) {
        this.egress = egress;
    }

    public String getNote() {
        return this.note;
    }

    public void setNote(String s) {
        this.note = s;
    }

    public Long getBandwidth() {
        return this.bandwidth;
    }

    public void setBandwidth(Long bandwidth) {
        this.bandwidth = bandwidth;
    }

    public Date getTestFinishedDate() {
        return setupDate;
    }

    public void setTestFinishedDate(Date testFinishedDate) {
        this.setupDate = testFinishedDate;
    }

    public Date getOperationStartDate() {
        return operationBeginDate;
    }

    public void setOperationStartDate(Date operationStartDate) {
        this.operationBeginDate = operationStartDate;
    }
}