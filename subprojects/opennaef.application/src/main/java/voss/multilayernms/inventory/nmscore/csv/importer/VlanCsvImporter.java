package voss.multilayernms.inventory.nmscore.csv.importer;

import jp.iiga.nmt.core.model.CsvImportQuery;
import naef.dto.CustomerInfoDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIdPoolDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.builder.CustomerInfoCommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.csv.CsvImporter;
import voss.multilayernms.inventory.nmscore.csv.importer.setter.util.CsvVlanBuilderSetterUtil;
import voss.multilayernms.inventory.nmscore.model.converter.VlanModelDisplayNameConverter;
import voss.multilayernms.inventory.renderer.VlanRenderer;
import voss.nms.inventory.builder.AttributeUpdateCommandBuilder;
import voss.nms.inventory.database.InventoryConnector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VlanCsvImporter extends CsvImporter {

    public VlanCsvImporter(CsvImportQuery query) throws IOException {
        super(query, new VlanModelDisplayNameConverter());
    }

    @Override
    public void commit(String editorName) throws IOException, InventoryException, ExternalServiceException {

        List<String> fields = getFieldNames();

        int vlanPoolNum = fields.indexOf("VLANPool");
        int vlanIdNum = fields.indexOf("VLAN ID");
        int endUserNum = fields.indexOf("EndUser");
        if (vlanPoolNum < 0 && vlanIdNum < 0 && endUserNum < 0) {
            throw new IllegalArgumentException("CSV file is broken");
        }

        List<List<String>> records = getRecords();
        List<VlanIdPoolDto> pools = VlanRenderer.getVlanIdPools();

        List<AttributeUpdateCommandBuilder> attributeBuilderList = new ArrayList<AttributeUpdateCommandBuilder>();
        List<CustomerInfoCommandBuilder> customerInfoBuilderList = new ArrayList<CustomerInfoCommandBuilder>();

        for (List<String> record : records) {
            VlanDto target = getTarget(pools, vlanPoolNum, vlanIdNum, record);
            if (target == null) {
                continue;
            }

            AttributeUpdateCommandBuilder builder = new AttributeUpdateCommandBuilder(target, editorName);

            for (int i = 0; i < fields.size(); i++) {
                CsvVlanBuilderSetterUtil.setBuilderAttribute(builder, fields.get(i), record.get(i));
            }

            String endUser = record.get(endUserNum);
            if (!endUser.isEmpty()) {
                createCustomerInfoBuilder(customerInfoBuilderList, endUser, target, editorName);
            }


            BuildResult result = builder.buildCommand();
            if (result == BuildResult.SUCCESS) {
                attributeBuilderList.add(builder);
            } else if (result == BuildResult.FAIL) {
                log.debug(record.toString());
                throw new IllegalArgumentException("build fail");
            }

        }


        List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();
        for (CustomerInfoCommandBuilder builder : customerInfoBuilderList) {
            BuildResult result = builder.buildCommand();
            if (result == BuildResult.SUCCESS) {
                commandBuilderList.add(builder);
            } else if (result == BuildResult.FAIL) {
                log.debug(builder.toString());
                throw new IllegalArgumentException("build fail");
            }
        }

        commandBuilderList.addAll(attributeBuilderList);
        if (commandBuilderList.size() > 0) {
            ShellConnector.getInstance().executes(commandBuilderList);
        }
    }

    private void createCustomerInfoBuilder(List<CustomerInfoCommandBuilder> customerInfoBuilderList, String endUser, VlanDto target, String editorName) throws IOException, ExternalServiceException {

        for (CustomerInfoCommandBuilder customerInfoBuilder : customerInfoBuilderList) {
            if (endUser.equals(customerInfoBuilder.getID())) {
                customerInfoBuilder.addTarget(target);
                return;
            }
        }

        CustomerInfoDto customerInfo = InventoryConnector.getInstance().getCustomerInfoByName(endUser);
        CustomerInfoCommandBuilder customerInfoBuilder;

        if (customerInfo != null) {
            customerInfoBuilder = new CustomerInfoCommandBuilder(customerInfo, editorName);
        } else {
            customerInfoBuilder = new CustomerInfoCommandBuilder(editorName);
            customerInfoBuilder.setID(endUser);
        }

        customerInfoBuilder.addTarget(target);
        customerInfoBuilderList.add(customerInfoBuilder);
    }

    protected VlanDto getTarget(List<VlanIdPoolDto> pools, int vlanPoolNum, int vlanIdNum, List<String> record) {
        String poolName = record.get(vlanPoolNum);
        String vlanId = record.get(vlanIdNum);

        for (VlanIdPoolDto pool : pools) {
            if (pool.getName().equals(poolName)) {
                for (VlanDto vlan : pool.getUsers()) {
                    if (vlanId.equals(VlanRenderer.getVlanId(vlan))) {
                        return vlan;
                    }
                }
            }
        }
        return null;
    }
}