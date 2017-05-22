package opennaef.rest.api.spawner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DtoSpawner 性能計測
 */
public class Tuning extends ThreadLocal<String> {
    private static final Logger log = LoggerFactory.getLogger(Tuning.class);

    private static ThreadLocal<Vars> holder = new ThreadLocal<Vars>() {
        @Override
        protected Vars initialValue() {
            return new Vars();
        }
    };

    public static void facade(long nano) {
        Vars vars = holder.get();
        vars.total += nano;
        vars.facade += nano;
    }

    public static void originator(long nano) {
        Vars vars = holder.get();
        vars.total += nano;
        vars.originator += nano;
    }

    public static void dto(long nano) {
        Vars vars = holder.get();
        vars.total += nano;
        vars.dto += nano;
    }

    public static void dump() {
        Vars vars = holder.get();
        log.info("total: " + vars.total / 1000000f + "ms.");
        log.debug("facade: " + vars.facade / 1000000f + "ms.");
        log.debug("originator: " + vars.originator / 1000000f + "ms.");
        log.debug("dto: " + vars.dto / 1000000f + "ms.");
        holder.set(new Vars());
    }

    /**
     * Threadを使いまわすため、変換処理が終わったら必ず初期化する必要がある
     */
    public static void init() {
        holder.set(new Vars());
    }

    private static class Vars {
        long total = 0;
        long facade = 0;
        long originator = 0;
        long dto = 0;
    }
}
