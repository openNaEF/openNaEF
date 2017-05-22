package tef.skelton;


/**
 * <p> 被制約側 constraintee が保持する制約 constraintor の集合.
 * <p> Tee2Tor は constrainTEE TO constrainTOR の意味.
 * <p> 「被制約側」は言い換えると: 制約を受ける側,被依存側 / independent側 / lower側
 * <p> 「制約側」は言い換えると: {制約,依存}する側 / dependent側 / upper側
 * <p> 型引数 T は constraintor 制約側, S は constraintee 被制約側.
 */
public class ConstrainTee2TorAttr<T extends TefConnector<?>, S extends TefConnector<?>>
    extends Attribute.SetAttr<T, S>
{
    public ConstrainTee2TorAttr(String name) {
        super(
            name,
            new AttributeType.MvoSetType<T>(TefConnector.class) {

                @Override public T parseElement(String valueStr) {
                    throw new UnsupportedOperationException();
                }
            });
    }

    @Override public void addValue(S constraintee, T constraintor) {
        if (TefConnector.Attr.STATUS.get(constraintee) == TefConnector.Status.NULL) {
            throw new IllegalStateException();
        }
        if (! constraintee.getBounds().isWithin(constraintor.getBounds())) {
            throw new IllegalStateException();
        }

        super.addValue(constraintee, constraintor);
    }

    public T getAsSingle(S constraintee) {
        return SkeltonUtils.asSingle(snapshot(constraintee));
    }
}
