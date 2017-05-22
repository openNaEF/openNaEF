package voss.discovery.iolib.snmp;

import net.snmp.Logger;

public class SnmpClientLogger implements Logger {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("SnmpClient");

    @Override
    public void close() {
    }

    @Override
    public void err(String msg, Throwable cause) {
        logger.warn(msg, cause);
    }

    @Override
    public void out(String msg) {
        logger.info(msg);
    }

}