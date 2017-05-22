package voss.core.server.util;

import org.apache.commons.httpclient.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;

import java.io.IOException;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.RemoteException;
import java.rmi.UnknownHostException;

public class ExceptionUtils {
    private static final Logger log = LoggerFactory.getLogger(ExceptionUtils.class);

    private ExceptionUtils() {
    }

    public static Throwable getRootCause(Throwable th) {
        if (th == null) {
            log.warn("th is null.");
            return null;
        }

        Throwable root = th;
        Throwable current = th;
        while (current != null) {
            root = current;
            current = current.getCause();
        }
        return root;
    }

    public static boolean isLocalProblem(RemoteException e) {
        if (e instanceof ConnectIOException) {
            return true;
        } else if (e instanceof ConnectException) {
            return true;
        } else if (e instanceof UnknownHostException) {
            return true;
        }
        return false;
    }

    public static ExternalServiceException getExternalServiceException(Exception e) {
        if (e instanceof ExternalServiceException) {
            return (ExternalServiceException) e;
        } else if (e instanceof RemoteException) {
            if (isLocalProblem((RemoteException) e)) {
                return new ExternalServiceException("Communication with the server could not be started.", e);
            }
            return new ExternalServiceException("An error occurred on the external server side.", e);
        } else if (e instanceof RuntimeException) {
            return new ExternalServiceException("An unexpected error occurred in the server. [" + e.getMessage() + "]", e);
        } else if (e instanceof HttpException) {
            return new ExternalServiceException("Unexpected error occurred in HTTP communication with external server. [" + e.getMessage() + "]", e);
        } else if (e instanceof ConnectException) {
            return new ExternalServiceException("Could not connect to external server. [" + e.getMessage() + "]", e);
        } else if (e instanceof IOException) {
            return new ExternalServiceException("An unexpected error occurred in the I / O with the external server. [" + e.getMessage() + "]", e);
        }
        return new ExternalServiceException("An unexpected error occurred. Response=[" + e.getMessage() + "]", e);
    }

    public static RuntimeException throwAsRuntime(Exception e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        Exception ex = getExternalServiceException(e);
        throw new RuntimeException(ex);
    }
}