package naef.dto;

import tef.skelton.Attribute;
import tef.skelton.NamedModel;
import tef.skelton.Range;
import tef.skelton.dto.EntityDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public abstract class IdPoolDto<R extends IdPoolDto<R, S, U>, S extends Object & Comparable<S>, U extends EntityDto>
    extends NaefDto
    implements NamedModel
{
    public static abstract class IntegerType<R extends IdPoolDto<R, Integer, U>, U extends EntityDto>
        extends IdPoolDto<R, Integer, U>
    {
        public IntegerType() {
        }

        @Override public IdRange<Integer> newIdRange(Range<?> range) {
            return newIntegerIdRange((Range<Integer>) range);
        }
    }

    public static IdRange<Integer> newIntegerIdRange(Range<Integer> range) {
        return new IdRange.Integer(range.getLowerBound(), range.getUpperBound());
    }

    public static abstract class LongType<R extends IdPoolDto<R, Long, U>, U extends EntityDto>
        extends IdPoolDto<R, Long, U>
    {
        public LongType() {
        }

        @Override public IdRange<Long> newIdRange(Range<?> range) {
            return newLongIdRange((Range<Long>) range);
        }
    }

    public static IdRange<Long> newLongIdRange(Range<Long> range) {
        return new IdRange.Long(range.getLowerBound(), range.getUpperBound());
    }

    public static abstract class StringType<R extends IdPoolDto<R, String, U>, U extends EntityDto>
        extends IdPoolDto<R, String, U>
    {
        public StringType() {
        }

        @Override public IdRange<String> newIdRange(Range<?> range) {
            return newStringIdRange((Range<String>) range);
        }
    }

    public static IdRange<String> newStringIdRange(Range<String> range) {
        return new IdRange.String(range.getLowerBound(), range.getUpperBound());
    }

    public static class ExtAttr {

        public static final SingleRefAttr<IdPoolDto<?, ?, ?>, IdPoolDto<?, ?, ?>> ROOT
            = new SingleRefAttr<IdPoolDto<?, ?, ?>, IdPoolDto<?, ?, ?>>("naef.dto.root");

        public static final SetAttr<IdRange<?>, IdPoolDto<?, ?, ?>> ID_RANGES
            = new SetAttr<IdRange<?>, IdPoolDto<?, ?, ?>>("naef.dto.id-ranges");
        public static final SingleRefAttr<IdPoolDto<?, ?, ?>, IdPoolDto<?, ?, ?>> PARENT
            = new SingleRefAttr<IdPoolDto<?, ?, ?>, IdPoolDto<?, ?, ?>>("naef.dto.parent");
        public static final SetRefAttr<IdPoolDto<?, ?, ?>, IdPoolDto<?, ?, ?>> CHILDREN
            = new SetRefAttr<IdPoolDto<?, ?, ?>, IdPoolDto<?, ?, ?>>("naef.dto.children");
        public static final SetRefAttr<EntityDto, IdPoolDto<?, ?, ?>> USERS
            = new SetRefAttr<EntityDto, IdPoolDto<?, ?, ?>>("naef.dto.users");
    }

    public IdPoolDto() {
    }

    @Override public String getName() {
        return Attribute.NAME.get(this);
    }

    public R getRoot() {
        return (R) ExtAttr.ROOT.deref(this);
    }

    abstract public IdRange<S> newIdRange(Range<?> range);

    abstract public S getId(U dto);

    public Set<IdRange<S>> getIdRanges() {
        Object o = ExtAttr.ID_RANGES.get(this);
        return (Set<IdRange<S>>) o;
    }

    public boolean isInRange(S id) {
        for (IdRange<S> range : getIdRanges()) {
            if (range.lowerBound.compareTo(id) <= 0
                && id.compareTo(range.upperBound) <= 0)
            {
                return true;
            }
        }
        return false;
    }

    public String getConcatenatedIdRangesStr() {
        return NaefDtoUtils.getConcatenatedIdRangesStr(new ArrayList<IdRange<S>>(getIdRanges()));
    }

    public R getParent() {
        return (R) ExtAttr.PARENT.deref(this);
    }

    public Set<R> getChildren() {
        Set<? extends IdPoolDto<?, ?, ?>> result = ExtAttr.CHILDREN.deref(this);
        return (Set<R>) result;
    }

    public SortedSet<U> getUsers() {
        SortedSet<U> result = new TreeSet<U>(new Comparator<U>() {

            @Override public int compare(U o1, U o2) {
                return NaefDtoUtils.compare(getId(o1), getId(o2));
            }
        });
        result.addAll((Set<U>) ExtAttr.USERS.deref(this));
        return result;
    }

    public long getTotalNumberOfIds() {
        return NaefDtoUtils.getTotalNumberOfIds(getIdRanges());
    }
}
