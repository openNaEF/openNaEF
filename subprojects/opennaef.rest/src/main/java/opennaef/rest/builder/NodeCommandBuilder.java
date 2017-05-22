package opennaef.rest.builder;

import naef.dto.LocationDto;
import naef.dto.NodeDto;

/**
 * Node 編集
 */
public class NodeCommandBuilder extends voss.nms.inventory.builder.NodeCommandBuilder {

    /**
     * 新規作成
     * @param editorName 編集者
     */
    public NodeCommandBuilder(String editorName) {
        super(editorName);
    }

    /**
     * 更新・削除
     * @param node 対象ノード
     * @param editorName 編集者
     */
    public NodeCommandBuilder(NodeDto node, String editorName) {
        super(node, editorName);
    }

    /**
     * Location と紐づける
     * @param locationAbsoluteName location 絶対名
     */
    @Override
    public void setLocation(String locationAbsoluteName) {
        super.setLocation(locationAbsoluteName);
    }

    /**
     * Location と紐づける
     * @param location location
     */
    public void setLocation(LocationDto location) {
        if (location == null) {
            throw new IllegalArgumentException("null location not allowed.");
        }
        this.setLocation(location.getAbsoluteName());
    }
}
