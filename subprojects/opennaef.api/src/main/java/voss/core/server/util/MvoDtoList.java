package voss.core.server.util;

import naef.dto.NaefDto;
import tef.MVO;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MvoDtoList<E extends NaefDto> implements List<E> {
    private final List<MVO.MvoId> idList = new ArrayList<MVO.MvoId>();
    private final ConcurrentHashMap<MVO.MvoId, E> map = new ConcurrentHashMap<MVO.MvoId, E>();

    @Override
    public boolean add(E element) {
        if (element == null) {
            return false;
        }
        MVO.MvoId id = DtoUtil.getMvoId(element);
        this.idList.add(id);
        this.map.put(id, element);
        return true;
    }

    @Override
    public void add(int pos, E element) {
        if (element == null) {
            return;
        }
        MVO.MvoId id = DtoUtil.getMvoId(element);
        this.idList.add(pos, id);
        this.map.put(id, element);
    }

    @Override
    public boolean addAll(Collection<? extends E> elements) {
        for (E element : elements) {
            MVO.MvoId id = DtoUtil.getMvoId(element);
            this.idList.add(id);
            this.map.put(id, element);
        }
        return true;
    }

    @Override
    public boolean addAll(int pos, Collection<? extends E> elements) {
        int index = pos;
        for (E element : elements) {
            MVO.MvoId id = DtoUtil.getMvoId(element);
            this.idList.add(index, id);
            this.map.put(id, element);
            index++;
        }
        return true;
    }

    @Override
    public void clear() {
        this.idList.clear();
        this.map.clear();
    }

    @Override
    public boolean contains(Object element) {
        if (!(element instanceof NaefDto)) {
            return false;
        }
        NaefDto dto = (NaefDto) element;
        return idList.contains(DtoUtil.getMvoId(dto));
    }

    @Override
    public boolean containsAll(Collection<?> elements) {
        for (Object element : elements) {
            if (!(element instanceof NaefDto)) {
                return false;
            }
            NaefDto dto = (NaefDto) element;
            if (!idList.contains(DtoUtil.getMvoId(dto))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public E get(int index) {
        MVO.MvoId id = idList.get(index);
        return this.map.get(id);
    }

    @Override
    public int indexOf(Object element) {
        if (!(element instanceof NaefDto)) {
            return -1;
        }
        NaefDto dto = (NaefDto) element;
        return idList.indexOf(DtoUtil.getMvoId(dto));
    }

    @Override
    public boolean isEmpty() {
        return this.idList.isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        List<E> list = new ArrayList<E>();
        for (MVO.MvoId id : idList) {
            E dto = this.map.get(id);
            list.add(dto);
        }
        return list.iterator();
    }

    @Override
    public int lastIndexOf(Object element) {
        if (!(element instanceof NaefDto)) {
            return -1;
        }
        NaefDto dto = (NaefDto) element;
        return idList.lastIndexOf(DtoUtil.getMvoId(dto));
    }

    @Override
    public ListIterator<E> listIterator() {
        List<E> list = new ArrayList<E>();
        for (MVO.MvoId id : idList) {
            E dto = this.map.get(id);
            list.add(dto);
        }
        return list.listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        List<E> list = new ArrayList<E>();
        for (MVO.MvoId id : idList) {
            E dto = this.map.get(id);
            list.add(dto);
        }
        return list.listIterator(index);
    }

    @Override
    public boolean remove(Object element) {
        if (!(element instanceof NaefDto)) {
            return false;
        }
        MVO.MvoId id = DtoUtil.getMvoId(((NaefDto) element));
        boolean result = this.idList.remove(id);
        if (result) {
            this.map.remove(id);
        }
        return result;
    }

    @Override
    public E remove(int index) {
        MVO.MvoId removed = this.idList.remove(index);
        if (removed == null) {
            return null;
        }
        E removedDto = this.map.remove(removed);
        return removedDto;
    }

    @Override
    public boolean removeAll(Collection<?> elements) {
        boolean result = false;
        for (Object element : elements) {
            if (!(element instanceof NaefDto)) {
                continue;
            }
            NaefDto dto = (NaefDto) element;
            if (remove(DtoUtil.getMvoId(dto))) {
                map.remove(DtoUtil.getMvoId(dto));
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        List<MVO.MvoId> anotherList = new ArrayList<MVO.MvoId>();
        Map<MVO.MvoId, NaefDto> anotherMap = new HashMap<MVO.MvoId, NaefDto>();
        for (Object e : c) {
            if (!(e instanceof NaefDto)) {
                continue;
            }
            NaefDto dto = (NaefDto) e;
            anotherList.add(DtoUtil.getMvoId(dto));
            anotherMap.put(DtoUtil.getMvoId(dto), dto);
        }
        boolean changed = this.idList.retainAll(anotherList);
        if (!changed) {
            return false;
        }
        List<MVO.MvoId> removedIDs = new ArrayList<MVO.MvoId>();
        for (E element : this.map.values()) {
            MVO.MvoId id = DtoUtil.getMvoId(element);
            if (!this.idList.contains(id)) {
                removedIDs.add(id);
            }
        }
        for (MVO.MvoId removedID : removedIDs) {
            this.map.remove(removedID);
        }
        return false;
    }

    @Override
    public E set(int index, E element) {
        MVO.MvoId id = DtoUtil.getMvoId(element);
        MVO.MvoId oldID = this.idList.set(index, id);
        this.map.put(id, element);
        return this.map.get(oldID);
    }

    @Override
    public int size() {
        return idList.size();
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        List<E> result = new ArrayList<E>();
        for (MVO.MvoId element : this.idList.subList(fromIndex, toIndex)) {
            E dto = this.map.get(element);
            result.add(dto);
        }
        return result;
    }

    @Override
    public Object[] toArray() {
        List<E> list = new ArrayList<E>();
        for (MVO.MvoId id : idList) {
            E dto = this.map.get(id);
            list.add(dto);
        }
        return list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        List<E> list = new ArrayList<E>();
        for (MVO.MvoId id : idList) {
            E dto = this.map.get(id);
            list.add(dto);
        }
        return list.toArray(array);
    }

}