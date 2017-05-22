package voss.multilayernms.inventory.web.vrf;


import naef.dto.PortDto;
import naef.dto.vrf.VrfDto;
import naef.dto.vrf.VrfIfDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.builder.VrfBuilder;
import voss.multilayernms.inventory.renderer.GenericRenderer;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.model.RenewableAbstractReadOnlyModel;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.PageUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VrfEditPage extends WebPage {
    public static final String OPERATION_NAME = "VrfIfEditor";

    private final WebPage backPage;
    private VrfDto vrf;
    private String vrfID;
    private String note = null;
    private final String editorName;
    private final Form<Void> form;
    private final RenewableAbstractReadOnlyModel<List<PortDto>> rowsModel;


    public VrfEditPage(final WebPage backPage, final VrfDto vrf) {
        this.backPage = backPage;
        this.vrf = vrf;
        this.vrfID = (vrf == null ? null : vrf.getStringId());
        this.note = GenericRenderer.getNote(vrf);

        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);

            Label headTitleLabel = new Label("headTitle", "Port(Router) VRF Edit on VRF ID "
                    + vrf.getStringId());
            add(headTitleLabel);

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

            this.form = new Form<Void>("form");
            add(form);

            Button proceedButton = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    processUpdate();
                    PageUtil.setModelChanged(getBackPage());
                    setResponsePage(getBackPage());
                }
            };
            this.form.add(proceedButton);

            TextArea<String> noteArea = new TextArea<String>("note", new PropertyModel<String>(this, "note"));
            this.form.add(noteArea);

            Link<Void> editLink = new Link<Void>("addPorts") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    NodeVrfPortSelectionPage page = new NodeVrfPortSelectionPage(VrfEditPage.this, getVrf(), null);
                    setResponsePage(page);
                }
            };
            this.form.add(editLink);


            this.rowsModel = new RenewableAbstractReadOnlyModel<List<PortDto>>() {
                private static final long serialVersionUID = 1L;
                private List<PortDto> rows = null;

                @Override
                public List<PortDto> getObject() {
                    if (rows == null) {
                        renew();
                    }
                    return rows;
                }

                public void renew() {
                    List<VrfIfDto> vrfIfs = new ArrayList<VrfIfDto>(getVrf().getMemberVrfifs());
                    List<PortDto> rows = new ArrayList<PortDto>();
                    for (VrfIfDto vrfIf : vrfIfs) {
                        List<PortDto> attached = new ArrayList<PortDto>();
                        for (PortDto p : vrfIf.getAttachedPorts()) {
                            attached.add(p);
                        }
                        for (PortDto port : attached) {
                            rows.add(port);
                        }
                    }
                    this.rows = rows;
                }
            };

            ListView<PortDto> attachedPortList = new ListView<PortDto>("attachedPorts", rowsModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<PortDto> item) {
                    final PortDto port = item.getModelObject();
                    String fqn = NameUtil.getNodeIfName(port);
                    Label label = new Label("fqn", new Model<String>(fqn));
                    item.add(label);
                }
            };
            this.form.add(attachedPortList);

        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    @SuppressWarnings("unused")
    private void processDelete() {
    }

    private void processUpdate() {

        try {
            VrfDto vrf = getVrf();
            ShellCommands commands = new ShellCommands(editorName);
            commands.addVersionCheckTarget(vrf);
            Map<String, String> attributes = new HashMap<String, String>();
            attributes.put(MPLSNMS_ATTR.NOTE, note);
            VrfBuilder.buildVrfAttributeUpdateCommands(commands, vrf, attributes);
            commands.addLastEditCommands();
            ShellConnector.getInstance().execute2(commands);
            PageUtil.setModelChanged(getBackPage());
            setResponsePage(getBackPage());
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public WebPage getBackPage() {
        return backPage;
    }


    public String getVrfId() {
        return this.vrfID;
    }

    public void setVrfId(String id) {
        this.vrfID = id;
    }

    public VrfDto getVrf() {
        return vrf;
    }

    @Override
    protected void onModelChanged() {
        try {
            this.vrf.renew();
            rowsModel.renew();
            super.onModelChanged();
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

}