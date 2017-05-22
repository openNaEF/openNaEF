package voss.discovery.agent.vmware.test;

import com.vmware.vim25.*;

import java.net.URL;

public class VNic_IpAddr_MacAddr_PortGroupListCollector {

    private static void collectProperties(VimPortType methods, ServiceContent sContent) throws Exception {
        ManagedObjectReference viewMgrRef = sContent.getViewManager();
        ManagedObjectReference propColl = sContent.getPropertyCollector();

        String[] vm = {"VirtualMachine"};
        ManagedObjectReference cViewRef = methods.createContainerView(viewMgrRef, sContent.getRootFolder(), vm, true);

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(cViewRef);
        oSpec.setSkip(true);

        TraversalSpec tSpec = new TraversalSpec();
        tSpec.setName("traverseEntities");
        tSpec.setPath("view");
        tSpec.setSkip(false);
        tSpec.setType("ContainerView");

        oSpec.setSelectSet(new TraversalSpec[]{tSpec});

        PropertySpec pSpecName = new PropertySpec();
        pSpecName.setType("VirtualMachine");
        pSpecName.setPathSet(new String[]{"name", "guest.net"});

        PropertyFilterSpec fSpec = new PropertyFilterSpec();
        fSpec.setObjectSet(new ObjectSpec[]{oSpec});
        fSpec.setPropSet(new PropertySpec[]{pSpecName});

        PropertyFilterSpec[] pfs = {fSpec};
        ObjectContent[] objectContents = methods.retrieveProperties(propColl, pfs);

        for (ObjectContent oc : objectContents) {
            for (DynamicProperty dp : oc.getPropSet()) {
                Object val = dp.getVal();

                if (val instanceof ArrayOfGuestNicInfo) {

                    GuestNicInfo[] guestNicInfos = ((ArrayOfGuestNicInfo) val).getGuestNicInfo();

                    if (guestNicInfos != null) {
                        for (GuestNicInfo guestNicInfo : guestNicInfos) {
                            int vNicDeviceId = ((GuestNicInfo) guestNicInfo).getDeviceConfigId();
                            String macAddr = ((GuestNicInfo) guestNicInfo).getMacAddress();
                            String[] ipAddrs = ((GuestNicInfo) guestNicInfo).getIpAddress();
                            String portGroupName = ((GuestNicInfo) guestNicInfo).getNetwork();
                            if (ipAddrs != null) {
                                for (String ip : ipAddrs) {
                                    System.out.println(" ipAddrs        = " + ip);
                                }
                            }
                            System.out.println(" portGroupName = " + portGroupName);
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