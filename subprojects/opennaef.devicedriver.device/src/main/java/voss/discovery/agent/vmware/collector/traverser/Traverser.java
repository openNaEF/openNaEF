package voss.discovery.agent.vmware.collector.traverser;

import com.vmware.vim25.*;

import java.rmi.RemoteException;

public interface Traverser {
    ObjectContent[] collectProperty(VimPortType service, ServiceContent siContent) throws InvalidProperty, RuntimeFault, RemoteException;
}