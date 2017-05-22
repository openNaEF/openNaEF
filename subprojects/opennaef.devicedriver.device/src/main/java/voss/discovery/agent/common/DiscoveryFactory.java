package voss.discovery.agent.common;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.NullDiscovery;
import voss.discovery.agent.alaxala.*;
import voss.discovery.agent.alaxala.profile.*;
import voss.discovery.agent.alcatel.Alcatel7710SRDiscovery;
import voss.discovery.agent.apresia.amios.ApresiaAmiosDiscovery;
import voss.discovery.agent.apresia.apware.ApresiaApwareDiscovery;
import voss.discovery.agent.atmbridge.exatrax.EXAtraxDiscovery;
import voss.discovery.agent.atmbridge.exatrax_snmp.ExatraxSnmpDeviceDiscovery;
import voss.discovery.agent.bladenetwork.BladeNetworkSwitchDiscovery;
import voss.discovery.agent.cisco.*;
import voss.discovery.agent.cisconexus.CiscoNexusDiscovery;
import voss.discovery.agent.extreme.ExtremeBD10800SnmpMibFix;
import voss.discovery.agent.extreme.ExtremeGenericDiscovery;
import voss.discovery.agent.f5.F5BigIpDiscovery;
import voss.discovery.agent.flashwave.fw5540.FlashWave5540Discovery;
import voss.discovery.agent.flashwave.fw5740.FlashWave5740Discovery;
import voss.discovery.agent.fortigate.FortigateDiscovery;
import voss.discovery.agent.foundry.FoundryDiscoveryMG8Fix;
import voss.discovery.agent.foundry.FoundryGenericDiscovery;
import voss.discovery.agent.juniper.JuniperJunosDiscovery;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.agent.mib2generic.Mib2GenericDeviceDiscovery;
import voss.discovery.agent.nec.NecIx3000Discovery;
import voss.discovery.agent.netscreen.NetscreenBoxDiscovery;
import voss.discovery.constant.DiscoveryParameterType;
import voss.discovery.iolib.*;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.model.Device;

import java.io.IOException;
import java.util.Map;

public class DiscoveryFactory {
    private final static Logger log = LoggerFactory.getLogger(DiscoveryFactory.class);
    private final AgentConfiguration agentConfig;
    private static DiscoveryFactory factory;

    private DiscoveryFactory() throws IOException {
        this.agentConfig = AgentConfiguration.getInstance();
        initialize();
    }

    public synchronized void initialize() throws IOException {
    }

    public synchronized static DiscoveryFactory getInstance() throws IOException {
        if (factory == null) {
            factory = new DiscoveryFactory();
        }
        return factory;
    }

    public String getType(SnmpAccess snmp) throws IOException, UnknownTargetException {
        if (snmp == null) {
            throw new IllegalArgumentException();
        }
        String sysObjectID = getSysObjectId(snmp);
        log.debug("getType():" + snmp.getSnmpAgentAddress().getAddress().getHostAddress()
                + ":" + sysObjectID);
        return agentConfig.getAgentType(sysObjectID);
    }

    private String getSysObjectId(SnmpAccess access) throws IOException {
        try {
            return Mib2Impl.getSysObjectId(access);
        } catch (Exception e) {
            log.error("unexpected error", e);
            throw new IOException(e);
        }
    }

