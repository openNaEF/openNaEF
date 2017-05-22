package voss.multilayernms.inventory.lock;

import naef.dto.NaefDto;
import voss.core.server.util.DtoUtil;
import voss.nms.inventory.util.NameUtil;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LockManager {
    private static LockManager instance = null;

    public synchronized static LockManager getInstance() {
        if (instance == null) {
            instance = new LockManager();
        }
        return instance;
    }

    private final Map<String, LockedElement> lockedObjects = new ConcurrentHashMap<String, LockedElement>();

    private LockManager() {
    }

    public synchronized boolean isLocked(NaefDto target) {
        if (target == null) {
            return false;
        }
        return getLockUser(target) != null;
    }

    public synchronized String getLockUser(NaefDto target) {
        if (target == null) {
            return null;
        }
        LockedElement le = this.lockedObjects.get(DtoUtil.getMvoId(target).toString());
        return le.editorName;
    }

    public synchronized String getLockUser(String id) {
        if (id == null) {
            return null;
        }
        LockedElement le = this.lockedObjects.get(id);
        if (le == null) {
            return null;
        }
        return le.editorName;
    }

    public synchronized void lock(NaefDto target, String lockUser) throws LockException {
        if (target == null) {
            return;
        }
        lock(DtoUtil.getMvoId(target).toString(), NameUtil.getCaption(target), lockUser);
    }

    public synchronized void lock(String id, String caption, String lockUser) throws LockException {
        String currentLockUser = getLockUser(id);
        if ((currentLockUser != null && !currentLockUser.equals(lockUser))) {
            throw new LockException("target is already locked by "
                    + currentLockUser + ", [" + id.toString() + "]=[" + caption + "]");
        }
        LockedElement le = new LockedElement(lockUser, caption, id);
        this.lockedObjects.put(id, le);
    }

    public synchronized void unlock(NaefDto target) {
        if (target == null) {
            return;
        }
        this.lockedObjects.remove(DtoUtil.getMvoId(target).toString());
    }

    public synchronized void unlock(String target) {
        if (target == null) {
            return;
        }
        this.lockedObjects.remove(target);
    }

    public synchronized List<LockedElement> listLocks() {
        List<LockedElement> lists = new ArrayList<LockedElement>(this.lockedObjects.values());
        Collections.sort(lists, new Comparator<LockedElement>() {
            @Override
            public int compare(LockedElement o1, LockedElement o2) {
                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o2 == null) {
                    return -1;
                } else if (o1 == null) {
                    return 1;
                }
                return o1.getLockedDate().compareTo(o2.getLockedDate());
            }
        });
        return lists;
    }

    public static class LockedElement implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Date lockedDate;
        private final String caption;
        private final String editorName;
        private final String target;

        public LockedElement(String editorName, String caption, String id) {
            this.lockedDate = new Date();
            this.editorName = editorName;
            this.caption = caption;
            this.target = id;
        }

        public String getEditorName() {
            return this.editorName;
        }

        public String getCaption() {
            return this.caption;
        }

        public Date getLockedDate() {
            return this.lockedDate;
        }

        public String getTarget() {
            return this.target;
        }

        @Override
        public int hashCode() {
            return this.target.hashCode() + 1023;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            } else if (o == this) {
                return true;
            }
            return this.target.equals(((LockedElement) o).target);
        }
    }
}