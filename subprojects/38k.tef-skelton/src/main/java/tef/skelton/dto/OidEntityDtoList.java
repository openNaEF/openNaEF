package tef.skelton.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import static tef.skelton.dto.EntityDtoUtils.getOid;
import static tef.skelton.dto.EntityDtoUtils.getOids;
import static tef.skelton.dto.EntityDtoUtils.getOidsSet;
import static tef.skelton.dto.EntityDtoUtils.isNullOrEntityDto;

/**
 * 要素の同値性判断を {@link EntityDto.Oid} だけで決定する java.util.List の実装です.
 * <p>
 * EntityDto.equals() は EntityDto.Desc の同値性に依存するため, 実装によっては Oid 
 * が同値であっても false を返す場合があります. 例えば MVO から生成した EntityDto が持つ 
 * {@link MvoDtoDesc} の場合, mvo-id, version, time の3点が揃わないと同値とみなされません.
 * <p>
 * このため, MVO から生成した EntityDto を要素として持つ java.util.ArrayList などの通常の
 * java.util.List の実装では Oid のみで同値とみなしたい場合は remove, containsAll, 
 * removeAll, retainAll, equals, indexOf, lastIndexOf が期待する動作をしません.
 * <p>
 * それに対して OidEntityDtoList は EntityDto の同値性判断を Oid だけで行います.
 */
public class OidEntityDtoList<E extends EntityDto> implements List<E>, java.io.Serializable {

    private final List<E> list_ = new ArrayList<E>();

    public OidEntityDtoList() {
    }

    public OidEntityDtoList(Collection<? extends E> c) {
        addAll(c);
    }

    @Override public synchronized int size() {
        return list_.size();
    }

    @Override public synchronized boolean isEmpty() {
        return list_.isEmpty();
    }

    @Override public synchronized boolean contains(Object o) {
        return 0 <= indexOf(o);
    }

    @Override public synchronized Iterator<E> iterator() {
        return list_.iterator();
    }

    @Override public synchronized Object[] toArray() {
        return list_.toArray();
    }

    @Override public synchronized <T> T[] toArray(T[] a) {
        return list_.toArray(a);
    }

    @Override public synchronized boolean add(E e) {
        return list_.add(e);
    }

    @Override public synchronized boolean remove(Object o) {
        int index = indexOf(o);
        if (index < 0) {
            return false;
        } else {
            list_.remove(index);
            return true;
        }
    }

    @Override public synchronized boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (! contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override public synchronized boolean addAll(Collection<? extends E> c) {
        return list_.addAll(c);
    }

    @Override public synchronized boolean addAll(int index, Collection<? extends E> c) {
        return list_.addAll(index, c);
    }

    @Override public synchronized boolean removeAll(Collection<?> c) {
        boolean modified = false;
        Set<EntityDto.Oid> oidsToRemove = getOidsSet(c);
        Iterator<E> itr = list_.iterator();
        while (itr.hasNext()) {
            if (oidsToRemove.contains(getOid(itr.next()))) {
                itr.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override public synchronized boolean retainAll(Collection<?> c) {
        boolean modified = false;
        Set<EntityDto.Oid> oidsToRetain = getOidsSet(c);
        Iterator<E> itr = list_.iterator();
        while (itr.hasNext()) {
            if (! oidsToRetain.contains(getOid(itr.next()))) {
                itr.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override public synchronized void clear() {
        list_.clear();
    }

    @Override public synchronized boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (! (o instanceof OidEntityDtoList)) {
            return false;
        }

        OidEntityDtoList another = (OidEntityDtoList) o;
        if (this.size() != another.size()) {
            return false;
        }

        for (int i = 0; i < size(); i++) {
            if (! oidEquals(getOid(this.get(i)), getOid(another.get(i)))) {
                return false;
            }
        }
        return true;
    }

    /**
     * java.util.List のAPI仕様で規定された実装です.
     */
    @Override public synchronized int hashCode() {
        int hashCode = 1;
        for (E e : list_) {
            EntityDto.Oid oid = getOid(e);
            hashCode = 31 * hashCode + (oid == null ? 0 : oid.hashCode());
        }
        return hashCode;
    }

    @Override public synchronized E get(int index) {
        return list_.get(index);
    }

    @Override public synchronized E set(int index, E element) {
        return list_.set(index, element);
    }

    @Override public synchronized void add(int index, E element) {
        list_.add(index, element);
    }

    @Override public synchronized E remove(int index) {
        return list_.remove(index);
    }

    @Override public synchronized int indexOf(Object o) {
        return isNullOrEntityDto(o)
            ? getFirstIndexByOid(getOid((EntityDto) o))
            : -1;
    }

    @Override public synchronized int lastIndexOf(Object o) {
        return isNullOrEntityDto(o)
            ? getLastIndexByOid(getOid((EntityDto) o))
            : -1;
    }

    @Override public synchronized ListIterator<E> listIterator() {
        return list_.listIterator();
    }

    @Override public synchronized ListIterator<E> listIterator(int index) {
        return list_.listIterator(index);
    }

    @Override public synchronized List<E> subList(int fromIndex, int toIndex) {
        return list_.subList(fromIndex, toIndex);
    }

    private synchronized int getFirstIndexByOid(EntityDto.Oid oid) {
        for (int i = 0; i < list_.size(); i++) {
            if (oidEquals(oid, getOid(list_.get(i)))) {
                return i;
            }
        }
        return -1;
    }

    private synchronized int getLastIndexByOid(EntityDto.Oid oid) {
        for (int i = list_.size() - 1; 0 <= i; i--) {
            if (oidEquals(oid, getOid(list_.get(i)))) {
                return i;
            }
        }
        return -1;
    }

    private boolean oidEquals(EntityDto.Oid oid1, EntityDto.Oid oid2) {
        return oid1 == null ? oid2 == null : oid1.equals(oid2);
    }
}
