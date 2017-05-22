package tef.skelton;

import tef.Interval;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 時限接続子(timed-connector).
 * <p>
 * 接続関係は {@link Attr#LEFT_BOUND} から {@link Attr#RIGHT_BOUND} の間存在するものとして扱われる.
 * <p>
 * {@link TefAction} の操作対象として定義された場合, 以下は {@link TefAction.Attr#STATUS} 
 * の状態遷移に伴って適切にメンテナンスされるため, 直接設定する必要はない:
 * <ul>
 *  <li>{@link Attr#STATUS}</li>
 *  <li>{@link Attr#LEFT_BOUND}</li>
 *  <li>{@link Attr#RIGHT_BOUND}</li>
 * </ul>
 */
public abstract class TefConnector<T extends TefConnector<T>> extends AbstractModel {

    public static final Double POSITIVE_INFINITY = new Double(Double.POSITIVE_INFINITY);
    public static final Double NEGATIVE_INFINITY = new Double(Double.NEGATIVE_INFINITY);

    public static enum Status {

        /**
         * 未確定状態. 構成の変更はこの状態でないと行えない.
         * <p>
         * {@link Status#SCHEDULED} から遷移したタイミングで以下が解除される:
         * <ul>
         *  <li>connectee (客体) からの逆参照.</li>
         *  <li>constraintee への依存 (制約) の設定.</li>
         * </ul>
         */
        NULL,

        /**
         * 変更が予定された状態. 設計中状態. 構成が確定した状態.
         * <p>
         * {@link Status#NULL} から遷移したタイミングで以下が設定される:
         * <ul>
         *  <li>connectee (客体) からの逆参照.</li>
         *  <li>constraintee への依存 (制約) の設定.</li>
         * </ul>
         */
        SCHEDULED,

        /**
         * 運用中状態.
         */
        RUNNING,

        /**
         * 退役状態. かつて運用中であったもの.
         */
        RETIRED,

        DISPOSED
    }

    static private abstract class StatusTransitionProcessor {

        static final List<StatusTransitionProcessor> instances = new ArrayList<StatusTransitionProcessor>();

        static {
            new StatusTransitionProcessor(Status.NULL, Status.SCHEDULED) { 

                @Override void processTransition(TefConnector<?> model) {
                    model.configure();
                }
            };
            new StatusTransitionProcessor(Status.SCHEDULED, Status.NULL) { 

                @Override void processTransition(TefConnector<?> model) {
                    model.deconfigure();
                }
            };
            new StatusTransitionProcessor(Status.SCHEDULED, Status.RUNNING) { 

                @Override void processTransition(TefConnector<?> model) {
                    validateConstrainteesStatus(model, Status.RUNNING);
                }
            };
            new StatusTransitionProcessor(Status.RUNNING, Status.SCHEDULED) { 

                @Override void processTransition(TefConnector<?> model) {
                }
            };
            new StatusTransitionProcessor(Status.RUNNING, Status.RETIRED) { 

                @Override void processTransition(TefConnector<?> model) {
                }
            };
            new StatusTransitionProcessor(Status.RETIRED, Status.RUNNING) { 

                @Override void processTransition(TefConnector<?> model) {
                    validateConstrainteesStatus(model, Status.RUNNING);
                }
            };
            new StatusTransitionProcessor(Status.NULL, Status.DISPOSED) {

                @Override void processTransition(TefConnector<?> model) {
                    model.deconfigure();
                }
            };
        }

        private static void validateConstrainteesStatus(TefConnector<?> model, Status status) {
            return;
        }

        static StatusTransitionProcessor getInstance(Status oldStatus, Status newStatus) {
            for (StatusTransitionProcessor instance : instances) {
                if (instance.oldStatus == oldStatus && instance.newStatus == newStatus) {
                    return instance;
                }
            }
            return null;
        }

        final Status oldStatus;
        final Status newStatus;

        StatusTransitionProcessor(Status oldStatus, Status newStatus) {
            synchronized (StatusTransitionProcessor.class) {
                if (getInstance(oldStatus, newStatus) != null) {
                    throw new IllegalArgumentException("duplicated.");
                }

                this.oldStatus = oldStatus;
                this.newStatus = newStatus;

                instances.add(this);
            }
        }

        abstract void processTransition(TefConnector<?> model);
    }

    public static class Attr {

        /**
         * 状態.
         */
        public static final Attribute.SingleEnum<Status, TefConnector<?>>
            STATUS = new Attribute.SingleEnum<Status, TefConnector<?>>("tef-connector.status", Status.class)
        {
            @Override public Status get(TefConnector<?> model) {
                Status result = super.get(model);
                return result == null ? Status.NULL : result;
            }

            @Override public void set(TefConnector<?> model, Status newValue) {
                newValue = newValue == null ? Status.NULL : newValue;

                Status oldValue = get(model);
                if (oldValue == newValue) {
                    return;
                }

                StatusTransitionProcessor processor = StatusTransitionProcessor.getInstance(oldValue, newValue);
                if (processor == null) {
                    throw new ValueException("遷移不可です: " + oldValue + " -> " + newValue);
                }

                super.set(model, newValue == Status.NULL ? null : newValue);

                processor.processTransition(model);
            }
        };

        /**
         * この接続子が有効になる時刻. 期始時刻.
         */
        public static final Attribute.SingleDouble<TefConnector<?>>
            LEFT_BOUND = new Attribute.SingleDouble<TefConnector<?>>("tef-connector.left-bound")
        {
            @Override public void set(TefConnector<?> model, Double value) {
                if (value == null ? get(model) == null : value.equals(get(model))) {
                    return;
                }

                super.set(model, value);
            }
        };

        /**
         * この接続子が無効になる時刻. 期末時刻.
         */
        public static final Attribute.SingleDouble<TefConnector<?>>
            RIGHT_BOUND = new Attribute.SingleDouble<TefConnector<?>>("tef-connector.right-bound")
        {
            @Override public void set(TefConnector<?> model, Double value) {
                if (value == null ? get(model) == null : value.equals(get(model))) {
                    return;
                }

                super.set(model, value);
            }
        };
    }

    protected TefConnector(MvoId id) {
        super(id);
    }

    protected TefConnector() {
    }

    public void validateBounds() {
        Double leftBound = Attr.LEFT_BOUND.get(this);
        Double rightBound = Attr.RIGHT_BOUND.get(this);
        if (leftBound == null || rightBound == null) {
            return;
        }

        Interval bounds = buildBounds(leftBound, rightBound);
        for (TefConnector<?> constraintee : getConstraintees()) {
            if (! constraintee.getBounds().isWithin(bounds)) {
                throw new IllegalStateException("out-of-bounds, constraintee: " + constraintee.getMvoId());
            }
        }
        for (TefConnector<?> constraintor : getConstraintors()) {
            if (! bounds.isWithin(constraintor.getBounds())) {
                throw new IllegalStateException("out-of-bounds, constraintor: " + constraintor.getMvoId());
            }
        }
    }

    public Interval getBounds() {
        return buildBounds(Attr.LEFT_BOUND.get(this), Attr.RIGHT_BOUND.get(this));
    }

    private Interval buildBounds(Double leftBound, Double rightBound) {
        if (leftBound == null || rightBound == null) {
            return null;
        }

        return new Interval(leftBound.doubleValue(), true, rightBound.doubleValue(), false);
    }

    public Set<ConnecTor2TeeAttr<?, T>> getConnecTor2TeeAttrs() {
        Set<ConnecTor2TeeAttr<?, T>> result = new HashSet<ConnecTor2TeeAttr<?, T>>();
        for (Attribute<?, ?> attr : Attribute.getAttributes(getClass())) {
            if (attr instanceof ConnecTor2TeeAttr<?, ?>) {
                result.add((ConnecTor2TeeAttr<?, T>) attr);
            }
        }
        return result;
    }

    public Set<ConstrainTor2TeeAttr<?, ?>> getConstrainTor2TeeAttrs() {
        Set<ConstrainTor2TeeAttr<?, ?>> result = new HashSet<ConstrainTor2TeeAttr<?, ?>>();
        for (Attribute<?, ?> attr : Attribute.getAttributes(getClass())) {
            if (attr instanceof ConstrainTor2TeeAttr<?, ?>) {
                result.add((ConstrainTor2TeeAttr<?, ?>) attr);
            }
        }
        return result;
    }

    public Set<ConstrainTee2TorAttr<?, ?>> getConstrainTee2TorAttrs() {
        Set<ConstrainTee2TorAttr<?, ?>> result = new HashSet<ConstrainTee2TorAttr<?, ?>>();
        for (Attribute<?, ?> attr : Attribute.getAttributes(getClass())) {
            if (attr instanceof ConstrainTee2TorAttr<?, ?>) {
                result.add((ConstrainTee2TorAttr<?, ?>) attr);
            }
        }
        return result;
    }

    private void configure() {
        if (getConstraintees().size() > 0) {
            throw new IllegalStateException(); 
        }

        for (ConnecTor2TeeAttr<?, T> connecteeAttr : getConnecTor2TeeAttrs()) {
            AbstractModel connectee = connecteeAttr.get((T) this);
            if (connectee != null) {
                ((ConnecTee2TorAttr<T, AbstractModel>) connecteeAttr.getConnecTee2TorAttr())
                    .addValue(connectee, (T) this);
            }
        }

        configureConstraintees();

        validateBounds();
    }

    protected abstract void configureConstraintees();

    private void deconfigure() {
        if (getConstraintors().size() > 0) {
            throw new IllegalStateException();
        }

        for (ConstrainTor2TeeAttr<?, ?> attr : getConstrainTor2TeeAttrs()) {
            for (TefConnector<?> constraintee : ((ConstrainTor2TeeAttr<?, T>) attr).snapshot((T) this)) {
                ((ConstrainTor2TeeAttr<TefConnector<?>, T>) attr).removeValue((T) this, constraintee);
            }
        }

        for (ConnecTor2TeeAttr<?, T> connecteeAttr : getConnecTor2TeeAttrs()) {
            AbstractModel connectee = connecteeAttr.get((T) this);
            if (connectee != null) {
                ((ConnecTee2TorAttr<T, AbstractModel>) connecteeAttr.getConnecTee2TorAttr())
                    .removeValue(connectee, (T) this);
            }
        }
    }

    public Set<TefConnector<?>> getConstraintees() {
        Set<TefConnector<?>> result = new HashSet<TefConnector<?>>();
        for (ConstrainTor2TeeAttr<?, ?> attr : getConstrainTor2TeeAttrs()) {
            result.addAll(((ConstrainTor2TeeAttr<?, T>) attr).snapshot((T) this));
        }
        return result;
    }

    public Set<TefConnector<?>> getConstrainteesAt(double time) {
        Set<TefConnector<?>> result = new HashSet<TefConnector<?>>();
        for (TefConnector<?> connector : getConstraintees()) {
            if (connector.getBounds().isWithin(time)) {
                result.add(connector);
            }
        }
        return result;
    }

    public Set<TefConnector<?>> getConstraintors() {
        Set<TefConnector<?>> result = new HashSet<TefConnector<?>>();
        for (ConstrainTee2TorAttr<?, ?> attr : getConstrainTee2TorAttrs()) {
            result.addAll(((ConstrainTee2TorAttr<?, T>) attr).snapshot((T) this));
        }
        return result;
    }

    public Set<TefConnector<?>> getConstraintorsAt(double time) {
        Set<TefConnector<?>> result = new HashSet<TefConnector<?>>();
        for (TefConnector<?> connector : getConstraintors()) {
            if (connector.getBounds().isWithin(time)) {
                result.add(connector);
            }
        }
        return result;
    }
}
