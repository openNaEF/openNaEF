package tef.skelton;

import java.util.HashSet;
import java.util.Set;

import tef.Interval;

/**
 * <p> connectee 側が保持する connector への参照のメタ定義. connectee-&gt;connector.
 * <p> Tee2Tor は connecTEE TO connecTOR の意味.
 */
public class ConnecTee2TorAttr<T extends TefConnector<?>, S extends AbstractModel>
    extends Attribute.SetAttr<T, S>
{
    public ConnecTee2TorAttr(String name, Class<?> klass) {
        super(
            name,
            new AttributeType.MvoSetType<T>(klass) {

                @Override public T parseElement(String valueStr) {
                    throw new UnsupportedOperationException();
                }
            });
    }

    public static class Single<T extends TefConnector<?>, S extends AbstractModel>
        extends ConnecTee2TorAttr<T, S>
    {
        public Single(String name, Class<?> klass) {
            super(name, klass);
        }

        public void validateBounds(S model, Interval bounds) {
            if (bounds == null) {
                throw new IllegalArgumentException();
            }
            for (T other : snapshot(model)) {
                if (! other.getBounds().isDisjoint(bounds)) {
                    throw new IllegalStateException(
                        bounds.leftBound + "-" + bounds.rightBound
                        + " " + model.getMvoId() + " " + other.getMvoId()
                        + " " + other.getBounds().leftBound
                        + "-" + other.getBounds().rightBound);
                }
            }
        }

        @Override public void addValue(S model, T value)
            throws ValueException, ConfigurationException
        {
            validateBounds(model, value.getBounds());

            super.addValue(model, value);
        }

        public T getAt(S model, double time) {
            T result = null;
            for (T connector : snapshot(model)) {
                if (connector.getBounds().isWithin(time)) {
                    if (result == null) {
                        result = connector;
                    } else {
                        throw new IllegalStateException(
                            model.getMvoId() + " " + result.getMvoId() + " " + connector.getMvoId());
                    }
                }
            }
            return result;
        }

        public Set<T> getLaters(S model, double time) {
            Set<T> result = new HashSet<T>();
            for (T connector : snapshot(model)) {
                if (time < connector.getBounds().leftBound) {
                    result.add(connector);
                }
            }
            return result;
        }

        public Set<T> getAtOrLaters(S model, double time) {
            Set<T> result = new HashSet<T>();

            T current = getAt(model, time);
            if (current != null) {
                result.add(current);
            }

            result.addAll(getLaters(model, time));

            return result;
        }
    }
}
