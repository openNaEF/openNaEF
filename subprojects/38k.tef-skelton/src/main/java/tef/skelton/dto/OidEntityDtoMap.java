package tef.skelton.dto;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static tef.skelton.dto.EntityDtoUtils.getOid;
import static tef.skelton.dto.EntityDtoUtils.isNullOrEntityDto;

/**
 * キーの EntityDto の同値性判断を {@link EntityDto.Oid} だけで決定する java.util.Map
 * の実装です.
 * <p>
 * EntityDto.equals() は EntityDto.Desc の同値性に依存するため, 実装によっては Oid 
 * が同値であっても false を返す場合があります. 例えば MVO から生成した EntityDto が持つ 
 * {@link MvoDtoDesc} の場合, mvo-id, version, time の3点が揃わないと同値とみなされません.
 * <p>
 * このため, MVO から生成した EntityDto をキーとして持つ java.util.HashMap などの通常の
 * java.util.Map の実装では, Oid が同一の EntityDto がキーとなるエントリが複数含まれる
 * ことになります.
 * <p>
 * それに対して OidEntityDtoMap は EntityDto の同値性判断を Oid だけで行うため, Oid が同一の 
 * EntityDto がキーとなるエントリは最大で一つだけであることが保証されます.
 */
public class OidEntityDtoMap<K extends EntityDto, V> implements Map<K, V>, java.io.Serializable {

    private final OidEntityDtoSet<K> keys_ = new OidEntityDtoSet<K>();
    private final Map<EntityDto.Oid, V> values_ = new HashMap<EntityDto.Oid, V>();

    public OidEntityDtoMap() {
    }

    @Override synchronized public int size() {
        return keys_.size();
    }

    @Override synchronized public boolean isEmpty() {
        return size() == 0;
    }

    @Override synchronized public boolean containsKey(Object key) {
        return keys_.contains(key);
    }

    @Override synchronized public boolean containsValue(Object value) {
        return values_.containsValue(value);
    }

    @Override synchronized public V get(Object key) {
        return isNullOrEntityDto(key)
            ? values_.get(getOid((EntityDto) key))
            : null;
    }

    @Override synchronized public V put(K key, V value) {
        keys_.add(key);
        return values_.put(getOid((EntityDto) key), value);
    }

    @Override synchronized public V remove(Object key) {
        if (isNullOrEntityDto(key)) {
            EntityDto dto = (EntityDto) key;
            keys_.remove(dto);
            return values_.remove(getOid((EntityDto) key));
        } else {
            return null;
        }
    }

    @Override synchronized public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override synchronized public void clear() {
        keys_.clear();
        values_.clear();
    }

    @Override synchronized public Set<K> keySet() {
        return new AbstractSet<K>() {

            @Override public int size() {
                return OidEntityDtoMap.this.size();
            }

            @Override public boolean contains(Object o) {
                return OidEntityDtoMap.this.containsKey(o);
            }

            @Override public Iterator<K> iterator() {
                return OidEntityDtoMap.this.keys_.iterator();
            }
        };
    }

    @Override synchronized public Collection<V> values() {
        return new AbstractCollection<V>() {

            @Override public int size() {
                return OidEntityDtoMap.this.size();
            }

            @Override public Iterator<V> iterator() {
                final Iterator<EntityDto.Oid> oidItr = values_.keySet().iterator();

                return new Iterator<V>() {

                    @Override synchronized public boolean hasNext() {
                        return oidItr.hasNext();
                    }

                    @Override synchronized public V next() {
                        return values_.get(oidItr.next());
                    }

                    @Override synchronized public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Override synchronized public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<Map.Entry<K, V>>() {

            @Override public int size() {
                return OidEntityDtoMap.this.size();
            }

            @Override public boolean contains(Object o) {
                if (! (o instanceof Map.Entry)) {
                    return false;
                }

                Map.Entry<K, V> arg = (Map.Entry<K, V>) o;
                K key = arg.getKey();
                V argVal = arg.getValue();
                V thisVal = OidEntityDtoMap.this.get(key);
                return OidEntityDtoMap.this.containsKey(key)
                    && (argVal == null ? thisVal == null : argVal.equals(thisVal));
            }

            @Override public Iterator<Map.Entry<K, V>> iterator() {
                final Iterator<K> keyItr = keys_.iterator();

                return new Iterator<Map.Entry<K, V>>() {

                    @Override synchronized public boolean hasNext() {
                        return keyItr.hasNext();
                    }

                    @Override synchronized public Map.Entry<K, V> next() {
                        K key = keyItr.next();
                        V val = OidEntityDtoMap.this.get(key);
                        return new AbstractMap.SimpleImmutableEntry<K, V>(key, val);
                    }

                    @Override synchronized public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
}
