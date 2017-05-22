package voss.multilayernms.inventory.builder;

import naef.dto.CustomerInfoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CustomerInfoCommandBuilder;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.constants.CustomerConstants;
import voss.multilayernms.inventory.renderer.CustomerInfoRenderer;

import java.io.IOException;
import java.util.List;

public class CustomerInfoAttributeCommandBuilder extends CustomerInfoCommandBuilder {
    private static final Logger log = LoggerFactory.getLogger(CustomerInfoAttributeCommandBuilder.class);
    private static final long serialVersionUID = 1L;
    private final CustomerInfoDto customer;

    public CustomerInfoAttributeCommandBuilder(CustomerInfoDto customer, String editorName) {
        super(customer, editorName);
        if (customer == null) {
            throw new IllegalArgumentException("no customer-info.");
        }
        this.customer = customer;
        setConstraint(CustomerInfoDto.class);
    }

    public void setFirewallPolicies(List<String> policies) {
        List<String> current = CustomerInfoRenderer.getFWPoliciesZoneMatrix(customer);
        replaceValues(CustomerConstants.FW_POLICY_ZONE_MATRIX, policies, true);
        recordChangesDiff(customer, CustomerConstants.FW_POLICY_ZONE_MATRIX, policies, current, false);
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException, ExternalServiceException {
        BuildResult result = super.buildCommandInner();
        switch (result) {
            case FAIL:
                return result;
        }
        InventoryBuilder.changeContext(cmd, customer);
        InventoryBuilder.buildSetAttributeUpdateCommand(cmd, customer, listAttributes);
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, InventoryException, ExternalServiceException {
        InventoryBuilder.changeContext(cmd, customer);
        InventoryBuilder.buildCollectionAttributeRemoveCommands(cmd, customer, CustomerConstants.FW_POLICY_ZONE_MATRIX);
        return super.buildDeleteCommandInner();
    }

}