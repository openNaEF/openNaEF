package voss.multilayernms.inventory.web.mpls;

import naef.dto.IdPoolDto;
import naef.dto.NetworkDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vpls.VplsDto;
import naef.dto.vrf.VrfDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ATTR;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.Util;
import voss.multilayernms.inventory.web.location.LocationDeletePage;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.PageUtil;

public class ReleaseNetworkIDConfirmationPage extends WebPage {
    private final String editorName;
    private final WebPage backPage;

    public ReleaseNetworkIDConfirmationPage(final WebPage backPage, final String operationName, final NetworkDto deleteTarget) {
        this.backPage = backPage;
        try {
            this.editorName = AAAWebUtil.checkAAA(this, operationName);
            if (deleteTarget == null) {
                throw new IllegalStateException("target is null.");
            }
            String name = NameUtil.getCaption(deleteTarget);
            Label confirmLabel = new Label("name", Model.of(name));
            add(confirmLabel);

            Form<Void> form = new Form<Void>("removeConfirmationForm");
            add(form);

            Button proceed = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        IdPoolDto<?, ?, ?> pool = null;
                        String idType = null;
                        String poolType = null;
                        ShellCommands commands = new ShellCommands(editorName);
                        if (deleteTarget instanceof PseudowireDto) {
                            PseudowireDto pw = (PseudowireDto) deleteTarget;
                            if (!Util.isAllNull(pw.getAc1(), pw.getAc2())) {
                                throw new IllegalStateException("this pw has ac. remove ac from pw at first.");
                            }
                            pool = pw.getStringIdPool();
                            idType = ATTR.ATTR_PW_ID_STRING;
                            poolType = ATTR.ATTR_PW_POOL_STRING;
                            InventoryBuilder.changeContext(commands, pw);
                        } else if (deleteTarget instanceof VlanDto) {
                            VlanDto vlan = (VlanDto) deleteTarget;
                            if (vlan.getMemberVlanifs().size() > 0) {
                                throw new IllegalStateException("this vlan has member.");
                            }
                            pool = vlan.getIdPool();
                            idType = ATTR.ATTR_VLAN_ID;
                            poolType = ATTR.ATTR_VLAN_POOL;
                            InventoryBuilder.changeContext(commands, vlan);
                        } else if (deleteTarget instanceof VplsDto) {
                            VplsDto vpls = (VplsDto) deleteTarget;
                            if (vpls.getMemberVplsifs().size() > 0) {
                                throw new IllegalStateException("this vpls has member.");
                            }
                            pool = vpls.getIntegerIdPool();
                            idType = ATTR.ATTR_VPLS_ID_STRING;
                            poolType = ATTR.ATTR_VPLS_POOL_STRING;
                            InventoryBuilder.changeContext(commands, vpls);
                        } else if (deleteTarget instanceof VrfDto) {
                            VrfDto vrf = (VrfDto) deleteTarget;
                            if (vrf.getMemberVrfifs().size() > 0) {
                                throw new IllegalStateException("this vrf has member.");
                            }
                            pool = vrf.getIntegerIdPool();
                            idType = ATTR.ATTR_VRF_ID_STRING;
                            poolType = ATTR.ATTR_VRF_POOL_STRING;
                            InventoryBuilder.changeContext(commands, vrf);
                        } else if (deleteTarget instanceof RsvpLspDto) {
                            RsvpLspDto lsp = (RsvpLspDto) deleteTarget;
                            if (!Util.isAllNull(lsp.getHopSeries1(), lsp.getHopSeries2())) {
                                throw new IllegalStateException("this lsp has path.");
                            }
                            pool = lsp.getIdPool();
                            idType = ATTR.ATTR_RSVPLSP_ID;
                            poolType = ATTR.ATTR_RSVPLSP_POOL;
                            InventoryBuilder.changeContext(commands, lsp);
                        } else if (deleteTarget instanceof RsvpLspHopSeriesDto) {
                            RsvpLspHopSeriesDto path = (RsvpLspHopSeriesDto) deleteTarget;
                            if (path.getHops().size() > 0) {
                                throw new IllegalStateException("this path has hop.");
                            }
                            pool = path.getIdPool();
                            idType = ATTR.ATTR_PATH_ID;
                            poolType = ATTR.ATTR_PATH_POOL;
                            InventoryBuilder.changeContext(commands, path);
                        } else {
                            throw new IllegalStateException("unexptected type:. " + deleteTarget.getAbsoluteName());
                        }
                        InventoryBuilder.buildNetworkIDReleaseCommand(commands, idType, poolType);
                        commands.addVersionCheckTarget(pool);
                        ShellConnector.getInstance().execute2(commands);
                        PageUtil.setModelChanged(ReleaseNetworkIDConfirmationPage.this);
                        setResponsePage(getBackPage());
                    } catch (InventoryException e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            form.add(proceed);

            Button back = new Button("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    setResponsePage(backPage);
                }
            };
            form.add(back);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    @SuppressWarnings("unused")
    private Logger log() {
        return LoggerFactory.getLogger(LocationDeletePage.class);
    }

}