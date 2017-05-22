package naef.mvo.mpls;

import naef.mvo.Network;
import naef.mvo.Port;

import java.util.ArrayList;
import java.util.List;

public class MplsMvoUtils {

    private MplsMvoUtils() {
    }

    public static List<RsvpLsp> getRsvpLsps() {
        List<RsvpLsp> result = new ArrayList<RsvpLsp>();
        for (RsvpLspIdPool pool : RsvpLspIdPool.home.list()) {
            for (RsvpLsp lsp : pool.getUsers()) {
                RsvpLspHopSeries hopSeries = lsp.get(RsvpLsp.Attr.HOP_SERIES_1);
                if (hopSeries == null) {
                    continue;
                }

                RsvpLspHop lastHop = hopSeries.getLastHop();
                if (lastHop == null) {
                    continue;
                }

                boolean srcConnectionStatus = getConnectionStatus(lastHop.getSrcPort(), lastHop);
                boolean dstConnectionStatus = getConnectionStatus(lastHop.getDstPort(), lastHop);
                if (srcConnectionStatus != dstConnectionStatus) {
                    throw new RuntimeException(
                        new java.util.Date(tef.TransactionContext.getTargetTime())
                            + " " + lastHop.getMvoId().getLocalStringExpression());
                }
                if (srcConnectionStatus) {
                    result.add(lsp);
                }
            }
        }
        return result;
    }

    public static List<Pseudowire> getPseudowires() {
        List<Pseudowire> result = new ArrayList<Pseudowire>();
        for (Pseudowire pw : Pseudowire.home.list()) {
            Boolean ac1ConnectionStatus = getConnectionStatus(pw.getAttachmentCircuit1(), pw);
            Boolean ac2ConnectionStatus = getConnectionStatus(pw.getAttachmentCircuit2(), pw);
            if (ac1ConnectionStatus != null && ac2ConnectionStatus != null
                && ac1ConnectionStatus.booleanValue() != ac2ConnectionStatus.booleanValue())
            {
                throw new RuntimeException(
                    new java.util.Date(tef.TransactionContext.getTargetTime())
                        + " " + pw.getMvoId().getLocalStringExpression());
            }
            if ((ac1ConnectionStatus != null && ac1ConnectionStatus.booleanValue())
                || (ac2ConnectionStatus != null && ac2ConnectionStatus.booleanValue()))
            {
                result.add(pw);
            }
        }
        return result;
    }

    public static Long getVcId(Pseudowire pw) {
        PseudowireIdPool idPool = pw.get(Pseudowire.Attr.ID_POOL);
        return idPool == null ? null : idPool.getId(pw);
    }

    public static List<RsvpLsp> getRsvpLsps(Pseudowire pw) {
        List<RsvpLsp> result = new ArrayList<RsvpLsp>();
        for (Network.LowerStackable lowerLayer : pw.getCurrentLowerLayers(false)) {
            if (lowerLayer instanceof RsvpLsp) {
                result.add((RsvpLsp) lowerLayer);
            }
        }
        return result;
    }

    private static Boolean getConnectionStatus(Port port, Network network) {
        return port == null
            ? null
            : port.getCurrentNetworks(network.getClass()).contains(network);
    }
}
