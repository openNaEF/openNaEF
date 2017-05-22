package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.resistvlansubnet.ResistVlanSubnetModel;
import jp.iiga.nmt.core.model.resistvlansubnet.VlanIdAndSubnetAddress;
import jp.iiga.nmt.core.model.resistvlansubnet.VlanSubnetFailList;
import naef.dto.CustomerInfoDto;
import naef.dto.IdRange;
import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIdPoolDto;
import naef.mvo.ip.IpAddress;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.builder.CustomerInfoCommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.constants.CustomerConstants;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;
import voss.multilayernms.inventory.renderer.CustomerInfoRenderer;
import voss.nms.inventory.builder.IpSubnetCommandBuilder;
import voss.nms.inventory.builder.VlanCommandBuilder;
import voss.nms.inventory.util.VlanUtil;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InventoryResistVlanAndSubnetAddressState extends UnificUIViewState {

    public InventoryResistVlanAndSubnetAddressState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException, IOException, InventoryException, ExternalServiceException {
        try {
            Object obj = Operation.getTargets(context);

            if (obj instanceof ResistVlanSubnetModel) {
                ResistVlanSubnetModel model = (ResistVlanSubnetModel) obj;

                String customerName = model.getCustomerName();
                String areaCode = model.getAreaCode();
                String userCode = model.getUserCode();
                String location = model.getLocation();
                int maskLength = model.getMaskLength();
                Map<String, VlanIdAndSubnetAddress> map = model.getList();
                String user = context.getUser();
                VlanSubnetFailList failList = new VlanSubnetFailList();
                VlanIdPoolDto pool = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getVlanPoolMvoId(), VlanIdPoolDto.class);
                List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();
                CustomerInfoCommandBuilder customerBuilder = createCustomerInfoBuilder(getCustomerInfoDto(customerName, pool), user, customerName);
                for (String key : map.keySet()) {
                    VlanIdAndSubnetAddress entity = map.get(key);
                    IpSubnetNamespaceDto namespace = MplsNmsInventoryConnector.getInstance().getMvoDto(entity.getMasterSubnetMvoId(), IpSubnetNamespaceDto.class);
                    log.debug(namespace.getAbsoluteName());
                    if (namespace == null) {
                        throw new IllegalArgumentException("Please select the master-subnet.");
                    }
                    if (validate(key, failList, pool, namespace, entity, maskLength)) {
                        List<CommandBuilder> list = create(failList, customerBuilder, pool, namespace,
                                key, entity, areaCode, userCode, location, maskLength, user);
                        commandBuilderList.addAll(list);
                    }
                }

                if (BuildResult.FAIL == customerBuilder.buildCommand()) {
                    failList.add("Customer Info", "Failed to create customer info.");
                }
                commandBuilderList.add(customerBuilder);

                if (failList.size() <= 0) {
                    try {
                        ShellConnector.getInstance().executes(commandBuilderList);
                    } catch (InventoryException e) {
                        String msg;
                        if (e.getCause() == null) {
                            msg = e.getMessage();
                        } else {
                            msg = e.getCause().getMessage();
                        }
                        failList.add("Error", msg);
                        log.error("", e);
                    }
                }

                setXmlObject(failList);

            } else {
                throw new IllegalArgumentException("Target is wrong.");
            }
            super.execute(context);
        } catch (InventoryException e) {
            log.error("" + e);
            throw e;
        } catch (ExternalServiceException e) {
            log.error("" + e);
            throw e;
        } catch (IOException e) {
            log.error("" + e);
            throw e;
        } catch (RuntimeException e) {
            log.error("" + e);
            throw e;
        } catch (ServletException e) {
            log.error("", e);
            throw e;
        }
    }

    private boolean validate(String key, VlanSubnetFailList failList, VlanIdPoolDto pool, IpSubnetNamespaceDto namespace, VlanIdAndSubnetAddress entity, int maskLength) {
        boolean checkVlan = true;
        ;
        if (entity.isRegistVlan()) {
            checkVlanId(key, failList, pool, entity.getVlanId());
        }
        boolean checkSubnet = checkSubnetAddress(key, failList, namespace, entity.getSubnetAddress(), maskLength);

        return checkVlan && checkSubnet;
    }

    private boolean checkVlanId(String serviceType, VlanSubnetFailList failList, VlanIdPoolDto pool, int vlanId) {
        boolean result = true;

        for (IdRange<Integer> range : pool.getIdRanges()) {
            if (range.lowerBound > vlanId || vlanId > range.upperBound) {
                failList.add(serviceType, "VLAN ID : " + vlanId + " is out of range.");
                result = false;
                break;
            }
        }

        if (VlanUtil.getVlan(pool, vlanId) != null) {
            failList.add(serviceType, "VLAN ID : " + vlanId + " is already paid out.");
            result = false;
        }

        return result;
    }

    private boolean checkSubnetAddress(String serviceType, VlanSubnetFailList failList, IpSubnetNamespaceDto namespace, String subnetAddress, int maskLength) {
        boolean result = true;
        if (!checkSubnetAddressRenge(namespace, subnetAddress, maskLength)) {
            failList.add(serviceType, "Subnet Address : " + subnetAddress + " is out of range.");
            result = false;
        }

        return result;
    }


    private boolean checkSubnetAddressRenge(IpSubnetNamespaceDto namespace, String subnetAddress, int maskLength) {
        IpSubnetAddressDto ipSubnetAddress = namespace.getIpSubnetAddress();

        if (ipSubnetAddress.getSubnetMask().intValue() > maskLength) {
            return false;
        }

        IpAddress ip = IpAddress.gain(subnetAddress);
        Set<IdRange<IpAddress>> ranges = ipSubnetAddress.getIdRanges();
        for (IdRange<IpAddress> range : ranges) {
            IpAddress lower = range.lowerBound;
            IpAddress upper = range.upperBound;
            if (0 >= lower.compareTo(ip) && upper.compareTo(ip) >= 0) {
                return true;
            }
        }

        return false;
    }

    private List<CommandBuilder> create(VlanSubnetFailList failList, CustomerInfoCommandBuilder customerBuilder,
                                        VlanIdPoolDto pool, IpSubnetNamespaceDto namespace, String serviceName, VlanIdAndSubnetAddress entity,
                                        String areaCode, String userCode, String location, int maskLength, String user
    ) throws IOException, ExternalServiceException, InventoryException {
        List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();
        VlanCommandBuilder vlanBuilder = createVlanBuilder(pool, user, entity, areaCode, userCode, serviceName, location);
        IpSubnetCommandBuilder ipSubnetBuilder = createIpSubnetBuilder(namespace, user, entity.getSubnetAddress(), maskLength);
        if (entity.isRegistVlan()) {
            String vlanAbsoluteName = vlanBuilder.getContext();
            String ipSubnetAbsoluteName = ipSubnetBuilder.getContext();
            customerBuilder.addTarget(vlanAbsoluteName);
            customerBuilder.addTarget(ipSubnetAbsoluteName);
            ipSubnetBuilder.addLowerLayerNetwork(vlanAbsoluteName);
        }
        if (entity.isUseExistingVlan()) {
            VlanDto vlan = getVlan(pool, entity.getVlanId());
            String ipSubnetAbsoluteName = ipSubnetBuilder.getContext();
            if (vlan != null) {
                String vlanAbsoluteName = vlan.getAbsoluteName();
                customerBuilder.addTarget(vlanAbsoluteName);
                customerBuilder.addTarget(ipSubnetAbsoluteName);
                ipSubnetBuilder.addLowerLayerNetwork(vlanAbsoluteName);
            } else {
                failList.add(serviceName, "VLAN ID:" + entity.getVlanId() + " does not exit");
            }
        }
        BuildResult vlanResult = null;
        if (vlanBuilder != null) {
            vlanResult = vlanBuilder.buildCommand();
        }
        BuildResult ipSubnetResult = ipSubnetBuilder.buildCommand();
        if ((vlanResult != null && BuildResult.FAIL == vlanResult) || BuildResult.FAIL == ipSubnetResult) {
            if (vlanResult != null && BuildResult.FAIL == vlanResult) {
                failList.add(serviceName, "Failed to create VLAN.");
            }

            if (BuildResult.FAIL == ipSubnetResult) {
                failList.add(serviceName, "Failed to create IP subnet.");
            }
            return commandBuilderList;
        }
        commandBuilderList.add(vlanBuilder);
        commandBuilderList.add(ipSubnetBuilder);
        return commandBuilderList;
    }

    private VlanDto getVlan(VlanIdPoolDto pool, int vlanId) {
        for (VlanDto vlan : pool.getUsers()) {
            if (vlan.getVlanId().equals(vlanId)) {
                return vlan;
            }
        }
        return null;
    }

    private IpSubnetCommandBuilder createIpSubnetBuilder(IpSubnetNamespaceDto namespace, String user, String subnetAddress, int maskLength) {
        IpSubnetCommandBuilder ipSubnetBuilder = new IpSubnetCommandBuilder(namespace, user);
        ipSubnetBuilder.setStartAddress(subnetAddress);
        ipSubnetBuilder.setMaskLength(maskLength);
        return ipSubnetBuilder;
    }

    private CustomerInfoCommandBuilder createCustomerInfoBuilder(CustomerInfoDto customerInfo, String user, String customerName) {
        CustomerInfoCommandBuilder csbuilder = null;
        if (customerInfo != null) {
            csbuilder = new CustomerInfoCommandBuilder(customerInfo, user);
        } else {
            csbuilder = new CustomerInfoCommandBuilder(user);
            csbuilder.setID(customerName);
        }
        return csbuilder;
    }

    private VlanCommandBuilder createVlanBuilder(VlanIdPoolDto pool,
                                                 String user, VlanIdAndSubnetAddress entity, String areaCode,
                                                 String userCode, String serviceName, String location) {

        if (!entity.isRegistVlan()) return null;
        if (entity.isUseExistingVlan()) return null;
        VlanCommandBuilder vlanBuilder = new VlanCommandBuilder(pool, user);
        vlanBuilder.setVlanID(entity.getVlanId());
        vlanBuilder.setValue(CustomerConstants.AREA_CODE, areaCode);
        vlanBuilder.setValue(CustomerConstants.USER_CODE, userCode);
        vlanBuilder.setPurpose(serviceName);
        vlanBuilder.setValue(CustomerConstants.AP_NAME, location);
        return vlanBuilder;
    }

    private CustomerInfoDto getCustomerInfoDto(String target, VlanIdPoolDto pool) {
        Set<VlanDto> vlans = pool.getUsers();
        CustomerInfoDto customerInfo = null;
        if (vlans != null && vlans.size() > 0) {
            for (VlanDto vlan : vlans) {
                for (CustomerInfoDto cs : vlan.getCustomerInfos()) {
                    if (CustomerInfoRenderer.getCustomerInfoId(cs).equals(target)) {
                        customerInfo = cs;
                        break;
                    }
                }
            }
        }
        return customerInfo;
    }
}