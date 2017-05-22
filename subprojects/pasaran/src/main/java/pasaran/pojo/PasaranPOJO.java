package pasaran.pojo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author yamazaki
 */
public class PasaranPOJO implements Serializable {
    public String mvoId;
    public long readTxTime;         // read-transaction の時間
    public String readTxVersion;    // read-transaction のバージョン

    public String name;
    public String objectType;

    // TODO 以下の属性は共通のものではないので、別のクラスに出す

    public String area;
    public String country;
    public String city;
    public String vendorName;
    public String kisyuName;
    public String purpose;

    public String parent;           // 親のMvoId文字列
    public List<String> children;   // 子のMvoId文字列のリスト

    public String hostNode;
    public List<String> guestNodes;
    public String vSwitchNode;

    public Map<String, String> attributes;

    // FIXME timelineのためのワークアラウンド
    public List<PasaranVersionPOJO> versions;
    public PasaranVersionPOJO initialVersion;
    public PasaranVersionPOJO endVersion;

    // vlan-if
    public Integer vlanId;
    public List<String> taggedPorts;
    public List<String> untaggedPorts;
    public List<String> aliases;
    public String owner;

    // eth-lag-if
    public List<String> memberPorts;

    // ip-if
    @Deprecated
    public List<String> associatedPorts;
    public Map<String, Integer> boundPorts;
}
