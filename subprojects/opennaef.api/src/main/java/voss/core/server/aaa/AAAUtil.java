package voss.core.server.aaa;

import voss.core.server.exception.AAAException;
import voss.core.server.exception.ExternalServiceException;

import java.io.IOException;

public class AAAUtil {

    public static String checkAAA(String ipAddress, String operationName)
            throws AAAException, IOException, NotLoggedInException, ExternalServiceException {
        try {
            AAAConfiguration aaa = AAAConfiguration.getInstance();
            if (!aaa.isInitialized()) {
                aaa.reloadConfiguration();
            }
            boolean success = aaa.checkAAA(ipAddress, operationName);
            if (!success) {
                throw new AAAException("Not authorized.");
            }
            AAAUser user = aaa.authenticate(ipAddress);
            return user.getName();
        } catch (AuthorizationException e) {
            throw new AAAException("Rights violations occurred in the engine. Please check server configuration.", e);
        } catch (IOException e) {
            throw new AAAException("I/O exception occurred.", e);
        }
    }
}