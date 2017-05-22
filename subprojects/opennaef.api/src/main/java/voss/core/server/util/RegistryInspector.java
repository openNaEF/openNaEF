package voss.core.server.util;

import java.rmi.Naming;
import java.rmi.Remote;

public class RegistryInspector {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("usage: RegistryInspector [registry url]");
            System.exit(-1);
        }

        String registry = args[0];
        try {
            String[] names = Naming.list(registry);
            for (String name : names) {
                Remote remoteObject = Naming.lookup(name);
                if (remoteObject != null) {
                    System.out.println("name[" + name + "] class=" + remoteObject.getClass().getCanonicalName());
                } else {
                    System.out.println("name[" + name + "] is null.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}