package voss.multilayernms.inventory.nmscore.csv.importer;

import jp.iiga.nmt.core.model.CsvImportQuery;
import naef.dto.NaefDto;
import naef.dto.NodeDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.csv.CsvImporter;
import voss.multilayernms.inventory.nmscore.csv.importer.setter.util.CsvGeneralBuilderSetterUtil;
import voss.multilayernms.inventory.nmscore.model.converter.NodeModelDisplayNameConverter;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.nms.inventory.builder.AttributeUpdateCommandBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NodeCsvImporter extends CsvImporter {

    public NodeCsvImporter(CsvImportQuery query) throws IOException {
        super(query, new NodeModelDisplayNameConverter());
    }

    @Override
    public void commit(String editorName) throws IOException, InventoryException, ExternalServiceException {

        List<String> fields = getFieldNames();

        int nodeNameNum = fields.indexOf("NodeName");
        int managementIpNum = fields.indexOf("MgmtIPAddr");
        if (nodeNameNum < 0 && managementIpNum < 0) {
            throw new IllegalArgumentException("CSV file is broken");
        }

        List<List<String>> records = getRecords();
        List<NodeDto> nodes = MplsNmsInventoryConnector.getInstance().getActiveNodes();

        List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();

        for (List<String> record : records) {
            NaefDto target = getTarget(nodes, nodeNameNum, managementIpNum, record);
            if (target == null) {
                continue;
            }

            AttributeUpdateCommandBuilder builder = new AttributeUpdateCommandBuilder(target, editorName);

            for (int i = 0; i < fields.size(); i++) {
                CsvGeneralBuilderSetterUtil.setBuilderAttribute(builder, fields.get(i), record.get(i));
            }

            BuildResult result = builder.buildCommand();
            if (result == BuildResult.SUCCESS) {
                commandBuilderList.add(builder);
            } else if (result == BuildResult.FAIL) {
                log.debug(record.toString());
                throw new IllegalArgumentException("build fail");
            }

        }
        if (commandBuilderList.size() > 0) {
            ShellConnector.getInstance().executes(commandBuilderList);
        }
    }

    protected NaefDto getTarget(List<NodeDto> nodes, int nodeNameNum, int managementIpNum, List<String> record) {
        for (NodeDto node : nodes) {
            String nodeName = NodeRenderer.getNodeName(node);
            String managementIp = NodeRenderer.getManagementIpAddress(node);

            if (record.get(nodeNameNum).equals(nodeName)) {
                if ((record.get(managementIpNum).isEmpty() && managementIp == null)
                        || record.get(managementIpNum).equals(managementIp)) {
                    return node;
                }
            }
        }
        return null;
    }
}