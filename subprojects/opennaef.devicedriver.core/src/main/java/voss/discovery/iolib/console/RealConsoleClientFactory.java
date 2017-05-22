package voss.discovery.iolib.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.AgentConfiguration;
import voss.discovery.agent.common.OSType;
import voss.discovery.agent.mib.CiscoImageMib;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.SupportedDiscoveryType;
import voss.discovery.iolib.simpletelnet.*;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.model.NodeInfo;
import voss.model.Protocol;

import java.io.IOException;
import java.net.InetAddress;

public class RealConsoleClientFactory implements ConsoleClientFactory {
    public static final Logger log = LoggerFactory.getLogger(RealConsoleClientFactory.class);

    @Override
    public ConsoleClient getConsoleClient(NodeInfo nodeinfo, SnmpAccess snmp,
                                          InetAddress inetAddress, SupportedDiscoveryType type)
            throws IOException, AbortedException {
        AgentConfiguration config = AgentConfiguration.getInstance();
        RealTerminalSocket telnet;
        if (nodeinfo.isSupported(Protocol.SSH2)
                || nodeinfo.isSupported(Protocol.SSH2_PUBLICKEY)) {
            telnet = new RealSsh2Socket(nodeinfo,
                    inetAddress,
                    nodeinfo.getProtocolPort(Protocol.SSH2).getPort(),
                    config.getTelnetTimeoutSeconds() * 1000);
            log.info("using SSH2 as CLI: " + inetAddress.getHostAddress());
        } else if (nodeinfo.isSupported(Protocol.SSH2_INTERACTIVE)) {
            telnet = new RealSsh2Socket(nodeinfo,
                    inetAddress,
                    nodeinfo.getProtocolPort(Protocol.SSH2_INTERACTIVE).getPort(),
                    config.getTelnetTimeoutSeconds() * 1000);
            log.info("using SSH2(KeyboardInteractive) as CLI: " + inetAddress.getHostAddress());
        } else if (nodeinfo.isSupported(Protocol.SSH2_PUBLICKEY)) {
            telnet = new RealSsh2Socket(nodeinfo,
                    inetAddress,
                    nodeinfo.getProtocolPort(Protocol.SSH2_PUBLICKEY).getPort(),
                    config.getTelnetTimeoutSeconds() * 1000);
            log.info("using SSH2(PublicKey) as CLI: " + inetAddress.getHostAddress());
        } else if (nodeinfo.isSupported(Protocol.TELNET)) {
            telnet = new RealTelnetSocket(inetAddress,
                    nodeinfo.getProtocolPort(Protocol.TELNET).getPort(),
                    config.getTelnetTimeoutSeconds() * 1000);
            log.info("using TELNET as CLI: " + inetAddress.getHostAddress());
        } else {
            return null;
        }
        telnet.setCommandInterval(config.getTelnetCommandIntervalMilliSeconds());
        telnet.setMoreInterval(config.getMoreIntervalMilliSeconds());

        switch (type) {
            case Cisco12000GsrRouter:
            case CiscoCatalyst2900XLDiscovery:
            case CiscoCatalyst3750Discovery:
            case CiscoCatalyst4500Discovery:
            case CiscoCatalystDiscovery:
            case CiscoIosRouter:
            case CiscoIosSwitch:
                OSType osType = CiscoImageMib.getOSType(snmp);
                switch (osType) {
                    case IOS:
                        return new IosTelnetClient(telnet, nodeinfo);
                    case IOS_XR:
                        throw new IOException("Not Supported: " + osType);
                    case CATOS:
                        return new CatOsTelnetClient(telnet, nodeinfo);
                    default:
                        throw new IOException("Not Supported: " + osType);
                }
            case CiscoNexusDiscovery:
                return new CiscoNexusTelnetClient(telnet, nodeinfo);
            case JuniperJunosRouter:
                return new JuniperJunosTelnetClient(telnet, nodeinfo);
            case NetscreenSSGBoxRouter:
            case NetscreenSSGChassisRouter:
                return new JuniperNetscreenTelnetClient(telnet, nodeinfo);
            case Alaxala1230SDiscovery:
            case Alaxala2430SDiscovery:
            case Alaxala3630SDiscovery:
            case Alaxala5400SDiscovery:
            case Alaxala6300SDiscovery:
            case Alaxala6700SDiscovery:
            case Alaxala7800SDiscovery:
            case BladeNetworkSwitchDiscovery:
                return new BladeNetworkTelnetClient(telnet, nodeinfo);
            case FortigateDiscovery:
                return new FortigateTelnetClient(telnet, nodeinfo);
            case FlashWave5740Discovery:
            case FlashWave5540Discovery:
            case FlashWave5530Discovery:
                return new FlashWaveTelnetClient(telnet, nodeinfo);
            case ApresiaApwareDiscovery:
            case ApresiaAmiosDiscovery:
            case ApresiaAeosDiscovery:
                return new ApresiaTelnetClient(telnet, nodeinfo);
            case ExtremeGenericDiscovery:
            case ExtremeBD10800Discovery:
            case F5BigIpDiscovery:
                telnet.setTerminalAsVt100();
                return new F5BigIpSshClient(telnet, nodeinfo);
            case FoundryGenericDiscovery:
            case FoundryMG8Discovery:
                return null;
            case NecSuperHub:
                return new SuperHubTelnetClient(telnet, nodeinfo);
            case SiiExatrax:
                return new SiiExatraxTelnetClient(telnet, nodeinfo);
            case NecIx3000Discovery:
                return new NecIxTelnetClient(telnet, nodeinfo);
            case Alcatel7710SRDiscovery:
                return new AlcatelTelnetClient(telnet, nodeinfo);
            case TestSshDiscovery:
                return new TestSshClient(telnet, nodeinfo);
            case AdvaWDMDiscovery:
            case Mib2Generic:
            case SiiExatrax_SNMP:
                return null;
        }
        throw new IllegalArgumentException("unknown type: " + type.toString());
    }
}