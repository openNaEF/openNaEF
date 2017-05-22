package voss.nms.inventory.model;

import naef.dto.NodeDto;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.NodeUtil;

import java.io.Serializable;

public class NodeModel implements Serializable {
    private static final long serialVersionUID = 1L;
    private final NodeDto node;

    public NodeModel(NodeDto node) {
        this.node = node;
    }

    public synchronized void renew() {
        this.node.renew();
    }

    public String isEndOfUse() {
        if (NodeUtil.isEndOfUse(this.node)) {
            return "end-of-use";
        }
        return null;
    }

    public String getNodeAlias() {
        return DtoUtil.getStringOrNull(node, "ノード別名");
    }

    public String getIpAddress() {
        return DtoUtil.getStringOrNull(node, "マネジメントアドレス");
    }

    public String getStatus() {
        return DtoUtil.getStringOrNull(node, "運用状態");
    }

    public String getPurpose() {
        return DtoUtil.getStringOrNull(node, "用途");
    }

    public String getDeviceType() {
        return DtoUtil.getStringOrNull(node, "機種");
    }

    public String getOsType() {
        return DtoUtil.getStringOrNull(node, "OS種別");
    }

    public String getOsVersion() {
        return DtoUtil.getStringOrNull(node, "OSバージョン");
    }

    public String getChassisSerial() {
        String chassisSerial = null;
        if (node.getChassises() != null && node.getChassises().size() > 0) {
            chassisSerial = DtoUtil.getString(node.getChassises().iterator().next(), "シャーシ・シリアル番号");
        }
        return chassisSerial;
    }

    public String getRack() {
        return DtoUtil.getStringOrNull(node, "ラック番号");
    }

    public String getSystemMtu() {
        return DtoUtil.getStringOrNull(node, "system mtu");
    }

    public String getSdmPrefer() {
        return DtoUtil.getStringOrNull(node, "sdm prefer");
    }

    public String getLastEditor() {
        return DtoUtil.getStringOrNull(node, "_LastEditor");
    }

    public String getNote() {
        return DtoUtil.getStringOrNull(node, "備考");
    }

    public String getLastEditTime() {
        return DtoUtil.getStringOrNull(node, "_LastEditTime");
    }

    public String getVersion() {
        return DtoUtil.getMvoVersionString(node);
    }
}