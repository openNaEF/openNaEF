package voss.multilayernms.inventory.web.vlan;

import naef.dto.IdRange;
import naef.dto.vlan.VlanIdPoolDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.builder.VlanCommandBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.PageUtil;

import java.util.*;

public class VlanCreationPage extends WebPage {
    public static final String OPERATION_NAME = "RsvpLspPathCreation";
    private final VlanIdPoolDto pool;
    private final String editorName;
    private final WebPage backPage;

    private Integer vlanId;

    public VlanCreationPage(WebPage backPage, VlanIdPoolDto pool) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            if (pool == null) {
                throw new IllegalArgumentException("pool is null.");
            }
            this.pool = pool;
            this.backPage = backPage;

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

            add(new FeedbackPanel("feedback"));

            Label nodeLabel = new Label("poolName", Model.of(pool.getName()));
            add(nodeLabel);

            Form<Void> form = new Form<Void>("form");
            add(form);

            Button proceedButton = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    processCreate();
                    PageUtil.setModelChanged(getBackPage());
                    setResponsePage(getBackPage());
                }
            };
            form.add(proceedButton);

            Map<Integer, Integer> ids = new HashMap<Integer, Integer>();
            for (IdRange<Integer> idRange : pool.getIdRanges()) {
                for (int i = idRange.lowerBound; i <= idRange.upperBound; i++) {
                    ids.put(i, i);
                }
            }

            List<Integer> vlanIdList = new ArrayList<Integer>(new TreeSet<Integer>(ids.keySet()));
            DropDownChoice<Integer> vlanList = new DropDownChoice<Integer>(
                    "vlanIds",
                    new PropertyModel<Integer>(this, "vlanId"),
                    vlanIdList);
            vlanList.setRequired(true);
            form.add(vlanList);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public void processCreate() {
        try {
            VlanCommandBuilder builder = new VlanCommandBuilder(this.pool, this.editorName);
            builder.setVlanID(vlanId);
            builder.setOperStatus(MPLSNMS_ATTR.OPER_STATUS_DEFAULT);
            builder.buildCommand();
            ShellConnector.getInstance().execute(builder);
            pool.renew();
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public WebPage getBackPage() {
        return this.backPage;
    }


    public Integer getVlanId() {
        return vlanId;
    }

    public void setVlanId(Integer vlanId) {
        this.vlanId = vlanId;
    }

    public VlanIdPoolDto getPool() {
        return pool;
    }

}