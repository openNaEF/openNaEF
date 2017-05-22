package voss.multilayernms.inventory.nmscore.portedit;

import jp.iiga.nmt.core.model.portedit.HardPort;
import jp.iiga.nmt.core.model.portedit.LagPort;
import jp.iiga.nmt.core.model.portedit.PortEditModel;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIdPoolDto;
import naef.dto.vlan.VlanIfDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.nmscore.constraints.PortEditConstraints;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.nms.inventory.builder.VlanIfCommandBuilder;
import voss.nms.inventory.util.VlanUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TaggedVlanStackToPort implements IPortEditUpdate {
    private PortDto port;
    private PortEditModel model;
    private String user;
    private Integer vlanID;
    private VlanDto vlan;

    public TaggedVlanStackToPort(PortDto port, PortEditModel model, String user) {
        this.port = (PortDto) port;
        this.model = model;
        this.user = user;
    }

    @Override
    public void update() throws RuntimeException, IOException, InventoryException, ExternalServiceException {

        try {
            if (model instanceof HardPort) {
                HardPort model = (HardPort) this.model;

                VlanIdPoolDto pool = VlanUtil.getPool(model.getVlanPoolName());
                if (model.getVlanIds().size() == 0) {
                    throw new InventoryException("VLAN ID is not set.");
                } else if (model.getTagMode() == null || model.getTagMode().isEmpty()) {
                    throw new InventoryException("TagMode is not selected.");
                } else if (!PortEditConstraints.isPortSetVlanIdEnabled(port)) {
                    throw new InventoryException("It can not assign a VLAN ID to the specified port.");
                }

                List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();
                VlanIfCommandBuilder builder = null;
                for (String vid : model.getVlanIds()) {
                    vlanID = Integer.valueOf(vid);
                    if (VlanUtil.getVlan(pool, Integer.valueOf(vlanID)) == null) {
                        throw new InventoryException("The specified VLAN ID could not be found.");
                    }
                    vlan = VlanUtil.getVlan(pool, vlanID);
                    NodeDto node = port.getNode();
                    VlanIfDto vlanif = VlanUtil.getVlanIf(node, vlanID);
                    List<PortDto> tagports = VlanUtil.getTaggedPorts(vlan);
                    for (PortDto prt : tagports) {
                        if (DtoUtil.isSameMvoEntity(port, prt)) {
                            throw new InventoryException("The specified port already has the specified VLAN ID.");
                        }
                    }
                    List<PortDto> untagports = VlanUtil.getUntaggedPorts(vlan);
                    for (PortDto prt : untagports) {
                        if (DtoUtil.isSameMvoEntity(port, prt)) {
                            throw new InventoryException("The specified port already has the specified VLAN ID.");
                        }
                    }
                    builder = new VlanIfCommandBuilder(node, (VlanIfDto) vlanif, user);
                    builder.setVlan(pool.getName(), vlanID.toString());
                    builder.setVlanId(vlanID);
                    List<String> portAbsoluteName = PortRenderer.getTaggedPortAbsoluteNames(vlanif);
                    List<String> portIfName = PortRenderer.getTaggedPortIfNames(vlanif);

                    if (portAbsoluteName != null && portIfName != null) {
                        portAbsoluteName.add(port.getAbsoluteName());
                        portIfName.add(port.getIfname());
                    } else if (portAbsoluteName == null && portIfName == null) {
                        portAbsoluteName = new ArrayList<String>();
                        portIfName = new ArrayList<String>();
                        portAbsoluteName.add(port.getAbsoluteName());
                        portIfName.add(port.getIfname());
                    }
                    builder.setTaggedPorts(portAbsoluteName, portIfName);
                    BuildResult result = builder.buildCommand();
                    if (BuildResult.NO_CHANGES == result) {
                        return;
                    } else if (BuildResult.FAIL == result) {
                        throw new IllegalStateException("Update Fail.");
                    }
                    commandBuilderList.add(builder);
                }
                ShellConnector.getInstance().executes(commandBuilderList);

            } else if (model instanceof LagPort) {
                LagPort model = (LagPort) this.model;

                VlanIdPoolDto pool = VlanUtil.getPool(model.getVlanPoolName());
                if (model.getVlanIds().size() == 0) {
                    throw new InventoryException("VLAN ID is not set.");
                } else if (model.getTagMode() == null || model.getTagMode().isEmpty()) {
                    throw new InventoryException("TagMode is not selected.");
                } else if (!PortEditConstraints.isPortSetVlanIdEnabled(port)) {
                    throw new InventoryException("It can not assign a VLAN ID to the specified port.");
                }

                List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();
                VlanIfCommandBuilder builder = null;
                for (String vid : model.getVlanIds()) {
                    vlanID = Integer.valueOf(vid);
                    if (VlanUtil.getVlan(pool, Integer.valueOf(vlanID)) == null) {
                        throw new InventoryException("The specified VLAN ID could not be found.");
                    }
                    vlan = VlanUtil.getVlan(pool, vlanID);
                    NodeDto node = port.getNode();
                    VlanIfDto vlanif = VlanUtil.getSwitchVlanIf(node, vlan.getVlanId());
                    builder = new VlanIfCommandBuilder(node, (VlanIfDto) vlanif, user);
                    builder.setVlan(pool.getName(), vlanID.toString());
                    builder.setVlanId(vlanID);
                    List<String> portAbsoluteName = PortRenderer.getTaggedPortAbsoluteNames(vlanif);
                    List<String> portIfName = PortRenderer.getTaggedPortIfNames(vlanif);

                    if (portAbsoluteName != null && portIfName != null) {
                        portAbsoluteName.add(port.getAbsoluteName());
                        portIfName.add(port.getIfname());
                    } else if (portAbsoluteName == null && portIfName == null) {
                        portAbsoluteName = new ArrayList<String>();
                        portIfName = new ArrayList<String>();
                        portAbsoluteName.add(port.getAbsoluteName());
                        portIfName.add(port.getIfname());
                    }
                    builder.setTaggedPorts(portAbsoluteName, portIfName);

                    BuildResult result = builder.buildCommand();
                    if (BuildResult.NO_CHANGES == result) {
                        return;
                    } else if (BuildResult.FAIL == result) {
                        throw new IllegalStateException("Update Fail.");
                    }
                    commandBuilderList.add(builder);
                }
                ShellConnector.getInstance().executes(commandBuilderList);
            }
        } catch (InventoryException e) {
            log.debug("InventoryException", e);
            if (e.getCause() == null) {
                throw new InventoryException(e);
            } else {
                throw new InventoryException(e.getCause().getMessage());
            }
        } catch (ExternalServiceException e) {
            log.debug("ExternalServiceException", e);
            if (e.getCause() == null) {
                throw new ExternalServiceException(e);
            } else {
                throw new ExternalServiceException(e.getCause().getMessage());
            }
        } catch (IOException e) {
            log.debug("IOException", e);
            if (e.getCause() == null) {
                throw new IOException(e);
            } else {
                throw new IOException(e.getCause().getMessage());
            }
        } catch (RuntimeException e) {
            log.debug("RuntimeException", e);
            if (e.getCause() == null) {
                throw new RuntimeException(e);
            } else {
                throw new RuntimeException(e.getCause().getMessage());
            }
        }
    }

}