package tef.skelton.dto;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static tef.skelton.dto.EntityDtoUtils.getOid;
import static tef.skelton.dto.EntityDtoUtils.getOids;
import static tef.skelton.dto.EntityDtoUtils.isNullOrEntityDto;

/**
 * 要素の同値性判断を {@link EntityDto.Oid} だけで決定する java.util.Set の実装です.
 * <p>
 * EntityDto.equals() は EntityDto.Desc の同値性に依存するため, 実装によっては Oid 
 * が同値であっても false を返す場合があります. 例えば MVO から生成した EntityDto が持つ 
 * {@link MvoDtoDesc} の場合, mvo-id, version, time の3点が揃わないと同値とみなされません.
 * <p>
 * このため, MVO から生成した EntityDto を要素として持つ java.util.HashSet には Oid 
 * が同一の EntityDto が複数含まれることになります.
 * <p>
 * それに対して OidEntityDtoSet は EntityDto の同値性判断を Oid だけで行うため, 
 * Oid が同一の EntityDto は要素中に最大で一つだけであることが保証されます.
 */
public class OidEntityDtoSet<E extends EntityDto> implements Set<E>, java.io.Serializable {

    private final Map<EntityDto.Oid, E> values_ = new HashMap<EntityDto.Oid, E>();

    public OidEntityDtoSet() {
    }

    public OidEntityDtoSet(Collection<? extends E> c) {
        addAll(c);
    }

    @Override synchronized public int size() {
        return values_.size();
    }

    @Override synchronized public boolean isEmpty() {
        return size() == 0;
    }

    @Override synchronized public boolean contains(Object o) {
        return isNullOrEntityDto(o)
            &&  values_.containsKey(getOid((EntityDto) o));
    }

    @Override synchronized public Iterator<E> iterator() {
        final Iterator<EntityDto.Oid> oidItr = values_.keySet().iterator();

        return new Iterator<E>() {

            @Override synchronized public boolean hasNext() {
                return oidItr.hasNext();
            }

            @Override synchronized public E next() {
                return values_.get(oidItr.next());
            }

            @Override synchronized public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override synchronized public Object[] toArray() {
        return values_.values().toArray();
    }

    @Override synchronized public <T> T[] toArray(T[] a) {
        return values_.values().toArray(a);
    }

    @Override synchronized public boolean add(E e) {
        return values_.put(getOid(e), e) != e;
    }

    @Override synchronized public boolean remove(Object o) {
        return isNullOrEntityDto(o)
            && values_.remove(getOid((EntityDto) o)) != o;
    }

    @Override synchronized public boolean containsAll(Collection<?> c) {
        return values_.keySet().containsAll(getOids(c));
    }

    @Override synchronized public boolean addAll(Collection<? extends E> c) {
        boolean result = false;
        for (E elem : c) {
            boolean b = add(elem);
            result = result || b;
        }
        return result;
    }

    @Override synchronized public boolean retainAll(Collection<?> c) {
        Set<E> removes = new OidEntityDtoSet<E>(this);
        for (Object o : c) {
            if (contains(o)) {
                removes.remove((E) o);
            }
        }

        return removeAll(removes);
    }

    @Override synchronized public boolean removeAll(Collection<?> c) {
        boolean result = false;
        for (Object o : c) {
            boolean b = remove(o);
            result = result || b;
        }
        return result;
    }

    @Override synchronized public void clear() {
        values_.clear();
    }

    /**
     * java.util.Set のAPI仕様に沿った実装です.
     */
    @Override synchronized public boolean equals(Object o) {
        if (o == null || ! (o instanceof Set<?>)) {
            return false;
        }

        Set<?> another = (Set<?>) o;
        try {
            return another.size() == size() && containsAll(another);
        } catch (ClassCastException cce) {
            return false;
        } catch (NullPointerException npe) {
            return false;
        }
    }

    /**
     * java.util.Set のAPI仕様に沿った実装です.
     */
    @Override synchronized public int hashCode() {
        int result = 0;
        for (E e : this) {
            result += e == null ? 0 : e.hashCode();
        }
        return result;
    }
}
