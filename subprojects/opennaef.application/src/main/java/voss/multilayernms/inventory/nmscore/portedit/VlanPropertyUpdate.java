package voss.multilayernms.inventory.nmscore.portedit;

import jp.iiga.nmt.core.model.portedit.Vlan;
import jp.iiga.nmt.core.model.portedit.VlanEditModel;
import naef.dto.CustomerInfoDto;
import naef.dto.vlan.VlanDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.builder.CustomerInfoCommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.constants.CustomerConstants;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.CustomerInfoRenderer;
import voss.nms.inventory.builder.VlanCommandBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


public class VlanPropertyUpdate implements IPortEditUpdate {
    private Vlan model;
    private String user;

    public VlanPropertyUpdate(VlanEditModel model, String user) {
        this.model = (Vlan) model;
        this.user = user;
    }

    @Override
    public void update() throws RuntimeException, IOException, InventoryException, ExternalServiceException {
        try {
            VlanDto vlan = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getMvoId(), VlanDto.class);
            List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();
            if (vlan != null) {
                VlanCommandBuilder vlanbuilder = new VlanCommandBuilder(vlan, user);
                populateBuilder(vlanbuilder);
                commandBuilderList.add(vlanbuilder);
                List<CustomerInfoCommandBuilder> csBuilders = createCsBuilders(vlan);
                commandBuilderList.addAll(csBuilders);
                for (CommandBuilder builder : commandBuilderList) {
                    BuildResult result = builder.buildCommand();
                    if (BuildResult.FAIL == result) {
                        throw new IllegalStateException("Update Fail.");
                    }
                }
                ShellConnector.getInstance().executes(commandBuilderList);
            }
        } catch (InventoryException e) {
            log.debug("InventoryException", e);
            if (e.getCause() == null) {
                throw new InventoryException(e);
            } else {
                throw new InventoryException(e.getCause().getMessage());
            }
        } catch (ExternalServiceException e) {
            log.debug("ExternalServiceException", e);
            if (e.getCause() == null) {
                throw new ExternalServiceException(e);
            } else {
                throw new ExternalServiceException(e.getCause().getMessage());
            }
        } catch (IOException e) {
            log.debug("IOException", e);
            if (e.getCause() == null) {
                throw new IOException(e);
            } else {
                throw new IOException(e.getCause().getMessage());
            }
        } catch (RuntimeException e) {
            log.debug("RuntimeException", e);
            if (e.getCause() == null) {
                throw new RuntimeException(e);
            } else {
                throw new RuntimeException(e.getCause().getMessage());
            }
        }
    }

    private void populateBuilder(VlanCommandBuilder vlanbuilder) {
        vlanbuilder.setValue(CustomerConstants.AREA_CODE, model.getAreaCode());
        vlanbuilder.setValue(CustomerConstants.USER_CODE, model.getUserCode());
        vlanbuilder.setPurpose(model.getPurpose());
        vlanbuilder.setValue(CustomerConstants.NOTICES, model.getNotices());
    }

    private List<CustomerInfoCommandBuilder> createCsBuilders(VlanDto vlan) throws IOException, ExternalServiceException, InventoryException {
        List<CustomerInfoCommandBuilder> result = new ArrayList<CustomerInfoCommandBuilder>();
        List<String> dialogCS = new ArrayList<String>();
        CustomerInfoCommandBuilder csbuilder = null;
        String customerNames = model.getUser();
        if (customerNames.contains(",")) {
            String[] tmp = customerNames.split(",");
            for (String cs : tmp) {
                if (!cs.isEmpty()) {
                    dialogCS.add(cs);
                }
            }
        } else {
            if (!customerNames.isEmpty()) {
                dialogCS.add(customerNames);
            }
        }
        Collection<CustomerInfoDto> currentCustomerInfos = vlan.getCustomerInfos();
        if (currentCustomerInfos.isEmpty() && dialogCS.isEmpty()) {
            return result;
        }

        for (String cs : dialogCS) {
            Set<CustomerInfoDto> dbCustomerInfos = CustomerInfoRenderer.getCustomerInfoExactMatchByName(cs);
            if (dbCustomerInfos.isEmpty()) {
                csbuilder = new CustomerInfoCommandBuilder(user);
                csbuilder.setID(cs);
                csbuilder.addTarget(vlan);
                result.add(csbuilder);
            } else if (dbCustomerInfos.size() > 0) {
                for (CustomerInfoDto csinf : dbCustomerInfos) {
                    csbuilder = new CustomerInfoCommandBuilder(csinf, user);
                    csbuilder.addTarget(vlan);
                    result.add(csbuilder);
                }
            }
        }

        for (CustomerInfoDto currentCustomerInfo : currentCustomerInfos) {
            String currentCustomerName = DtoUtil.getString(currentCustomerInfo, "ID");
            if (!dialogCS.contains(currentCustomerName)) {
                CustomerInfoCommandBuilder currentCsInfoBuilder = new CustomerInfoCommandBuilder(currentCustomerInfo, user);
                currentCsInfoBuilder.removeTarget(vlan.getAbsoluteName());
                result.add(currentCsInfoBuilder);
            }
        }
        return result;
    }
}