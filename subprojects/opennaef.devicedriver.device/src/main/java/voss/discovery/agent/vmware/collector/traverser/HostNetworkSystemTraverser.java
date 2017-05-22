package voss.discovery.agent.vmware.collector.traverser;

import com.vmware.vim25.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;

public class HostNetworkSystemTraverser implements Traverser {
    private final static Logger log = LoggerFactory.getLogger(HostNetworkSystemTraverser.class);

    @Override
    public ObjectContent[] collectProperty(VimPortType service, ServiceContent siContent) throws InvalidProperty, RuntimeFault, RemoteException {
        ManagedObjectReference propColl = siContent.getPropertyCollector();

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(siContent.getRootFolder());
        oSpec.setSkip(true);

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

        ObjectContent[] objectContents = service.retrieveProperties(propColl, pfs);
        return objectContents;
    }
}