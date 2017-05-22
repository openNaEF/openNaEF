package voss.discovery.agent.vmware.collector.traverser;

import com.vmware.vim25.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;

public class HostConfigInfoTraverser implements Traverser {
    private final static Logger log = LoggerFactory.getLogger(HostConfigInfoTraverser.class);

    @Override
    public ObjectContent[] collectProperty(VimPortType service, ServiceContent siContent) throws InvalidProperty, RuntimeFault, RemoteException {
        ManagedObjectReference propColl = siContent.getPropertyCollector();

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(siContent.getRootFolder());
        oSpec.setSkip(true);

        TraversalSpec tSpec = new TraversalSpec();
        tSpec.setType("Folder");
        tSpec.setPath("childEntity");
        tSpec.setSkip(true);

        oSpec.setSelectSet(new TraversalSpec[]{tSpec});

        TraversalSpec dcToNetwork = new TraversalSpec();
        dcToNetwork.setType("Datacenter");
        dcToNetwork.setPath("network");
        dcToNetwork.setSkip(true);

        tSpec.setSelectSet(new TraversalSpec[]{dcToNetwork});

        TraversalSpec networkToHost = new TraversalSpec();
        networkToHost.setType("Network");
        networkToHost.setPath("host");
        networkToHost.setSkip(false);

        dcToNetwork.setSelectSet(new TraversalSpec[]{networkToHost});

        PropertySpec pSpecName = new PropertySpec();
        pSpecName.setType("HostSystem");
        pSpecName.setPathSet(new String[]{"config"});

        PropertyFilterSpec fSpec = new PropertyFilterSpec();
        fSpec.setObjectSet(new ObjectSpec[]{oSpec});
        fSpec.setPropSet(new PropertySpec[]{pSpecName});

        PropertyFilterSpec[] pfs = {fSpec};

        ObjectContent[] props2 = service.retrieveProperties(propColl, pfs);
        return props2;
    }
}