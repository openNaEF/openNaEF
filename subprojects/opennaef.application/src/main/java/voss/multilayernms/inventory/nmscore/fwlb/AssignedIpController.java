package voss.multilayernms.inventory.nmscore.fwlb;

import naef.dto.CustomerInfoDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import voss.core.server.builder.CustomerInfoCommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.renderer.SubnetRenderer;
import voss.nms.inventory.builder.IpSubnetCommandBuilder;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;

public class AssignedIpController implements FWLBController {

    @Override
    public boolean canHandle(String action) {
        return action.equals("assignedIp");
    }

    @Override
    public Object handleGet(FlowContext context) throws ServletException {
        return null;
    }

    @Override
    public Object handlePut(FlowContext context, Object data) throws ServletException {
        try {
            String customerId = context.getParameter("customerId");

            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            CustomerInfoDto customerInfoDto = conn.getMvoDto(customerId, CustomerInfoDto.class);

            List<String> assignedIps = (List<String>) data;

            IpSubnetNamespaceDto rootSubnetNameSpace = findSubnetNameSpace("0.0.0.0/0");


            for (String ip : assignedIps) {
                String[] splits = ip.split("/");
                String address = splits[0];
                Integer mask = Integer.parseInt(splits[1]);

                IpSubnetDto target = findIpSubnetDto(rootSubnetNameSpace, address + "/" + mask);
                if (target == null) {
                    createIpSubnet(context.getUser(), rootSubnetNameSpace, address, mask);
                    target = findIpSubnetDto(rootSubnetNameSpace, address);
                }

                associate(context.getUser(), customerInfoDto, target);
            }
        } catch (IOException e) {
            throw new ServletException(e);
        } catch (InventoryException e) {
            throw new ServletException(e);
        } catch (ExternalServiceException e) {
            throw new ServletException(e);
        }

        return new Object();
    }

    private IpSubnetNamespaceDto findSubnetNameSpace(String address) throws ExternalServiceException {
        List<IpSubnetNamespaceDto> nameSpaces = SubnetRenderer.getAllIpSubnetNamespace();

        for (IpSubnetNamespaceDto nameSpace : nameSpaces) {
            if (nameSpace.getName().equals(address)) {
                return nameSpace;
            }
        }
        return null;
    }

    private IpSubnetDto findIpSubnetDto(IpSubnetNamespaceDto rootSubnetNameSpace, String address) {
        for (IpSubnetDto ipSubnet : rootSubnetNameSpace.getUsers()) {
            String testAddress = ipSubnet.getSubnetAddress().getAddress().toString()
                    + "/" + ipSubnet.getSubnetAddress().getSubnetMask().toString();
            if (testAddress.equals(address)) {
                return ipSubnet;
            }
        }
        return null;
    }

    private void createIpSubnet(
            String user,
            IpSubnetNamespaceDto subnetNameSpace,
            String address,
            Integer mask)
            throws IOException, ExternalServiceException, InventoryException {
        IpSubnetCommandBuilder ipSubnetCommandBuilder = new IpSubnetCommandBuilder(subnetNameSpace, user);
        ipSubnetCommandBuilder.setStartAddress(address);
        ipSubnetCommandBuilder.setMaskLength(mask);
        ipSubnetCommandBuilder.buildCommand();
        ShellConnector.getInstance().execute(ipSubnetCommandBuilder);
    }

    private void associate(String user, CustomerInfoDto csInfoDto, IpSubnetDto ipSubnetDto) throws IOException, ExternalServiceException,
            InventoryException {
        CustomerInfoCommandBuilder builder = new CustomerInfoCommandBuilder(csInfoDto, user);
        builder.addTarget(ipSubnetDto);
        builder.buildCommand();
        ShellConnector.getInstance().execute(builder);
    }
}