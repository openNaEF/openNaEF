package voss.discovery.iolib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.AgentConfiguration;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.iolib.snmp.SnmpAccess;

import java.io.IOException;

public class DiscoveryTypeFactory {
    private static final Logger log = LoggerFactory.getLogger(DiscoveryTypeFactory.class);

    public SupportedDiscoveryType getType(SnmpAccess snmp) throws IOException, UnknownTargetException {
        if (snmp == null) {
            throw new IllegalArgumentException();
        }
        String sysObjectID = getSysObjectId(snmp);
        log.debug("getType():" + snmp.getSnmpAgentAddress().getAddress().getHostAddress()
                + ":" + sysObjectID);
        String typeName = AgentConfiguration.getInstance().getAgentType(sysObjectID);
        try {
            return SupportedDiscoveryType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String getSysObjectId(SnmpAccess access) throws IOException {
        try {
            return Mib2Impl.getSysObjectId(access);
        } catch (Exception e) {
            log.error("unexpected error", e);
            throw new IOException(e);
        }
    }

}