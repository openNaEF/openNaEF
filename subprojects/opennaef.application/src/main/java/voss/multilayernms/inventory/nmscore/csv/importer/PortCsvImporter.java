package voss.multilayernms.inventory.nmscore.csv.importer;

import jp.iiga.nmt.core.model.CsvImportQuery;
import naef.dto.NaefDto;
import naef.dto.PortDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.csv.CsvImporter;
import voss.multilayernms.inventory.nmscore.csv.importer.setter.util.CsvPortBuilderSetterUtil;
import voss.multilayernms.inventory.nmscore.inventory.accessor.PortHandler;
import voss.multilayernms.inventory.nmscore.model.converter.PortModelDisplayNameConverter;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.nms.inventory.builder.AttributeUpdateCommandBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PortCsvImporter extends CsvImporter {

    public PortCsvImporter(CsvImportQuery query) throws IOException {
        super(query, new PortModelDisplayNameConverter());
    }

    @Override
    public void commit(String editorName) throws IOException, InventoryException, ExternalServiceException {

        List<String> fields = getFieldNames();

        int nodeNameNum = fields.indexOf("NodeName");
        int ifNameNum = fields.indexOf("ifName");
        if (nodeNameNum < 0 && ifNameNum < 0) {
            throw new IllegalArgumentException("CSV file is broken");
        }

        List<List<String>> records = getRecords();
        List<PortDto> ports = PortHandler.getActivePorts();

        List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();

        for (List<String> record : records) {
            NaefDto target = getTarget(ports, nodeNameNum, ifNameNum, record);
            if (target == null) {
                continue;
            }

            AttributeUpdateCommandBuilder builder = new AttributeUpdateCommandBuilder(target, editorName);

            for (int i = 0; i < fields.size(); i++) {
                CsvPortBuilderSetterUtil.setBuilderAttribute(builder, fields.get(i), record.get(i));
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

    protected NaefDto getTarget(List<PortDto> ports, int nodeNameNum, int ifNameNum, List<String> record) {
        for (PortDto port : ports) {
            String nodeName = PortRenderer.getNodeName(port);
            String ifName = PortRenderer.getIfName(port);

            if (record.get(nodeNameNum).equals(nodeName) && record.get(ifNameNum).equals(ifName)) {
                if (port.isAlias()) {
                    return port.getAliasSource();
                }

                return port;
            }
        }
        return null;
    }
}