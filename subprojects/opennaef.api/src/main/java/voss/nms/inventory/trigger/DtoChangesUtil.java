package voss.nms.inventory.trigger;

import naef.dto.NaefDto;
import naef.dto.NodeElementDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.dto.DtoChanges;
import tef.skelton.dto.EntityDto;
import tef.skelton.dto.EntityDto.Desc;
import voss.core.server.util.DtoUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DtoChangesUtil {
    private static final Logger log = LoggerFactory.getLogger(DtoChangesUtil.class);

    public static <T extends NaefDto> List<T> getChangedDtos(Class<T> _class, Collection<EntityDto> newObjects,
                                                             Collection<EntityDto> changedObjects) {
        List<T> result = Collections.emptyList();
        if (newObjects != null) {
            for (EntityDto obj : newObjects) {
                if (_class.isInstance(obj)) {
                    result.add(_class.cast(obj));
                }
            }
        }
        if (changedObjects != null) {
            for (EntityDto obj : changedObjects) {
                if (_class.isInstance(obj)) {
                    result.add(_class.cast(obj));
                }
            }
        }
        return result;
    }

    public static <T> T getPreChangeValue(DtoChanges changes, EntityDto target, String attrName, Class<T> _class) {
        if (changes == null || target == null || attrName == null || _class == null) {
            log.debug("arg is null: changes=" + changes + ", target=" + target + ":" + attrName + ", _class=" + _class);
            return null;
        }
        Object o = changes.getPreChangeValue(target, attrName);
        if (o == null) {
            return null;
        }
        if (o instanceof Desc<?>) {
            Desc<?> ref = (Desc<?>) o;
            o = target.toDto(ref);
        }
        if (_class.isInstance(o)) {
            return _class.cast(o);
        } else {
            throw new IllegalStateException("unexpected object type: expected="
                    + _class.getName() + ", but=" + o.getClass().getName());
        }
    }

    public static NodeElementDto getOwner(DtoChanges changes, NodeElementDto target) {
        if (changes == null || target == null) {
            return null;
        }
        NodeElementDto owner = target.getOwner();
        if (owner == null) {
            owner = getPreChangeValue(changes, target, NodeElementDto.ExtAttr.OWNER.getName(), NodeElementDto.class);
        }
        return owner;
    }

    public static Boolean isChanged(DtoChanges changes, EntityDto dto, String attrName) {
        Object pre = changes.getPreChangeValue(dto, attrName);
        if (pre == null) {
            String val = DtoUtil.getStringOrNull(dto, attrName);
            if (val == null) {
                return null;
            } else {
                return Boolean.valueOf(val);
            }
        }
        boolean preStatus = ((Boolean) pre).booleanValue();
        boolean current = DtoUtil.getBoolean(dto, attrName);
        if (preStatus) {
            if (current) {
                return null;
            } else {
                return Boolean.FALSE;
            }
        } else {
            if (current) {
                return Boolean.TRUE;
            } else {
                return null;
            }
        }
    }

}