package tef.skelton;

/**
 * <p> connector 側が保持する connectee への参照のメタ定義. connector-&gt;connectee.
 * <p> Tor2Tee は connecTOR TO connecTEE の意味.
 */
public abstract class ConnecTor2TeeAttr<T extends AbstractModel, S extends TefConnector<?>>
    extends Attribute.SingleModel<T, S>
{
    /**
     * 逆参照の多重度が 0..1 のもの.
     */
    public static abstract class Single<T extends AbstractModel, S extends TefConnector<?>>
        extends ConnecTor2TeeAttr<T, S>
    {
        public Single(String name, Class<T> klass) {
            super(name, klass);
        }

        @Override abstract public ConnecTee2TorAttr.Single<S, T> getConnecTee2TorAttr();
    }

    public ConnecTor2TeeAttr(String name, Class<T> klass) {
        super(name, klass);
    }

    @Override public void set(S connector, T connectee) {
        if (TefConnector.Attr.STATUS.get(connector) != TefConnector.Status.NULL) {
            throw new IllegalStateException();
        }

        super.set(connector, connectee);
    }

    abstract public ConnecTee2TorAttr<S, T> getConnecTee2TorAttr();
}
