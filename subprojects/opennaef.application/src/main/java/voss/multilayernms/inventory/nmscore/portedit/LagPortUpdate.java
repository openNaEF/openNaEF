package voss.multilayernms.inventory.nmscore.portedit;

import jp.iiga.nmt.core.model.portedit.LagPort;
import jp.iiga.nmt.core.model.portedit.PortEditModel;
import naef.dto.*;
import naef.dto.atm.AtmApsIfDto;
import naef.dto.eth.EthLagIfDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ATTR;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.constants.CustomerConstants;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.nms.inventory.builder.AtmAPSCommandBuilder;
import voss.nms.inventory.builder.EthernetLAGCommandBuilder;
import voss.nms.inventory.builder.LinkCommandBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.util.*;

public class LagPortUpdate implements IPortEditUpdate {
    private PortDto port;
    private LagPort model;
    private String user;

    public LagPortUpdate(PortDto port, PortEditModel model, String user) {
        this.port = port;
        this.model = (LagPort) model;
        this.user = user;
    }

    public LagPortUpdate(PortEditModel model, String user) {
        this.port = null;
        this.model = (LagPort) model;
        this.user = user;
    }

    public void create() throws RuntimeException, IOException, ExternalServiceException, InventoryException {
        NodeDto node = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getNodeMvoId(), NodeDto.class);
        String name = model.getIfName();
        for (PortDto port : node.getPorts()) {
            if (name.equals(port.getIfname())) {
                throw new IllegalArgumentException(name + " is already registered.");
            }
        }

