package tef.skelton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class IdPool<S extends IdPool, T extends Object & Comparable<T>, U> 
    extends AbstractHierarchicalModel<S>
    implements NamedModel
{
    public static class PoolException extends Exception {

        public PoolException(String message) {
            super(message);
        }
    }

    public static abstract
        class SingleMap<S extends SingleMap<S, T, U>, T extends Object & Comparable<T>, U>
        extends IdPool<S, T, U> 
    {
        private final M1<T, U> idToUser_ = new M1<T, U>();
        private final M1<U, T> userToId_ = new M1<U, T>();

        protected SingleMap(MvoId id) {
            super(id);
        }

        protected SingleMap() {
        }

        @Override public boolean isUsedId(T id) {
            return getUser(id) != null;
        }

        @Override public boolean isAssignedUser(U user) {
            return getId(user) != null;
        }

        @Override protected void putUser(T id, U user) {
            if (isUsedId(id)) {
                throw new IllegalArgumentException();
            }
            if (isAssignedUser(user)) {
                throw new IllegalArgumentException();
            }

            idToUser_.put(id, user);
            userToId_.put(user, id);
        }

        @Override protected void removeUser(U user) {
            if (! isAssignedUser(user)) {
                throw new IllegalArgumentException();
            }

            T id = getId(user);
            if (id == null) {
                throw new IllegalStateException();
            }

            idToUser_.remove(id);
            userToId_.remove(user);
        }

        @Override public T getId(U user) {
            return userToId_.get(user);
        }

        public U getUser(T id) {
            return idToUser_.get(id);
        }

        @Override public Set<T> getUsedIds() {
            Set<T> result = new LinkedHashSet<T>();
            for (T id : idToUser_.getKeys()) {
                if (isUsedId(id)) {
                    result.add(id);
                }
            }
            return result;
        }

        public Set<U> getUsers() {
            Set<U> result = new LinkedHashSet<U>();
            for (U user : userToId_.getKeys()) {
                if (isAssignedUser(user)) {
                    result.add(user);
                }
            }
            return result;
        }

        public U getUserByIdString(String str) throws FormatException, PoolException {
            return getUser(parseId(str));
        }
    }

    private final S1<Range<T>> masterRanges_ = new S1<Range<T>>();
    private final M1<Range<T>, S> subRanges_ = new M1<Range<T>, S>();

    protected IdPool(MvoId id) {
        super(id);
    }

    protected IdPool() {
    }

    public String getFqn() {
        return uiTypeName()
            + SkeltonTefService.instance().getFqnPrimaryDelimiter()
            + SkeltonUtils.fqnEscape(getName());
    }

    @Override public void setParent(S parent) {
        if (parent != null) {
            for (Range<T> range : getMasterRanges()) {
                try {
                    parent.checkSubRangeAllocatable(range);
                } catch (PoolException pe) {
                    throw new ConfigurationException("[階層構造変更制約違反] 親子の範囲が適合しません, " + pe.getMessage());
                }
            }
        }

        super.setParent(parent);
    }

    public List<Range<T>> getMasterRanges() {
        List<Range<T>> result = new ArrayList<Range<T>>(masterRanges_.get());
        Collections.<Range<T>>sort(
            result,
            new Comparator<Range<T>>() {

                @Override public int compare(Range<T> o1, Range<T> o2) {
                    int lowerComp = compareT(o1.getLowerBound(), o2.getLowerBound());
                    if (lowerComp != 0) {
                        return lowerComp;
                    }

                    int upperComp = compareT(o1.getUpperBound(), o2.getUpperBound());
                    if (upperComp != 0) {
                        return upperComp;
                    }

                    return 0;
                }

                private int compareT(T o1, T o2) {
                    if (o1 == o2) {
                        return 0;
                    }
                    if (o1 == null && o2 != null) {
                        return -1;
                    }
                    if (o1 != null && o2 == null) {
                        return 1;
                    }
                    return o1.compareTo(o2);
                }
            });
        return result;
    }

    public boolean isInRange(T id) {
        for (Range<T> range : getMasterRanges()) {
            if (range.contains(id, id)) {
                return true;
            }
        }
        return false;
    }

    public List<Range<T>> getSubRanges() {
        List<Range<T>> result = new ArrayList<Range<T>>();
        for (Range<T> range : subRanges_.getKeys()) {
            if (subRanges_.get(range) != null) {
                result.add(range);
            }
        }
        return result;
    }

    public S getAssignedIdPool(Range<T> range) {
        return subRanges_.get(range);
    }

    public void assignUser(T id, U user) throws PoolException {
        if (id == null || user == null) {
            throw new IllegalArgumentException();
        }

        checkRange(id, id);
        checkOverlap(getSubRanges(), id, id);

        if (isUsedId(id)) {
            throw new PoolException("使用されているIDです.");
        }
        if (isAssignedUser(user)) {
            throw new PoolException("割当済ユーザです.");
        }

        putUser(id, user);
    }

    public void unassignUser(U user) throws PoolException {
        if (user == null) {
            throw new IllegalArgumentException();
        }

        if (! isAssignedUser(user)) {
            throw new PoolException("割当されていないユーザです.");
        }

        removeUser(user);
    }

    abstract public boolean isUsedId(T id);
    abstract public boolean isAssignedUser(U user);
    abstract protected void putUser(T id, U user);
    abstract protected void removeUser(U user);
    abstract public T getId(U user);
    abstract public Set<T> getUsedIds();
    abstract public T parseId(String str) throws FormatException;
    abstract public Range<T> parseRange(String str) throws FormatException;

    public void allocateRange(Range<T> newRange) throws PoolException {
        if (getParent() != null) {
            getParent().allocateSubRange(newRange, this);
        }

        checkOverlap(getMasterRanges(), newRange.getLowerBound(), newRange.getUpperBound());

        masterRanges_.add(newRange);
    }

    void allocateSubRange(Range<T> subRange, S subPool) throws PoolException {
        checkSubRangeAllocatable(subRange);

        subRanges_.put(subRange, subPool);
    }

    void checkSubRangeAllocatable(Range<T> subRange) throws PoolException {
        checkUsed(subRange);

        checkOverlap(getSubRanges(), subRange.getLowerBound(), subRange.getUpperBound());

        checkRange(subRange.getLowerBound(), subRange.getUpperBound());
    }

    public void releaseRange(Range<T> existingRange) throws PoolException {
        if (! masterRanges_.contains(existingRange)) {
            throw new PoolException("割当済範囲ではありません.");
        }
        for (T id : getUsedIds()) {
            if (existingRange.contains(id, id)) {
                throw new PoolException("使用中IDがあるため解放できません.");
            }
        }
        for (Range<T> subRange : getSubRanges()) {
            if (existingRange.contains(subRange)) {
                throw new PoolException("払出済範囲があるため解放できません.");
            }
        }

        if (getParent() != null) {
            getParent().releaseSubRange(existingRange, this);
        }

        masterRanges_.remove(existingRange);
    }

    void releaseSubRange(Range<T> subRange, S subPool) {
        if (subRanges_.get(subRange) != subPool) {
            throw new IllegalArgumentException();
        }

        subRanges_.put(subRange, null);
    }

    private void checkUsed(Range<T> subRange) throws PoolException {
        for (T usedId : getUsedIds()) {
            if (subRange.contains(usedId, usedId)) {
                throw new PoolException(subRange + " は既に使用されています.");
            }
        }
    }

    private void checkOverlap(Collection<Range<T>> existingRanges, T testeeLowerBound, T testeeUpperBound)
        throws PoolException
    {
        for (Range<T> existingRange : existingRanges) {
            if (existingRange.isOverlap(testeeLowerBound, testeeUpperBound)) {
                throw new PoolException("既に払出されている " + existingRange.getRangeStr() + " と重複します.");
            }
        }
    }

    private void checkRange(T lowerBound, T upperBound) throws PoolException {
        for (Range<T> masterRange : getMasterRanges()) {
            if (masterRange.contains(lowerBound, upperBound)) {
                return;
            }
        }
        throw new PoolException("払出し範囲外です.");
    }

    public void alterRange(Range<T> existingRange, Range<T> newRange) throws PoolException {
        if (! masterRanges_.contains(existingRange)) {
            throw new PoolException("割当済範囲ではありません.");
        }

        if (existingRange.getLowerBound().equals(newRange.getLowerBound())
            && existingRange.getUpperBound().equals(newRange.getUpperBound()))
        {
            return;
        }

        if (newRange.contains(existingRange)) {
            processExpandRange(existingRange, newRange);
        } else if (existingRange.contains(newRange)) {
            processShrinkRange(existingRange, newRange);
        } else {
            throw new PoolException("変更後の範囲は元の範囲に対する拡張/縮小のどちらかに一意に決定できません.");
        }
    }

    private void processExpandRange(Range<T> existingRange, Range<T> newRange)
        throws PoolException
    {
        if (! newRange.contains(existingRange)) {
            throw new IllegalArgumentException();
        }

        if (getParent() != null) {
            getParent().releaseSubRange(existingRange, this);
        }

        masterRanges_.remove(existingRange);

        allocateRange(newRange);
    }

    private void processShrinkRange(Range<T> existingRange, Range<T> newRange)
        throws PoolException
    {
        if (! existingRange.contains(newRange)) {
            throw new IllegalArgumentException();
        }

        for (T id : getUsedIds()) {
            if (existingRange.contains(id, id) && ! newRange.contains(id, id)) {
                throw new PoolException("使用中IDがあるため縮小できません.");
            }
        }
        for (Range<T> subRange : getSubRanges()) {
            if (existingRange.contains(subRange) && ! newRange.contains(subRange)) {
                throw new PoolException("払出済範囲があるため縮小できません.");
            }
        }

        if (getParent() != null) {
            getParent().releaseSubRange(existingRange, this);
        }

        masterRanges_.remove(existingRange);

        allocateRange(newRange);
    }

    public void mergeRange(Range<T> range1, Range<T> range2) throws PoolException {
        if (! masterRanges_.contains(range1) || ! masterRanges_.contains(range2)) {
            throw new PoolException("割当済範囲を指定してください.");
        }
        if (range1 == range2) {
            return;
        }

        Range<T> newRange = range1.newInstance(
            Range.<T>selectLower(range1.getLowerBound(), range2.getLowerBound()),
            Range.<T>selectUpper(range1.getUpperBound(), range2.getUpperBound()));

        if (getParent() != null) {
            getParent().releaseSubRange(range1, this);
            getParent().releaseSubRange(range2, this);
        }

        masterRanges_.remove(range1);
        masterRanges_.remove(range2);

        allocateRange(newRange);
    }

    public void splitRange(Range<T> existingRange, T splitterLower, T splitterUpper)
        throws PoolException
    {
        if (! masterRanges_.contains(existingRange)) {
            throw new PoolException("割当済範囲を指定してください.");
        }
        if (splitterLower.equals(splitterUpper)) {
            throw new PoolException("分割範囲は開区間として指定してください.");
        }
        if (! existingRange.contains(splitterLower, splitterUpper)) {
            throw new PoolException("分割範囲が対象範囲に包含されません.");
        }

        for (T id : getUsedIds()) {
            if (splitterLower.compareTo(id) < 0 && id.compareTo(splitterUpper) < 0) {
                throw new PoolException("使用中IDがあるため分割できません.");
            }
        }
        for (Range<T> subRange : getSubRanges()) {
            if (! (subRange.getUpperBound().compareTo(splitterLower) <= 0
                || splitterUpper.compareTo(subRange.getLowerBound()) <= 0))
            {
                throw new PoolException("払出済範囲があるため分割できません.");
            }
        }

        if (getParent() != null) {
            getParent().releaseSubRange(existingRange, this);
        }

        masterRanges_.remove(existingRange);

        allocateRange(existingRange.newInstance(existingRange.getLowerBound(), splitterLower));
        allocateRange(existingRange.newInstance(splitterUpper, existingRange.getUpperBound()));
    }
}
