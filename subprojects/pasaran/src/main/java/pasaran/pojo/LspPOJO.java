package pasaran.pojo;

import java.util.List;

public class LspPOJO extends PasaranPOJO {
    public List<LspHopSeriesPOJO> hopSeries;    // lspの経路
    public String ingressNode;                  // lspの入口となるnodeのid
    public String egressNode;                   // lspの入口となるnodeのid

    public String activeHopSeries;              // Naef上でアクティブと設定した経路のhop-seriesのid
    public String actualHopSeries;              // 実機からディスカバリしてきた実機の経路のhop-seriesのid

    public List<String> upperLayer;
}
