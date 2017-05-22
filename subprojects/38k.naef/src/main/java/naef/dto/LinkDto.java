package naef.dto;

import java.util.ArrayList;
import java.util.List;

public class LinkDto extends NetworkDto {

    public LinkDto() {
    }

    /**
     * 引数で指定した port の対向を返します。
     * <p>
     * 引数の port はこの link の両端のどちらかでなければなりません。
     **/
    public PortDto getPeer(PortDto port) {
        List<PortDto> ports = new ArrayList<PortDto>(getMemberPorts());
        if (ports.size() > 2) {
            throw new IllegalStateException("ポート数異常: " + getOid() + ", " + ports.size());
        }

        if (ports.size() != 2) {
            return null;
        }

        PortDto port0 = ports.get(0);
        PortDto port1 = ports.get(1);
        if (port0.getOid().equals(port.getOid())) {
            return port1;
        } else if (port1.getOid().equals(port.getOid())) {
            return port0;
        }

        throw new IllegalArgumentException(port.getOid() + " は " + getOid() + " のポートではありません.");
    }
}
