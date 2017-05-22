package voss.mplsnms.rmc;

import java.util.ArrayList;
import java.util.List;

import lib38k.rmc.MethodCall0;
import lib38k.rmc.MethodExec0;
import tef.MVO;
import tef.TransactionContext;
import tef.skelton.SkeltonTefService;
import tef.skelton.dto.EntityDto;
import tef.skelton.dto.MvoDtoFactory;

public class DtoFacade {

    public static class Call extends MethodCall0<List<EntityDto>> {
    }

    public static class Exec extends MethodExec0<Call, List<EntityDto>> {

        @Override public List<EntityDto> execute() {
            TransactionContext.setupRecycleReadTransaction();

            MvoDtoFactory factory = SkeltonTefService.instance().getMvoDtoFactory();

            List<EntityDto> result = new ArrayList<EntityDto>();
            for (MVO mvo : tef.TefService.instance().getMvoRegistry().list()) {
                try {
                    result.add(factory.build(null, mvo));
                } catch (Exception e) {
                }
            }

            return result;
        }
    }
}
