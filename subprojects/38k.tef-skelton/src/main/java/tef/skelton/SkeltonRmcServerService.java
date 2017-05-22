package tef.skelton;

import lib38k.logger.Logger;
import lib38k.rmc.MethodCall;
import lib38k.rmc.RmcServerService;
import tef.TransactionContext;
import tef.skelton.dto.EntityDto;

class SkeltonRmcServerService extends RmcServerService {

    SkeltonRmcServerService(int port, Logger logger) {
        super(port, logger);
    }

    @Override protected String renderArg(Object arg) {
        if (arg instanceof EntityDto) {
            EntityDto dto = (EntityDto) arg;
            return dto.getDescriptor().toString();
        } else {
            return super.renderArg(arg);
        }
    }

    @Override protected Object executeMethod(MethodCall<?> methodcall) {
        try {
            return super.executeMethod(methodcall);
        } finally {
            TransactionContext.close();
        }
    }
}
