package voss.nms.inventory.diff.network.builder;

import naef.dto.ip.IpIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.ExceptionUtils;
import voss.model.MplsVlanDevice;
import voss.model.Port;
import voss.model.VrfInstance;
import voss.nms.inventory.builder.PortCommandBuilder;
import voss.nms.inventory.config.DiffConfiguration;
import voss.nms.inventory.constants.IpAddressModel;
import voss.nms.inventory.diff.network.DiffPolicy;
import voss.nms.inventory.diff.network.IpAddressDB;
import voss.nms.inventory.diff.network.IpAddressHolder;

public abstract class AbstractPortBuilderFactory implements BuilderFactory {
    private IpAddressDB ipDB;

    public void setIpDB(IpAddressDB ipDB) {
        this.ipDB = ipDB;
    }

    private boolean isVrfRelatedPort(Port port) {
        if (port == null) {
            return false;
        } else if (!(port.getDevice() instanceof MplsVlanDevice)) {
            return false;
        }
        MplsVlanDevice device = (MplsVlanDevice) port.getDevice();
        VrfInstance vrf = device.getPortRelatedVrf(port);
        return vrf != null;
    }

    protected void setIpAddress(PortCommandBuilder builder, Port port, String ip, String mask) {
        if (builder == null) {
            return;
        }
        builder.setAssociateIpSubnetAddress(false);
        Logger log = LoggerFactory.getLogger(AbstractPortBuilderFactory.class);
        DiffConfiguration config = DiffConfiguration.getInstance();
        DiffPolicy policy;
        try {
            policy = config.getDiffPolicy();
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
        if (isVrfRelatedPort(port)) {
            log.debug("this object is for vrf, so ip-address is processed in vrf-diff.");
            return;
        }
        if (ip == null) {
            builder.setNewIpAddress(null, null, null, false);
            return;
        }
        String vpnPrefix = config.getVpnDriver().getVpnPrefix(port);
        IpAddressHolder ipHolder = this.ipDB.getIpAddress(vpnPrefix, ip);
        if (ipHolder == null) {
            return;
        }
        if (IpAddressModel.P2P == policy.getIpAddressModel()) {
            if (ipHolder.isDuplicated()) {
                return;
            }
        }
        IpIfDto dbIpIf = ipHolder.getDbPort();
        if (dbIpIf != null) {
            builder.setIpIf(dbIpIf);
        } else {
            builder.setNewIpAddress(vpnPrefix, ip, mask, true);
        }
    }
}