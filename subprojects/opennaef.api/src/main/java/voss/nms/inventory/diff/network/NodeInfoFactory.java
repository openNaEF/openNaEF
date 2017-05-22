package voss.nms.inventory.diff.network;

import naef.dto.NodeDto;
import voss.core.server.exception.ExternalServiceException;
import voss.model.NodeInfo;

import java.io.IOException;

public interface NodeInfoFactory {
    NodeInfo createNodeInfo(NodeDto node) throws IOException, ExternalServiceException;
}