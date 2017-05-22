package voss.discovery.agent.vmware.test;

import com.vmware.vim25.*;

import java.net.URL;
import java.util.HashMap;

public class VSwitchPortListCollector {

    private static void collectProperties(VimPortType methods, ServiceContent sContent) throws Exception {
        ManagedObjectReference propColl = sContent.getPropertyCollector();

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(sContent.getRootFolder());
        oSpec.setSkip(false);

        TraversalSpec tSpec = new TraversalSpec();
        tSpec.setType("Folder");
        tSpec.setPath("childEntity");
        tSpec.setSkip(false);

        oSpec.setSelectSet(new TraversalSpec[]{tSpec});

        TraversalSpec dcToNetwork = new TraversalSpec();
        dcToNetwork.setType("Datacenter");
        dcToNetwork.setPath("network");
        dcToNetwork.setSkip(false);

        TraversalSpec dcToVmFolder = new TraversalSpec();
        dcToVmFolder.setType("Datacenter");
        dcToVmFolder.setPath("vmFolder");
        dcToVmFolder.setSkip(false);

        tSpec.setSelectSet(new TraversalSpec[]{dcToNetwork, dcToVmFolder});

        TraversalSpec folderToVirtualMachine = new TraversalSpec();
        folderToVirtualMachine.setType("Folder");
        folderToVirtualMachine.setPath("childEntity");
        folderToVirtualMachine.setSkip(false);

        dcToVmFolder.setSelectSet(new TraversalSpec[]{folderToVirtualMachine});

        TraversalSpec networkToHost = new TraversalSpec();
        networkToHost.setType("Network");
        networkToHost.setPath("host");
        networkToHost.setSkip(false);

        dcToNetwork.setSelectSet(new TraversalSpec[]{networkToHost});

        TraversalSpec hostSystemToConfigManager = new TraversalSpec();
        hostSystemToConfigManager.setType("HostSystem");
        hostSystemToConfigManager.setPath("configManager.networkSystem");
        hostSystemToConfigManager.setSkip(false);

        networkToHost.setSelectSet(new TraversalSpec[]{hostSystemToConfigManager});

        PropertySpec pSpecVMConfig = new PropertySpec();
        pSpecVMConfig.setType("VirtualMachine");
        pSpecVMConfig.setPathSet(new String[]{"config"});

        PropertySpec pSpecPortgroup = new PropertySpec();
        pSpecPortgroup.setType("HostNetworkSystem");
        pSpecPortgroup.setPathSet(new String[]{"networkInfo.portgroup"});

        PropertyFilterSpec fSpec = new PropertyFilterSpec();
        fSpec.setObjectSet(new ObjectSpec[]{oSpec});
        fSpec.setPropSet(new PropertySpec[]{pSpecVMConfig, pSpecPortgroup});

        PropertyFilterSpec[] pfs = {fSpec};
        HashMap<String, HashMap<String, String>> macVmMap = new HashMap<String, HashMap<String, String>>();

        ObjectContent[] objectContents = methods.retrieveProperties(propColl, pfs);
        for (ObjectContent oc : objectContents) {
            for (DynamicProperty dp : oc.getPropSet()) {
                System.out.println(dp.getName() + "=" + dp.getVal());
                Object val = dp.getVal();
                if (val instanceof VirtualMachineConfigInfo) {
                    VirtualMachineConfigInfo config = (VirtualMachineConfigInfo) val;
                    for (VirtualDevice vd : config.getHardware().getDevice()) {
                        if (vd instanceof VirtualEthernetCard) {
                            String mac = ((VirtualEthernetCard) vd).getMacAddress();
                            if (!macVmMap.containsKey(mac)) {
                                macVmMap.put(mac, new HashMap<String, String>());
                            }
                            macVmMap.get(mac).put("vm", config.getName());
                            macVmMap.get(mac).put("nicName", vd.getDeviceInfo().getLabel());
                        }
                    }
                }
                if (val instanceof ArrayOfHostPortGroup) {
                    HostPortGroup[] portGroups = ((ArrayOfHostPortGroup) val).getHostPortGroup();
                    for (HostPortGroup portGroup : portGroups) {
                        if (portGroup.getPort() != null) {
                            for (HostPortGroupPort port : portGroup.getPort()) {
                                if (macVmMap.containsKey(port.getMac(0))) {
                                    System.out.println(
                                            port.getKey() + ", " +
                                                    macVmMap.get(port.getMac(0)).get("vm") + ", " +
                                                    macVmMap.get(port.getMac(0)).get("nicName")
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String serverName = "example.com";
        String userName = "hoge";
        String password = "fuga";
        String url = "https://" + serverName + "/sdk";

        ManagedObjectReference siRef;
        VimServiceLocator locator;
        VimPortType service;
        ServiceContent siContent;

        System.setProperty("axis.socketSecureFactory", "org.apache.axis.components.net.SunFakeTrustSocketFactory");

        siRef = new ManagedObjectReference();
        siRef.setType("ServiceInstance");
        siRef.set_value("ServiceInstance");

        locator = new VimServiceLocator();
        locator.setMaintainSession(true);
        service = locator.getVimPort(new URL(url));
        siContent = service.retrieveServiceContent(siRef);
        if (siContent.getSessionManager() != null) {
            service.login(siContent.getSessionManager(),
                    userName,
                    password,
                    null);
        }

        collectProperties(service, siContent);

        service.logout(siContent.getSessionManager());
        service = null;
        siContent = null;
    }
}