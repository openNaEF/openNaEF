package tef.skelton;

public class IdAttribute
    <R extends Object & Comparable<R>, S extends Model, T extends IdPool.SingleMap<?, R, S>>
    extends Attribute.SingleAttr<R, S>
{
    private final IdPoolAttribute<T, S> idPoolAttribute_;

    public IdAttribute(String name, AttributeType<R> type, IdPoolAttribute<T, S> idPoolAttribute) {
        super(name, type);

        idPoolAttribute_ = idPoolAttribute;
    }

    @Override public void set(S model, R newValue)
        throws ValueException, ConfigurationException
    {
        T pool = idPoolAttribute_.get(model);
        if (pool != null) {
            try {
                if (pool.isAssignedUser(model)) {
                    pool.unassignUser(model);
                }

                if (newValue != null) {
                    pool.assignUser(newValue, model);
                }
            } catch (IdPool.PoolException pe) {
                throw new ConfigurationException(pe.getMessage());
            }
        }

        super.set(model, newValue);
    }
}
