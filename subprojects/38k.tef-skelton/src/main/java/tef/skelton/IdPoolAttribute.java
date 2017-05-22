package tef.skelton;

public class IdPoolAttribute<T extends IdPool.SingleMap<?, ?, S>, S extends Model>
    extends Attribute.SingleModel<T, S>
{
    public IdPoolAttribute(String name, Class<T> type) {
        super(name, type);
    }

    @Override public void set(S model, T newPool) throws ValueException, ConfigurationException {
        T oldPool = get(model);
        if (oldPool == null ? newPool == null : oldPool.equals(newPool)) {
            return;
        }

        if (oldPool != null) {
            try {
                if (oldPool.isAssignedUser(model)) {
                    oldPool.unassignUser(model);
                }
            } catch (IdPool.PoolException pe) {
                throw new ConfigurationException(pe.getMessage());
            }
        }

        super.set(model, newPool);
    }
}
