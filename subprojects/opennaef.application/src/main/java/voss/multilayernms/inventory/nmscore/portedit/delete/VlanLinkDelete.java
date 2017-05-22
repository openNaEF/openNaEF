package voss.multilayernms.inventory.nmscore.portedit.delete;

import jp.iiga.nmt.core.model.portedit.VlanEditModel;
import naef.dto.LinkDto;
import naef.dto.NetworkDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vlan.VlanLinkDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.VlanLinkRenderer;
import voss.nms.inventory.builder.AliasPortCommandBuilder;
import voss.nms.inventory.builder.VlanIfBindingCommandBuilder;
import voss.nms.inventory.builder.VlanIfCommandBuilder;
import voss.nms.inventory.builder.VlanLinkCommandBuilder;
import voss.nms.inventory.util.VlanUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class VlanLinkDelete implements IPortDelete {

    private VlanEditModel model;
    private String user;

    private PortDto vlanif1 = null;
    private PortDto vlanif2 = null;
    private PortDto port1 = null;
    private PortDto port2 = null;

    public VlanLinkDelete(VlanEditModel model, String user) {
        this.model = model;
        this.user = user;
    }

    @Override
    public void delete() throws RuntimeException, IllegalStateException, IOException, InventoryException, ExternalServiceException {
        try {
            List<String> mvoids = model.getVlanLinkMvoIds();
            if (mvoids.size() > 0) {
                List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();
                for (String mvoid : mvoids) {
                    VlanLinkDto vlanlink = MplsNmsInventoryConnector.getInstance().getMvoDto(mvoid, VlanLinkDto.class);
                    if (vlanlink == null) {
                        throw new IllegalStateException("The specified VLAN link was not found.");
                    } else if (vlanlink != null) {
                        vlanif1 = VlanLinkRenderer.getPort1(vlanlink);
                        vlanif2 = VlanLinkRenderer.getPort2(vlanlink);
                        Set<NetworkDto> phisicallinks = vlanlink.getLowerLayerLinks();
                        if (phisicallinks != null && phisicallinks.size() == 1) {
                            for (NetworkDto phisicallink : phisicallinks) {
                                if (phisicallink instanceof LinkDto) {
                                    PortDto phisicalport1 = VlanLinkRenderer.getPhysicalPort1((LinkDto) phisicallink);
                                    PortDto phisicalport2 = VlanLinkRenderer.getPhysicalPort2((LinkDto) phisicallink);
                                    if (DtoUtil.isSameMvoEntity(phisicalport1.getNode(), vlanif1.getNode()) && DtoUtil.isSameMvoEntity(phisicalport2.getNode(), vlanif2.getNode())) {
                                        this.port1 = phisicalport1;
                                        this.port2 = phisicalport2;
                                    } else if (DtoUtil.isSameMvoEntity(phisicalport2.getNode(), vlanif1.getNode()) && DtoUtil.isSameMvoEntity(phisicalport1.getNode(), vlanif2.getNode())) {
                                        this.port1 = phisicalport2;
                                        this.port2 = phisicalport1;
                                    }
                                }
                            }
                        }
                        CommandBuilder vlanlinkBuilder = new VlanLinkCommandBuilder(vlanlink, user);
                        vlanlinkBuilder.buildDeleteCommand();
                        commandBuilderList.add(vlanlinkBuilder);
                        VlanIfCommandBuilder vlanifbuilder = null;
                        VlanIfCommandBuilder vlanifbuilderforSW = null;
                        VlanIfBindingCommandBuilder vlanifbindingbuilder = null;
                        if (VlanUtil.isRouterVlanIf(vlanif1)) {
                            if (vlanif1.getAliases().size() > 0) {
                                for (PortDto aliasp : vlanif1.getAliases()) {
                                    AliasPortCommandBuilder aliasportbuilder = new AliasPortCommandBuilder(aliasp, user);
                                    aliasportbuilder.buildDeleteCommand();
                                    commandBuilderList.add(aliasportbuilder);
                                }
                            }
                            vlanifbuilder = new VlanIfCommandBuilder(port1, (VlanIfDto) vlanif1, user);
                            vlanifbuilder.setRemoveVlanLink(false);
                            vlanifbuilder.setPreCheckEnable(false);
                            vlanifbuilder.buildDeleteCommand();
                            commandBuilderList.add(vlanifbuilder);
                        }
                        else if (VlanUtil.isSwitchVlanIf((VlanIfDto) vlanif1)) {
                            vlanifbindingbuilder = new VlanIfBindingCommandBuilder((VlanIfDto) vlanif1, user);
                            vlanifbindingbuilder.removeTaggedPort(port1);
                            vlanifbindingbuilder.setPreCheckEnable(false);
                            vlanifbindingbuilder.buildCommand();
                            commandBuilderList.add(vlanifbindingbuilder);

                        }

                        if (VlanUtil.isRouterVlanIf(vlanif2)) {
                            if (vlanif2.getAliases().size() > 0) {
                                for (PortDto aliasp : vlanif2.getAliases()) {
                                    AliasPortCommandBuilder aliasportbuilder = new AliasPortCommandBuilder(aliasp, user);
                                    aliasportbuilder.buildDeleteCommand();
                                    commandBuilderList.add(aliasportbuilder);
                                }
                            }
                            vlanifbuilder = new VlanIfCommandBuilder(port2, (VlanIfDto) vlanif2, user);
                            vlanifbuilder.setRemoveVlanLink(false);
                            vlanifbuilder.setPreCheckEnable(false);
                            vlanifbuilder.buildDeleteCommand();
                            commandBuilderList.add(vlanifbuilder);
                        }
                        else if (VlanUtil.isSwitchVlanIf((VlanIfDto) vlanif2)) {
                            vlanifbindingbuilder = new VlanIfBindingCommandBuilder((VlanIfDto) vlanif2, user);
                            vlanifbindingbuilder.removeTaggedPort(port2);
                            vlanifbindingbuilder.setPreCheckEnable(false);
                            vlanifbindingbuilder.buildCommand();
                            commandBuilderList.add(vlanifbindingbuilder);

                        }
                    }
                }


                List<String> switch_vlanifs_mvoIds = new ArrayList<String>();

                for (String mvoid : mvoids) {
                    VlanLinkDto vlanlink = MplsNmsInventoryConnector.getInstance().getMvoDto(mvoid, VlanLinkDto.class);
                    if (vlanlink == null) {
                        throw new IllegalStateException("The specified VLAN link was not found.");
                    } else if (vlanlink != null) {
                        vlanif1 = VlanLinkRenderer.getPort1(vlanlink);
                        vlanif2 = VlanLinkRenderer.getPort2(vlanlink);
                        if (VlanUtil.isSwitchVlanIf((VlanIfDto) vlanif1)) {
                            if (!switch_vlanifs_mvoIds.contains(DtoUtil.getMvoId(vlanif1).toString())) {
                                switch_vlanifs_mvoIds.add(DtoUtil.getMvoId(vlanif1).toString());
                            }
                        }
                        if (VlanUtil.isSwitchVlanIf((VlanIfDto) vlanif2)) {
                            if (!switch_vlanifs_mvoIds.contains(DtoUtil.getMvoId(vlanif2).toString())) {
                                switch_vlanifs_mvoIds.add(DtoUtil.getMvoId(vlanif2).toString());
                            }
                        }
                    }
                }

                VlanIfCommandBuilder vlanifbuilder2 = null;
                if (switch_vlanifs_mvoIds.size() > 0) {
                    for (String switch_vlanif : switch_vlanifs_mvoIds) {
                        VlanIfDto vlanif = MplsNmsInventoryConnector.getInstance().getMvoDto(switch_vlanif, VlanIfDto.class);
                        vlanifbuilder2 = new VlanIfCommandBuilder((NodeDto) vlanif.getNode(), (VlanIfDto) vlanif, user);
                        vlanifbuilder2.setPreCheckEnable(false);
                        vlanifbuilder2.setRemoveVlanLink(false);
                        vlanifbuilder2.buildDeleteCommand();
                        commandBuilderList.add(vlanifbuilder2);
                    }
                }
                ShellConnector.getInstance().executes(commandBuilderList);
            }
        } catch (InventoryException e) {
            log.debug("InventoryException", e);
            throw e;
        } catch (IllegalStateException e) {
            log.debug("IllegalStateException", e);
            throw e;
        } catch (RuntimeException e) {
            log.debug("RuntimeException", e);
            throw e;
        }
    }
}