    public synchronized DeviceDiscovery getDiscovery(DeviceAccess access) throws IOException, AbortedException {
        assert access != null;
        if (access.getExtInfo(DeviceAccessExtInfoKey.DEVICE_DISCOVERY_RESULT) != null) {
            Device d = (Device) access.getExtInfo(DeviceAccessExtInfoKey.DEVICE_DISCOVERY_RESULT);
            SimulatedDeviceDiscovery discovery = new SimulatedDeviceDiscovery(d);
            return discovery;
        }
        try {
            String typeName = getType(access.getSnmpAccess());
            if (typeName == null) {
                log.debug("cannot determine discovery-type.");
                return null;
            }
            SupportedDiscoveryType type = null;
            try {
                type = SupportedDiscoveryType.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                return getExtraDiscovery(typeName, access);
            }
            log.debug("detected: [" + access.getTargetAddress().getHostAddress() + "]->" + type);

            switch (type) {
                case CiscoIosRouter:
                    return new CiscoIosDiscovery(access);
                case Cisco12000GsrRouter:
                    return new Cisco12000GsrIosDiscovery(access);
                case CiscoCatalystDiscovery:
                    return new CiscoCatalystDiscovery(access);
                case CiscoCatalyst2900XLDiscovery:
                    return new CiscoCatalyst2900XLDiscovery(access);
                case CiscoCatalyst3750Discovery:
                    return new CiscoCatalyst3750Discovery(access);
                case CiscoCatalyst4500Discovery:
                    return new CiscoCatalyst4500Discovery(access);
                case CiscoNexusDiscovery:
                    return new CiscoNexusDiscovery(access);
                case ExtremeGenericDiscovery:
                    return new ExtremeGenericDiscovery(access);
                case ExtremeBD10800Discovery:
                    return new ExtremeGenericDiscovery(access, new ExtremeBD10800SnmpMibFix(access.getSnmpAccess()));
                case F5BigIpDiscovery:
                    return new F5BigIpDiscovery(access);
                case FoundryGenericDiscovery:
                    return new FoundryGenericDiscovery(access);
                case FoundryMG8Discovery:
                    return new FoundryDiscoveryMG8Fix(access);
                case JuniperJunosRouter:
                    return new JuniperJunosDiscovery(access);
                case NetscreenSSGBoxRouter:
                    return new NetscreenBoxDiscovery(access);
                case NetscreenSSGChassisRouter:
                    return null;
                case Alaxala2430SDiscovery:
                    return new Alaxala2430SDiscovery(access, new Alaxala2400SVendorProfile());
                case Alaxala3630SDiscovery:
                    return new Alaxala3630SDiscovery(access, new Alaxala3630SVendorProfile());
                case Alaxala1230SDiscovery:
                    return null;
                case Alaxala5400SDiscovery:
                    return new Alaxala5400SDiscovery(access, new Alaxala5400SAlaxalaTypeProfile());
                case Alaxala6300SDiscovery:
                    return new Alaxala6300SDiscovery(access, new Alaxala6300SAlaxalaTypeProfile());
                case Alaxala6700SDiscovery:
                    return new Alaxala6700SDiscovery(access, new Alaxala6700SAlaxalaTypeProfile());
                case Alaxala7800SDiscovery:
                    return new Alaxala7800SDiscovery(access, new Alaxala7800SAlaxalaTypeProfile());
                case ApresiaApwareDiscovery:
                    return new ApresiaApwareDiscovery(access);
                case ApresiaAmiosDiscovery:
                    return new ApresiaAmiosDiscovery(access);
                case ApresiaAeosDiscovery:
                    throw new IllegalArgumentException("not yet");
                case Alcatel7710SRDiscovery:
                    return new Alcatel7710SRDiscovery(access);
                case BladeNetworkSwitchDiscovery:
                    return new BladeNetworkSwitchDiscovery(access);
                case FortigateDiscovery:
                    return new FortigateDiscovery(access);
                case FlashWave5540Discovery:
                    return new FlashWave5540Discovery(access);
                case FlashWave5740Discovery:
                    return new FlashWave5740Discovery(access);
                case NecIx3000Discovery:
                    return new NecIx3000Discovery(access);
                case SiiExatrax:
                    return new EXAtraxDiscovery(access);
                case SiiExatrax_SNMP:
                    return new ExatraxSnmpDeviceDiscovery(access);
                case TestSshDiscovery:
                case Mib2Generic:
                    return new Mib2GenericDeviceDiscovery(access);
                case NOT_SUPPORTED:
                    return new NullDiscovery();
            }
            throw new IllegalStateException("unknown target: " + type.toString());
        } catch (UnknownTargetException e) {
            log.error("unknown target: " + e.getSysObjectID());
            throw new IOException(e);
        } catch (DeviceNotCollectableStateException e) {
            log.error("cpu load limit exceeds. aborting discovery...");
            throw new IOException(e);
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    private DeviceDiscovery getExtraDiscovery(String type, DeviceAccess access) throws IOException {
        Map<String, String> map = AgentConfiguration.getInstance().getDiscoveryParameter(DiscoveryParameterType.EXTRA_DISCOVERY_FACTORY);
        if (map == null) {
            return new NullDiscovery();
        }
        String factoryName = map.get(type);
        if (factoryName == null) {
            throw new IllegalStateException("no factory definition found: type=" + type);
        }
        try {
            log.debug("testing factory: " + type + "->" + factoryName);
            Class<?> cls = Class.forName(factoryName);
            if (cls == null) {
                throw new IllegalStateException("no factory class found: " + factoryName);
            }
            @SuppressWarnings("unchecked")
            Class<ExtraDiscoveryFactory> factoryClass = (Class<ExtraDiscoveryFactory>) cls;
            ExtraDiscoveryFactory factory = factoryClass.newInstance();
            return factory.getDiscovery(type, access);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("No factory class found [" + factoryName + "]");
        } catch (ClassCastException e) {
            throw new IllegalStateException("unexpected factory class for ["
                    + factoryName + "]: " + factory.getClass().getName());
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

}