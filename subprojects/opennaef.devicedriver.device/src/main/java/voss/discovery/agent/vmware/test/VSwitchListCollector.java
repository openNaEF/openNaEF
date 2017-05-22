package voss.discovery.agent.vmware.test;

import com.vmware.vim25.*;

import java.net.URL;

public class VSwitchListCollector {

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

        tSpec.setSelectSet(new TraversalSpec[]{dcToNetwork});

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

        PropertySpec pSpecName = new PropertySpec();
        pSpecName.setType("HostSystem");
        pSpecName.setPathSet(new String[]{"name"});

        PropertySpec pSpecVSwitch = new PropertySpec();
        pSpecVSwitch.setType("HostNetworkSystem");
        pSpecVSwitch.setPathSet(new String[]{"networkConfig.vswitch"});

        PropertyFilterSpec fSpec = new PropertyFilterSpec();
        fSpec.setObjectSet(new ObjectSpec[]{oSpec});
        fSpec.setPropSet(new PropertySpec[]{pSpecName, pSpecVSwitch});

        PropertyFilterSpec[] pfs = {fSpec};
        ObjectContent[] objectContents = methods.retrieveProperties(propColl, pfs);
        for (ObjectContent oc : objectContents) {
            for (DynamicProperty dp : oc.getPropSet()) {
                System.out.println(dp.getName() + "=" + dp.getVal());
                Object val = dp.getVal();
                if (val instanceof ArrayOfHostVirtualSwitchConfig) {
                    HostVirtualSwitchConfig[] vswitches = ((ArrayOfHostVirtualSwitchConfig) val).getHostVirtualSwitchConfig();
                    for (HostVirtualSwitchConfig vswitch : vswitches) {
                        System.out.println(vswitch.getName());
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