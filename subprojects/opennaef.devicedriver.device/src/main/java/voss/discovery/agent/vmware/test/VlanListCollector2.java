package voss.discovery.agent.vmware.test;

import com.vmware.vim25.*;

import java.net.URL;

public class VlanListCollector2 {

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

        PropertySpec pSpecPortgroup = new PropertySpec();
        pSpecPortgroup.setType("HostNetworkSystem");
        pSpecPortgroup.setPathSet(new String[]{"networkConfig.portgroup"});

        PropertyFilterSpec fSpec = new PropertyFilterSpec();
        fSpec.setObjectSet(new ObjectSpec[]{oSpec});
        fSpec.setPropSet(new PropertySpec[]{pSpecPortgroup});

        PropertyFilterSpec[] pfs = {fSpec};
        ObjectContent[] objectContents = methods.retrieveProperties(propColl, pfs);
        for (ObjectContent oc : objectContents) {
            for (DynamicProperty dp : oc.getPropSet()) {
                System.out.println(dp.getName() + "=" + dp.getVal());
                Object val = dp.getVal();
                if (val instanceof ArrayOfHostPortGroupConfig) {
                    HostPortGroupConfig[] portGroups = ((ArrayOfHostPortGroupConfig) val).getHostPortGroupConfig();
                    for (HostPortGroupConfig portGroup : portGroups) {
                        System.out.println(portGroup.getSpec().getName() + ", " + portGroup.getSpec().getVlanId());
                    }
                }
            }
        }
    }

    private static void refresh(VimPortType methods, ServiceContent sContent) throws Exception {
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

        PropertySpec pSpecPortgroup = new PropertySpec();
        pSpecPortgroup.setType("HostSystem");
        pSpecPortgroup.setPathSet(new String[]{"configManager.networkSystem"});

        PropertyFilterSpec fSpec = new PropertyFilterSpec();
        fSpec.setObjectSet(new ObjectSpec[]{oSpec});
        fSpec.setPropSet(new PropertySpec[]{pSpecPortgroup});

        PropertyFilterSpec[] pfs = {fSpec};
        ObjectContent[] objectContents = methods.retrieveProperties(propColl, pfs);
        for (ObjectContent oc : objectContents) {
            for (DynamicProperty dp : oc.getPropSet()) {
                System.out.println(dp.getName() + "=" + dp.getVal());
                Object val = dp.getVal();
                if (val instanceof ManagedObjectReference) {
                    methods.refreshNetworkSystem((ManagedObjectReference) val);
                    System.out.println("Refreshed.");
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

        refresh(service, siContent);
        collectProperties(service, siContent);

        service.logout(siContent.getSessionManager());
        service = null;
        siContent = null;
    }
}