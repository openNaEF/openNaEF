package opennaef.rest.api.spawner;

/**
 * MvoDtoDesc を JSON に変換する際に、同一階層のものだけを Dto へ変換できるようにするためのThreadLocal変数を保持する
 */
public class ConvertVariable extends ThreadLocal<String> {

    private static ThreadLocal<Vars> holder = new ThreadLocal<Vars>() {
        @Override
        protected Vars initialValue() {
            return new Vars();
        }
    };

    /**
     * true の時は ValueConverter で MvoDtoDesc を変換するときに、mvo-link ではなく Dto を JSON に変換する
     * @return
     */
    public static boolean deref() {
        return holder.get().deref;
    }

    public static void setDeref(boolean deref) {
        Vars vars = holder.get();
        vars.deref = deref;
        holder.set(vars);
    }

    /**
     * Dtoの子要素までDto化してしまうと無限ループするため、Dtoと同一階層のもののみをDtoへ変換する
     * @return
     */
    public static long derefStart() {
        Vars vars = holder.get();
        long depth = vars.depth;
        if (vars.derefDepth == null) {
            vars.derefDepth = depth;
            holder.set(vars);
        }
        return depth;
    }

    /**
     * Dto化する階層を返す
     * Dto化する階層が見つかっていなければ null を返す
     * @return
     */
    public static Long derefDepth() {
        Vars vars = holder.get();
        return vars.derefDepth;
    }

    public static long depthIncrement() {
        Vars vars = holder.get();
        long depth = ++vars.depth;
        holder.set(vars);
        return depth;
    }

    public static long depthDecrement() {
        Vars vars = holder.get();
        long depth = --vars.depth;
        holder.set(vars);
        return depth;
    }

    /**
     * Threadを使いまわすため、変換処理が終わったら必ず初期化する必要がある
     */
    public static void init() {
        holder.set(new Vars());
    }

    private static class Vars {
        long depth = 0;
        boolean deref = false;
        Long derefDepth;
    }
}
