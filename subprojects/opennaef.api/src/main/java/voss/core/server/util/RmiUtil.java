package voss.core.server.util;

import java.rmi.server.UnicastRemoteObject;

public class RmiUtil {

    public static String getClient() {
        try {
            return UnicastRemoteObject.getClientHost();
        } catch (Exception e) {
            return null;
        }
    }
}