        EthernetLAGCommandBuilder builder = new EthernetLAGCommandBuilder(model.getNodeName(), name, user);
        ethLagBuilder(builder, model);
        builder.setIfName(model.getIfName());
        BuildResult result = builder.buildCommand();
        if (BuildResult.NO_CHANGES == result) {
            log.debug("BuildResult.NO_CHANGES = " + BuildResult.NO_CHANGES.toString());
            return;
        } else if (BuildResult.FAIL == result) {
            throw new IllegalStateException("Update Fail.");
        }
        ShellConnector.getInstance().executes(builder);
    }

    public void memberUpdate() throws RuntimeException, IOException, ExternalServiceException, InventoryException {
        EthernetLAGCommandBuilder builder = new EthernetLAGCommandBuilder((EthLagIfDto) port, user);
        ethLagBuilder(builder, model);
        BuildResult result = builder.buildCommand();
        if (BuildResult.NO_CHANGES == result) {
            log.debug("BuildResult.NO_CHANGES = " + BuildResult.NO_CHANGES.toString());
            return;
        } else if (BuildResult.FAIL == result) {
            throw new IllegalStateException("Update Fail.");
        }
        ShellConnector.getInstance().executes(builder);
    }

    @Override
    public void update() throws RuntimeException, IOException, ExternalServiceException, InventoryException {
        List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();
        EthernetLAGCommandBuilder builder = null;

        try {

            if (port instanceof EthLagIfDto) {
                builder = new EthernetLAGCommandBuilder((EthLagIfDto) port, user);
            } else if (port instanceof AtmApsIfDto) {
            } else {
                throw new IllegalArgumentException(port.getIfname() + " is not EthLagIf");
            }

            builder.setIfName(model.getIfName());
            if (!model.getAdminStatus().equals("N/A")) {
                builder.setAdminStatus(model.getAdminStatus());
            }
            builder.setValue(MPLSNMS_ATTR.BANDWIDTH, model.getBandwidth());
            builder.setPortDescription(model.getDescription());
            builder.setValue(CustomerConstants.NOTICES, model.getNotices());
            builder.setValue(MPLSNMS_ATTR.PURPOSE, model.getPurpose());
            builder.setEndUserName(model.getUser());
            if (!(model.getIpAddress() == null || model.getIpAddress().isEmpty())) {
                if (model.getVpnPrefix() != null) {
                    builder.setNewIpAddress(model.getVpnPrefix(), model.getIpAddress(), model.getSubnetMask());
                } else {
                    builder.setNewIpAddress(null, model.getIpAddress(), model.getSubnetMask());
                }
            } else if (model.getIpAddress() == null || model.getIpAddress().isEmpty()) {
                builder.setNewIpAddress(null, null, null);
            }

            if (model.getPortMode() != null && model.getSwitchPortMode() != null) {
                builder.setPortMode(model.getPortMode());
                builder.setSwitchPortMode(model.getSwitchPortMode());
            } else if (model.getPortMode() == null && model.getSwitchPortMode() == null) {
            }

            String connectPortMvoId = model.getConnectPortMvoId();
            PortDto targetPort = null;
            if (connectPortMvoId != null && !connectPortMvoId.isEmpty()) {
                targetPort = MplsNmsInventoryConnector.getInstance().getMvoDto(connectPortMvoId, PortDto.class);
                if (targetPort == null) {
                    throw new IllegalArgumentException("Connection destination port does not exist");
                }
            }
            if (targetPort instanceof EthLagIfDto || targetPort == null) {
                commandBuilderList.addAll(createLagLinkBuilder(port, targetPort));
            }

            BuildResult result = builder.buildCommand();
            if (BuildResult.NO_CHANGES == result) {
                log.debug("BuildResult.NO_CHANGES = " + BuildResult.NO_CHANGES.toString());
            } else if (BuildResult.FAIL == result) {
                throw new IllegalStateException("Update Fail.");
            }
            commandBuilderList.add(builder);

            ShellConnector.getInstance().executes(commandBuilderList);

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

    private List<CommandBuilder> createLagLinkBuilder(PortDto currentPort, PortDto targetPort) throws IOException, ExternalServiceException, InventoryException {
        List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
        if (currentPort == null) return builders;

        LinkDto link = null;
        for (NetworkDto dto : currentPort.getNetworks()) {
            if (ATTR.TYPE_LAG_LINK.equals(dto.getObjectTypeName())) {
                link = (LinkDto) dto;
            }
        }
        if (isChangedPort(currentPort, targetPort, link)) {
            if (link != null) {
                LinkCommandBuilder builder = new LinkCommandBuilder(link, user);
                BuildResult result = builder.buildDeleteCommand();
                if (BuildResult.SUCCESS == result) {
                    builders.add(builder);
                } else if (BuildResult.FAIL == result) {
                    throw new IllegalStateException("Update Fail.");
                }
            }
            if (targetPort != null) {
                LinkCommandBuilder builder = new LinkCommandBuilder(currentPort, targetPort, user);
                BuildResult result = builder.buildCommand();
                if (BuildResult.SUCCESS == result) {
                    builders.add(builder);
                } else if (BuildResult.FAIL == result) {
                    throw new IllegalStateException("Update Fail.");
                }
            }
        }

        return builders;
    }

    public void ethLagBuilder(EthernetLAGCommandBuilder builder, LagPort model) {
        Map<String, String> memberPorts = new HashMap<String, String>();
        Map<String, Map<String, String>> ports = model.getMemberPorts();
        for (String key : ports.keySet()) {
            memberPorts.putAll(ports.get(key));
        }
        builder.setMemberPorts(memberPorts);
    }

    public void atmLagBuilder(AtmAPSCommandBuilder builder, LagPort model) {
        Map<String, String> memberPorts = new HashMap<String, String>();
        Map<String, Map<String, String>> ports = model.getMemberPorts();
        for (String key : ports.keySet()) {
            memberPorts.putAll(ports.get(key));
        }
        builder.setMemberPorts(memberPorts);
    }

    private boolean isChangedPort(PortDto currentPort, PortDto targetPort, LinkDto link) {
        if (link == null) return true;
        if (currentPort == null || targetPort == null) return true;

        Iterator<PortDto> iterator = link.getMemberPorts().iterator();
        PortDto port1 = iterator.next();
        PortDto port2 = iterator.next();

        if ((DtoUtils.isSameEntity(currentPort, port1) || DtoUtils.isSameEntity(currentPort, port2))
                && (DtoUtils.isSameEntity(targetPort, port1) || DtoUtils.isSameEntity(targetPort, port2))) {
            return false;
        }

        return true;
    }
}