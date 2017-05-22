package voss.discovery.agent.vmware.collector.traverser;

import com.vmware.vim25.*;

import java.rmi.RemoteException;

public class GuestInfoTraverser implements Traverser {

    @Override
    public ObjectContent[] collectProperty(VimPortType service,
                                           ServiceContent siContent) throws InvalidProperty, RuntimeFault,
            RemoteException {
        ManagedObjectReference viewMgrRef = siContent.getViewManager();
        ManagedObjectReference propColl = siContent.getPropertyCollector();

        String[] vm = {"VirtualMachine"};
        ManagedObjectReference cViewRef = service.createContainerView(viewMgrRef, siContent.getRootFolder(), vm, true);

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
        pSpecName.setPathSet(new String[]{"name", "guest"});

        PropertyFilterSpec fSpec = new PropertyFilterSpec();
        fSpec.setObjectSet(new ObjectSpec[]{oSpec});
        fSpec.setPropSet(new PropertySpec[]{pSpecName});

        PropertyFilterSpec[] pfs = {fSpec};

        ObjectContent[] props = service.retrieveProperties(propColl, pfs);
        return props;

    }
}