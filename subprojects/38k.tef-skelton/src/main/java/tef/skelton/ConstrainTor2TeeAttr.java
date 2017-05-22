package tef.skelton;

/**
 * <p> 制約側 constraintor が保持する被制約 constraintee の集合.
 * <p> Tor2Tee は constrainTOR TO constrainTEE の意味.
 * <p> 「制約側」は言い換えると: {制約,依存}する側 / dependent側 / upper側
 * <p> 「被制約側」は言い換えると: 制約を受ける側,被依存側 / independent側 / lower側
 * <p> 型引数 T は constraintee 被制約側, S は constraintor 制約側.
 */
public abstract class ConstrainTor2TeeAttr<T extends TefConnector<?>, S extends TefConnector<?>>
    extends Attribute.SetAttr<T, S>
{
    public ConstrainTor2TeeAttr(String name) {
        super(
            name,
            new AttributeType.MvoSetType<T>(TefConnector.class) {

                @Override public T parseElement(String valueStr) {
                    throw new UnsupportedOperationException();
                }
            });
    }

    /**
     * addValue(S,T) 時に同時に設定するindependent側(被依存側/lower側)の DependentAttr.
     * 逆参照.
     */
    abstract public ConstrainTee2TorAttr<S, ?> getConstrainTee2TorAttr();

    @Override public void addValue(S constraintor, T constraintee) {
        super.addValue(constraintor, constraintee);

        ((ConstrainTee2TorAttr<S, T>) getConstrainTee2TorAttr()).addValue(constraintee, constraintor);
    }

    @Override public void removeValue(S constraintor, T constraintee) {
        super.removeValue(constraintor, constraintee);

        ((ConstrainTee2TorAttr<S, T>) getConstrainTee2TorAttr()).removeValue(constraintee, constraintor);
    }

    public T getAsSingle(S constraintor) {
        return SkeltonUtils.asSingle(snapshot(constraintor));
    }
}
