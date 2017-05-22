package naef.shell;

import naef.mvo.AbstractNetwork;
import naef.mvo.Network;
import naef.mvo.P2pLink;
import naef.mvo.PathHop;
import naef.mvo.Port;
import tef.MVO;
import tef.skelton.ConfigurationException;
import tef.skelton.SkeltonTefService;
import tef.skelton.UiTypeName;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigurePathHopCommand extends NaefShellCommand {

    private enum Operation {

        ADD_HOP {

            @Override void process(
                ConfigurePathHopCommand cmd,
                Network.PathHopSeries<?, ?, ?> pathHopSeries,
                Commandline args)
                throws ShellCommandException
            {
                if (args.argsSize() < 3) {
                    throw new ShellCommandException("extra-args: [link type] [src port]");
                }

                UiTypeName pathHopSeriesType
                    = SkeltonTefService.instance().uiTypeNames().getByType(pathHopSeries.getClass());
                if (pathHopSeriesType == null) {
                    throw new ShellCommandException("型定義が見つかりません: " + pathHopSeries.getClass().getName());
                }

                Class<? extends Network> linkType = cmd.resolveNetworkType(args.arg(1));
                String srcPortName = args.arg(2);
                List<String> linkPortNames = args.args().subList(2, args.args().size());

                Network link = cmd.resolveNetwork(linkType, linkPortNames);
                if (! (link instanceof Network.LowerStackable)) {
                    throw new ShellCommandException("指定されたリンクは lower-stackable ではありません.");
                }

                Port srcPort = cmd.resolve(Port.class, srcPortName);
                Port dstPort;
                if (P2pLink.class.isAssignableFrom(link.getClass())) {
                    dstPort = ((P2pLink) link).getPeer(srcPort);
                    if (dstPort == null) {
                        throw new ShellCommandException("対向ポートが見つかりません.");
                    }
                } else {
                    Integer maxConfigurablePorts = AbstractNetwork.Attr.MAX_CONFIGURABLE_PORTS.get(link);
                    if (maxConfigurablePorts == null || maxConfigurablePorts.intValue() != 2) {
                        throw new ShellCommandException("リンクに p2p 設定がされていません.");
                    }

                    Set<? extends Port> ports = new HashSet<Port>(link.getCurrentMemberPorts());
                    if (ports.size() != 2) {
                        throw new ShellCommandException(
                            "リンクを構成するポート数の期待値は2であるのに対して実際は" + ports.size() + "です.");
                    }
                    ports.remove(srcPort);
                    if (ports.size() != 1) {
                        throw new IllegalStateException(
                            "ポート数異常: " + ports.size() + ", " + ((MVO) link).getMvoId());
                    }

                    dstPort = ports.iterator().next();
                }

                PathHop lastHop = pathHopSeries.getLastHop();
                if (lastHop != null) {
                    if (lastHop.getDstPort().getNode() != srcPort.getNode()) {
                        throw new ShellCommandException("連続したリンクを指定してください.");
                    }
                }

                PathHop hop;
                if (pathHopSeriesType.type() == naef.mvo.mpls.RsvpLspHopSeries.class) {
                    assertType(
                        "ホップ列は rsvp-lsp-hop-series",
                        naef.mvo.mpls.RsvpLspHopSeries.class,
                        pathHopSeries);
                    hop = new naef.mvo.mpls.RsvpLspHop(
                        (naef.mvo.mpls.RsvpLspHopSeries) pathHopSeries, srcPort, dstPort);
                } else if (pathHopSeriesType.type() == naef.mvo.wdm.OpticalPath.class) {
                    assertType("ホップ列は optical-path", naef.mvo.wdm.OpticalPath.class,pathHopSeries);
                    assertType("ポートは wdm-port", naef.mvo.wdm.WdmPort.class, srcPort);
                    assertType("ポートは wdm-port", naef.mvo.wdm.WdmPort.class, dstPort);
                    hop = new naef.mvo.wdm.OpticalPathHop(
                        (naef.mvo.wdm.OpticalPath) pathHopSeries,
                        (naef.mvo.wdm.WdmPort) srcPort,
                        (naef.mvo.wdm.WdmPort) dstPort);
                } else {
                    throw new ShellCommandException("サポートされていない型です: " + pathHopSeriesType.name());
                }

                hop.stackOver((Network.LowerStackable) link);
            }
        },

        RESET_HOPS {

            @Override void process(
                ConfigurePathHopCommand cmd,
                Network.PathHopSeries<?, ?, ?> pathHopSeries,
                Commandline args)
                throws ShellCommandException
            {
                if (args.argsSize() != 1) {
                    throw new ShellCommandException("no extra-args expected.");
                }

                for (PathHop hop = pathHopSeries.getLastHop(); hop != null; hop = hop.getPrevious()) {
                    hop.dispose();
                }
            }
        };

        abstract void process(
            ConfigurePathHopCommand cmd,
            Network.PathHopSeries<?, ?, ?> pathHopSeries,
            Commandline args)
            throws ShellCommandException;

        private static void assertType(String caption, Class<?> expectedClass, Object obj)
            throws ShellCommandException
        {
            if (! expectedClass.isInstance(obj)) {
                throw new ShellCommandException(caption + " を指定してください.");
            }
        }
    }

    @Override public String getArgumentDescription() {
        return "[operation: add-hop, reset-hops] extra-args...";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        Network.PathHopSeries<?, ?, ?> pathHopSeries = contextAs(Network.PathHopSeries.class, "ホップ列");

        Operation op = resolveEnum("operation", Operation.class, args.arg(0));

        beginWriteTransaction();

        try {
            op.process(this, pathHopSeries, args);
        } catch (ConfigurationException ce) {
            throw new ShellCommandException(ce.getMessage());
        }

        commitTransaction();
    }
}
