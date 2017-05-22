package pasaran.api;

import naef.dto.PathHopDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import naef.ui.NaefDtoFacade;
import pasaran.NaefConnector;
import pasaran.pojo.LspHopSeriesPOJO;
import pasaran.pojo.LspPOJO;
import pasaran.pojo.PasaranPOJO;
import pasaran.pojo.PathHopPOJO;
import pasaran.util.MvoUtil;
import tef.skelton.dto.EntityDto;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LspPOJOUtil {

    /**
     * 時間とバージョンを指定してLspを取得
     * @param time 時間
     * @param version バージョン
     * @return Lspのリスト
     */
    public static List<PasaranPOJO> getLsps(String time, String version) throws RemoteException {
        long start = System.currentTimeMillis();
        NaefDtoFacade dtoFacade = NaefConnector.getInstance().getConnection().getDtoFacade();

        List<PasaranPOJO> pojos = new ArrayList<>();
        for (RsvpLspDto lsp : dtoFacade.getRsvpLsps()) {
            LspPOJO pojo = LspPOJOUtil.createPOJO(lsp);
            pojos.add(pojo);
        }

        long end = System.currentTimeMillis();
        System.out.printf("lsp end. %dms\n", (end - start));

        return pojos;
    }

    public static LspPOJO createPOJO(RsvpLspDto lsp) {
        if (lsp == null) throw new IllegalArgumentException("lsp is null.");

        System.out.printf("-- LSP: %s // %s\n", lsp.getOid().toString(), lsp.getId());
        LspPOJO lspPojo = new LspPOJO();
        setCommonPOJOAttr(lspPojo, lsp);
        lspPojo.name = lsp.getName();
        lspPojo.ingressNode = MvoUtil.getRefId(lsp, RsvpLspDto.ExtAttr.INGRESS_NODE);
        lspPojo.egressNode = MvoUtil.getRefId(lsp, RsvpLspDto.ExtAttr.EGRESS_NODE);
        lspPojo.hopSeries = new ArrayList<>();
        lspPojo.activeHopSeries = MvoUtil.getRefId(lsp, RsvpLspDto.ExtAttr.ACTIVE_HOP_SERIES);
        lspPojo.actualHopSeries = MvoUtil.getRefId(lsp, RsvpLspDto.ExtAttr.ACTUAL_HOP_SERIES);

        for (RsvpLspHopSeriesDto hopSeries : getHopSeries(lsp)) {
            System.out.printf("    * %s // %s\n", hopSeries.getOid().toString(), hopSeries.getName());
            LspHopSeriesPOJO hopSeriesPojo = new LspHopSeriesPOJO();
            setCommonPOJOAttr(hopSeriesPojo, hopSeries);
            hopSeriesPojo.hops = new ArrayList<>();
            lspPojo.hopSeries.add(hopSeriesPojo);

            for (PathHopDto hop : hopSeries.getHops()) {
                System.out.printf("        + %s // %s -> %s\n", hop.getOid().toString(), hop.getSrcPort().getAbsoluteName(), hop.getDstPort().getAbsoluteName());
                PathHopPOJO pathHopPojo = new PathHopPOJO();
                setCommonPOJOAttr(pathHopPojo, hop);

                pathHopPojo.srcPort = MvoUtil.getRefId(hop, PathHopDto.Attr.SRC_PORT);
                pathHopPojo.dstPort = MvoUtil.getRefId(hop, PathHopDto.Attr.DST_PORT);

                hopSeriesPojo.hops.add(pathHopPojo);
            }
        }

        lspPojo.upperLayer = lsp.getPseudowires().stream()
                .map(pw -> pw.getOid().toString())
                .collect(Collectors.toList());

        return lspPojo;
    }

    // hop-series-1~5を配列にする
    public static Set<RsvpLspHopSeriesDto> getHopSeries(RsvpLspDto lsp) {
        Set<RsvpLspHopSeriesDto> hopSeries = new HashSet<>();
        hopSeries.add(lsp.getHopSeries1());
        hopSeries.add(lsp.getHopSeries2());
        hopSeries.add(lsp.getHopSeries3());
        hopSeries.add(lsp.getHopSeries4());
        hopSeries.add(lsp.getHopSeries5());
        return hopSeries.stream().filter(s -> s != null).collect(Collectors.toSet());
    }

    private static void setCommonPOJOAttr(PasaranPOJO pojo, EntityDto dto) {
        pojo.mvoId = MvoUtil.toMvoId(dto);
        // FIXME 時間をPOJOへ入れる
//        pojo.readTxTime = TransactionContext.getTargetTime();
//        pojo.readTxVersion = TransactionContext.getTargetVersion().getIdString();
    }


}
