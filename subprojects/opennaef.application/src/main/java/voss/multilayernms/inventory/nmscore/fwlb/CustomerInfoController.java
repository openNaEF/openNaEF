package voss.multilayernms.inventory.nmscore.fwlb;

import naef.dto.*;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIfDto;
import naef.ui.NaefDtoFacade;
import naef.ui.NaefDtoFacade.SearchMethod;
import net.phalanx.fwlb.model.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.builder.CustomerInfoCommandBuilder;
import voss.core.server.builder.SystemUserCommandBuilder;
import voss.core.server.constant.SystemUserAttribute;
import voss.core.server.database.ATTR;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.Util;
import voss.multilayernms.inventory.builder.CustomerInfoAttributeCommandBuilder;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.constraints.PortEditConstraints;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.renderer.CustomerInfoRenderer;
import voss.nms.inventory.builder.AttributeUpdateCommandBuilder;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomerInfoController implements FWLBController {

    private static final Logger log = LoggerFactory.getLogger(CustomerInfoController.class);

    @Override
    public boolean canHandle(String action) {
        return action.equals("customerInfo");
    }

    @Override
    public Object handleGet(FlowContext context) throws ServletException {
        String attribute = context.getParameter("attribute");
        if ((attribute != null) && attribute.equals("fwPolicy")) {
            String customerId = context.getParameter("customerId");
            CustomerInfoDto cs = null;
            try {
                cs = MplsNmsInventoryConnector.getInstance().getMvoDto(customerId, CustomerInfoDto.class);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InventoryException e) {
                e.printStackTrace();
            } catch (ExternalServiceException e) {
                e.printStackTrace();
            }
            return CustomerInfoRenderer.getFWPoliciesZoneMatrix(cs);
        } else {
            Collection<Customer> customers = new ArrayList<Customer>();
            try {
                MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
                NaefDtoFacade facade;
                facade = conn.getDtoFacade();

                Collection<CustomerInfoDto> dtos = new ArrayList<CustomerInfoDto>();
                dtos.addAll(facade.selectCustomerInfos(SearchMethod.REGEXP, ATTR.CUSTOMER_INFO_ID, ".*"));

                for (CustomerInfoDto dto : dtos) {
                    String customerVdom = getCustomerVdom(dto);
                    Customer customer = new Customer();
                    customer.setId(dto.getOid().toString());
                    customer.setName(CustomerInfoRenderer.getCustomerName(dto));
                    customer.setVDOM(customerVdom);
                    customers.add(customer);
                }
            } catch (IOException e) {
                throw new ServletException(e);
            } catch (ExternalServiceException e) {
                throw new ServletException(e);
            }
            return customers;
        }
    }

    private String getCustomerVdom(CustomerInfoDto dto) {
        String customerVdom = null;
        for (NaefDto naefdto : dto.getReferences()) {
            if (naefdto instanceof VlanDto) {
                VlanDto vlan = VlanDto.class.cast(naefdto);
                for (VlanIfDto vif : vlan.getMemberVlanifs()) {
                    if (vif.getAliases().size() > 0) {
                        for (PortDto p : vif.getAliases()) {
                            if (PortEditConstraints.isPortDirectLine(p.getAliasSource())) {
                                customerVdom = p.getNode().getName();
                            }
                        }
                    }
                }
            }
        }
        return customerVdom;
    }

    @Override
    public Object handlePut(FlowContext context, Object data) throws ServletException {
        try {
            String attribute = context.getParameter("attribute");
            if ((attribute != null) && attribute.equals("customerName")) {
                updateCustomerName(context, data);
            } else if ((attribute != null) && attribute.equals("fwPolicy")) {
                updateFwPolicy(context, data);
            } else {
                updateCustomer(context, data);
            }
        } catch (IOException e) {
            throw new ServletException(e);
        } catch (ExternalServiceException e) {
            throw new ServletException(e);
        } catch (InventoryException e) {
            throw new ServletException(e);
        }

        return null;
    }

    private void updateCustomerName(FlowContext context, Object data)
            throws ServletException, IOException, InventoryException,
            ExternalServiceException {
        String customerId = context.getParameter("customerId");
        String customerName = null;
        if (data instanceof String) {
            customerName = (String) data;
        }
        if (customerId == null || customerName == null) {
            throw new ServletException("Illegal request");
        }

        CustomerInfoDto customerInfoDto =
                MplsNmsInventoryConnector.getInstance().getMvoDto(
                        customerId, CustomerInfoDto.class);
        AttributeUpdateCommandBuilder builder =
                new AttributeUpdateCommandBuilder(customerInfoDto, context.getUser());
        builder.setValue("ID", customerName);
        builder.buildCommand();

        ShellConnector.getInstance().execute(builder);
    }

    @SuppressWarnings("unchecked")
    private void updateFwPolicy(FlowContext context, Object data)
            throws IOException, InventoryException, ExternalServiceException {
        String customerId = context.getParameter("customerId");
        List<String> policies = (List<String>) data;

        CustomerInfoDto cs = MplsNmsInventoryConnector.getInstance().getMvoDto(customerId, CustomerInfoDto.class);
        log.debug(String.format("customerId: %s, policies: %s", customerId, policies.toString()));

        CustomerInfoAttributeCommandBuilder customerInfoAttributeCommandBuilder = new CustomerInfoAttributeCommandBuilder(cs, context.getUser());
        customerInfoAttributeCommandBuilder.setFirewallPolicies(policies);
        customerInfoAttributeCommandBuilder.buildCommand();
        ShellConnector.getInstance().execute(customerInfoAttributeCommandBuilder);
    }


    private void updateCustomer(FlowContext context, Object data)
            throws IOException, ExternalServiceException, InventoryException,
            ServletException {
        Customer customer = (Customer) data;
        MplsNmsInventoryConnector conn;
        List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();

        conn = MplsNmsInventoryConnector.getInstance();

        CustomerInfoDto customerInfoDto = conn.getMvoDto(customer.getId(), CustomerInfoDto.class);
        CustomerInfoCommandBuilder customerInfobuilder = new CustomerInfoCommandBuilder(customerInfoDto, context.getUser());

        if (!customer.isAffectOnlyLB()) {
            buildSystemUser(customer, context);

            SystemUserDto systemUser = getSystemUserDtoByName(customer);

            customerInfobuilder.setSystemUser(systemUser);

            NodeDto vdom = getNodeDtoByName(customer.getVDOM());
            if (vdom != null) {
                customerInfobuilder.addTarget(vdom);
                customerInfobuilder.setValue(FWLBATTR.FMPortalUser, customer.getFMPortalUser());
                customerInfobuilder.setValue(FWLBATTR.FMPortalPass, customer.getFMPortalPass());
            }
        }
        if (!customer.getLB().getName().equals("NONE")) {
            NodeDto lb = getNodeDtoByName(customer.getLB().getName());
            if (lb != null) {
                customerInfobuilder.addTarget(lb);
                customerInfobuilder.setValue(FWLBATTR.SSL_MAX_LIMIT, customer.getSSLMaxLimit());
                customerInfobuilder.setValue(FWLBATTR.MEMBER_SERVER_MAX_LIMIT, customer.getMemberMaxLimit());
            }
        } else {
            log.debug("LB name=" + customer.getLB().getName());
        }
        customerInfobuilder.buildCommand();
        commandBuilderList.add(customerInfobuilder);

        ShellConnector.getInstance().executes(commandBuilderList);
    }

    private SystemUserDto getSystemUserDtoByName(Customer customer) throws ExternalServiceException, IOException {
        MplsNmsInventoryConnector conn;
        conn = MplsNmsInventoryConnector.getInstance();
        for (SystemUserDto systemUser : conn.getSystemUsers()) {
            if (systemUser.getName().equals(customer.getSystemUser())) {
                return systemUser;
            }
        }
        return null;
    }

    private void buildSystemUser(Customer customer, FlowContext context) throws IOException, ExternalServiceException, InventoryException {
        SystemUserDto existingSystemUser = getSystemUserDtoByName(customer);

        if (existingSystemUser == null) {
            SystemUserCommandBuilder subuilder;
            subuilder = new SystemUserCommandBuilder(customer.getSystemUser(), context.getUser());

            subuilder.setActive(true);
            subuilder.setCaption(customer.getSystemUser());
            String hash = Util.getDigest("SHA-256", customer.getSystemPass());
            subuilder.setPasswordScheme("SHA-256");
            subuilder.setPasswordHash(hash);
            subuilder.buildCommand();
            ShellConnector.getInstance().execute(subuilder);
        } else if (existingSystemUser != null) {
            AttributeUpdateCommandBuilder attrbuilder = new AttributeUpdateCommandBuilder(existingSystemUser, context.getUser());
            String hash = Util.getDigest("SHA-256", customer.getSystemPass());
            attrbuilder.setValue(SystemUserAttribute.AUTH_PASSWORD_SCHEME, "SHA-256");
            attrbuilder.setValue(SystemUserAttribute.AUTH_PASSWORD_HASH, hash);
            attrbuilder.buildCommand();
            ShellConnector.getInstance().execute(attrbuilder);
        }

    }

    private NodeDto getNodeDtoByName(String nodeName) throws ServletException, ExternalServiceException, IOException {
        MplsNmsInventoryConnector conn;
        conn = MplsNmsInventoryConnector.getInstance();
        for (NodeDto node : conn.getActiveNodes()) {
            if (node.getName().equals(nodeName)) {
                return node;
            }
        }
        return null;
    }
}