package voss.core.server.util;

import tef.MVO;
import tef.skelton.dto.EntityDto;

import java.util.*;

public class MvoDtoSet<T extends EntityDto> implements Set<T> {
    private final HashMap<MVO.MvoId, T> map = new HashMap<MVO.MvoId, T>();

    @Override
    public boolean add(T element) {
        if (element == null) {
            return false;
        } else if (map.keySet().contains(DtoUtil.getMvoId(element))) {
            return false;
        }
        map.put(DtoUtil.getMvoId(element), element);
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> elements) {
        boolean changed = false;
        for (T element : elements) {
            if (element == null) {
                continue;
            } else if (map.keySet().contains(DtoUtil.getMvoId(element))) {
                continue;
            } else {
                map.put(DtoUtil.getMvoId(element), element);
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public boolean contains(Object object) {
        if (object == null) {
            return false;
        } else if (!(object instanceof EntityDto)) {
            return false;
        }
        EntityDto dto = (EntityDto) object;
        return this.map.keySet().contains(DtoUtil.getMvoId(dto));
    }

    @Override
    public boolean containsAll(Collection<?> objects) {
        for (Object o : objects) {
            if (o == null) {
                return false;
            } else if (!(o instanceof EntityDto)) {
                return false;
            }
            EntityDto dto = (EntityDto) o;
            if (!this.map.keySet().contains(DtoUtil.getMvoId(dto))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isEmpty() {
        return this.map.size() == 0;
    }

    @Override
    public Iterator<T> iterator() {
        return this.map.values().iterator();
    }

    @Override
    public boolean remove(Object element) {
        if (element == null) {
            return false;
        } else if (!(element instanceof EntityDto)) {
            return false;
        }
        EntityDto dto = (EntityDto) element;
        Object removed = this.map.remove(DtoUtil.getMvoId(dto));
        return removed != null;
    }

    @Override
    public boolean removeAll(Collection<?> elements) {
        boolean changed = false;
        for (Object o : elements) {
            if (o == null) {
                continue;
            } else if (!(o instanceof EntityDto)) {
                continue;
            }
            EntityDto dto = (EntityDto) o;
            Object removed = this.map.remove(DtoUtil.getMvoId(dto));
            changed = changed || (removed != null);
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> elements) {
        boolean changed = false;
        Set<MVO.MvoId> targets = new HashSet<MVO.MvoId>();
        for (Object o : elements) {
            if (o == null) {
                continue;
            } else if (!(o instanceof EntityDto)) {
                continue;
            }
            EntityDto dto = (EntityDto) o;
            targets.add(DtoUtil.getMvoId(dto));
        }
        Set<MVO.MvoId> keys = new HashSet<MVO.MvoId>(this.map.keySet());
        for (MVO.MvoId key : keys) {
            if (!targets.contains(key)) {
                Object removed = this.map.remove(key);
                changed = changed || (removed != null);
            }
        }
        return changed;
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public Object[] toArray() {
        return this.map.values().toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] arg0) {
        return this.map.values().toArray(arg0);
    }

}