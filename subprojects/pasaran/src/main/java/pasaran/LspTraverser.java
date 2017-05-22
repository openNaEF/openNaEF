package pasaran;

import naef.dto.DtoUtils;
import naef.dto.PathHopDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import naef.ui.NaefDtoFacade;
import net.arnx.jsonic.JSON;
import pasaran.api.LspPOJOUtil;
import pasaran.pojo.LspPOJO;

import java.util.Set;

public class LspTraverser {
    private static final JSON json;
    static {
        json = new JSON();
        json.setPrettyPrint(true);
        json.setSuppressNull(true);
    }


    public static void main(String[] args) throws Exception {
        NaefDtoFacade dtoFacade = NaefConnector.getInstance().getConnection().getDtoFacade();
        Set<RsvpLspDto> lsps = dtoFacade.getRsvpLsps();

        for (RsvpLspDto lsp : lsps) {
            LspPOJO pojo = LspPOJOUtil.createPOJO(lsp);
            //System.out.println(json.format(pojo));
        }
    }

    // LSP Traverser
    public static void main2(String[] args) throws Exception {
        NaefDtoFacade dtoFacade = NaefConnector.getInstance().getConnection().getDtoFacade();
        Set<RsvpLspDto> lsps = dtoFacade.getRsvpLsps();

        int i = 1;
        for (RsvpLspDto lsp : lsps) {
            System.out.printf("-- LSP[%3d]: %s // %s\n", i++, lsp.getOid().toString(), lsp.getId());
            //lsp.getActiveHopSeries()      // Naef上でアクティブと設定した経路
            //lsp.getActualHopSeries()      // 実機からディスカバリしてきた実機の経路

            //lsp.getIngressNode()          // LSPの入口となるノード
            //lsp.getEgressNode()           // LSPの出口となるノード

            //lsp.getHopSeries1~5()           // LSPの経路

            for (RsvpLspHopSeriesDto hopSeries : LspPOJOUtil.getHopSeries(lsp)) {
                System.out.printf("    * %s // %s\n", hopSeries.getOid().toString(), hopSeries.getName());

                PathHopDto prevHop = null;
                for (PathHopDto hop : hopSeries.getHops()) {
                    System.out.printf("        + %s // %s -> %s\n", hop.getOid().toString(), hop.getSrcPort().getAbsoluteName(), hop.getDstPort().getAbsoluteName());

                    // 実験 ----------------------------------------------------------------------------------------
                    // prevHopのdstPort と hopのsrcPort は同一Nodeである
                    // hopSeries.getHops() では入り口から出口に向かってソートされた状態でHopを取得できる
                    if (prevHop != null) {
                        if (DtoUtils.isSameEntity(prevHop.getDstPort().getNode(), hop.getSrcPort().getNode())) {
                            prevHop = hop;
                        } else {
                            throw new Exception(hopSeries.getOid().toString());
                        }
                    }
                    // --------------------------------------------------------------------------------------------
                }
            }
        }
    }

}
