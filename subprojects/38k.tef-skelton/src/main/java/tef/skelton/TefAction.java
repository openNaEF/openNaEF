package tef.skelton;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TefConnector} で表現されるオブジェクト間の関係を変化させる主体.
 * <p>
 * 主要な責務:
 * <ul>
 *  <li>この action が作用する時刻を持つ.<br>
 *      {@link TefAction.Attr#TIME}</li>
 *  <li>変化の客体である {@link TefConnector} を持つ (定義は subtype で行う).</li>
 *  <li>同種の他の action と前後関係 (連鎖) を持つ.<br>
 *      {@link TefAction.Attr#PREDECESSOR}, {@link TefAction.Attr#SUCCESSOR}</li>
 *  <li>{@link TefAction.Status 状態} を持つ.<br>
 *      {@link TefAction.Attr#STATUS}</li>
 *  <li>状態遷移に伴って関連の変化がメンテナンスされる.</li>
 * </ul>
 */
public abstract class TefAction<T extends TefAction<T>>
    extends AbstractModel 
    implements Model 
{
    public static enum Status {

        NULL,

        /**
         * 変更が予定された状態. 設計中状態. 構成が確定した状態.
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

        /**
         * 破棄された状態. status が NULL の状態からの遷移のみが可能.
         * 一度この状態に設定すると他の状態へ遷移することはできない.
         */
        DISPOSED
    }

    static private abstract class StatusTransitionProcessor {

        static final List<StatusTransitionProcessor> instances = new ArrayList<StatusTransitionProcessor>();

        static {
            new StatusTransitionProcessor(Status.NULL, Status.SCHEDULED) { 

                @Override void processTransition(TefAction<?> model) {
                    if (Attr.TIME.get(model) == null) {
                        throw new ConfigurationException(Attr.TIME.getName() + " が設定されていません.");
                    }
                    Double time = Attr.TIME.get(model);
                    for (Attribute<? extends TefConnector<?>, Model> attrMeta : model.getConnectorAttrs()) {
                        TefConnector<?> thisConn = model.get(attrMeta);
                        if (thisConn == null) {
                            continue;
                        }

                        TefConnector<?> lastConn = model.getLastConnector(attrMeta);
                        if (lastConn != null) {
                            TefConnector.Attr.RIGHT_BOUND.set(lastConn, time);
                            lastConn.validateBounds();
                        }

                        TefConnector.Attr.LEFT_BOUND.set(thisConn, time);
                        TefConnector.Attr.RIGHT_BOUND.set(thisConn, TefConnector.POSITIVE_INFINITY);
                        thisConn.validateBounds();
                    }
                }
            };
            new StatusTransitionProcessor(Status.SCHEDULED, Status.NULL) { 

                @Override void processTransition(TefAction<?> model) {
                    for (Attribute<? extends TefConnector<?>, Model> attrMeta : model.getConnectorAttrs()) {
                        TefConnector<?> thisConn = model.get(attrMeta);
                        if (thisConn == null) {
                            continue;
                        }

                        TefConnector.Attr.LEFT_BOUND.set(thisConn, null);
                        TefConnector.Attr.RIGHT_BOUND.set(thisConn, null);
                        TefConnector<?> lastConn = model.getLastConnector(attrMeta);
                        if (lastConn != null) {
                            TefConnector.Attr.RIGHT_BOUND.set(lastConn, TefConnector.POSITIVE_INFINITY);
                        }
                    }
                }
            };
            new StatusTransitionProcessor(Status.SCHEDULED, Status.RUNNING) { 

                @Override void processTransition(TefAction<?> model) {
                    TefAction<?> predecessor = model.getPredecessor();
                    if (predecessor != null) {
                        Status predecessorStatus = Attr.STATUS.get(predecessor);
                        if (predecessorStatus == Status.RUNNING) {
                            Attr.STATUS.set(predecessor, Status.RETIRED);
                        } else if (predecessorStatus != Status.RETIRED) {
                            throw new IllegalStateException("invalid predecessor-action status: " + model.getMvoId());
                        }
                    }
                }
            };
            new StatusTransitionProcessor(Status.RUNNING, Status.SCHEDULED) { 

                @Override void processTransition(TefAction<?> model) {
                    TefAction<?> predecessor = model.getPredecessor();
                    if (predecessor != null) {
                        Status predecessorStatus = Attr.STATUS.get(predecessor);
                        if (predecessorStatus == Status.RETIRED) {
                            Attr.STATUS.set(predecessor, Status.RUNNING);
                        } else if (predecessorStatus != Status.RUNNING) {
                            throw new IllegalStateException("invalid predecessor-action status: " + model.getMvoId());
                        }
                    }
                }
            };
            new StatusTransitionProcessor(Status.RUNNING, Status.RETIRED) { 

                @Override void processTransition(TefAction<?> model) {
                    TefAction<?> successor = model.getSuccessor();
                    if (successor == null) {
                        throw new IllegalStateException(
                            "invalid configuration, no successor found: " + model.getMvoId());
                    }

                    Status successorStatus = Attr.STATUS.get(successor);
                    if (successorStatus == Status.SCHEDULED) {
                        Attr.STATUS.set(successor, Status.RUNNING);
                    } else if (successorStatus != Status.RUNNING) {
                        throw new IllegalStateException("invalid successor-action status: " + model.getMvoId());
                    }
                }
            };
            new StatusTransitionProcessor(Status.RETIRED, Status.RUNNING) { 

                @Override void processTransition(TefAction<?> model) {
                    TefAction<?> successor = model.getSuccessor();
                    if (successor == null) {
                        throw new IllegalStateException(
                            "invalid configuration, no successor found: " + model.getMvoId());
                    }

                    Status successorStatus = Attr.STATUS.get(successor);
                    if (successorStatus == Status.RUNNING) {
                        Attr.STATUS.set(successor, Status.SCHEDULED);
                    } else if (successorStatus != Status.SCHEDULED) {
                        throw new IllegalStateException("invalid successor-action status: " + model.getMvoId());
                    }
                }
            };
            new StatusTransitionProcessor(Status.NULL, Status.DISPOSED) {

                @Override void processTransition(TefAction<?> model) {
                    if (model.getPredecessor() != null || model.getSuccessor() != null) {
                        throw new ConfigurationException("前後関係が設定されています.");
                    }

                    for (Attribute<?, ?> attr : Attribute.getAttributes(model.getClass())) {
                        if (attr instanceof OwnerAttr<?, ?>) {
                            model.set((Attribute<?, Model>) attr, null);
                        }
                    }
                }
            };
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
            synchronized(StatusTransitionProcessor.class) {
                if (getInstance(oldStatus, newStatus) != null) {
                    throw new IllegalArgumentException("duplicated.");
                }

                this.oldStatus = oldStatus;
                this.newStatus = newStatus;

                instances.add(this);
            }
        }

        abstract void processTransition(TefAction<?> model);
    }

    /**
     * action sequence の保持主体を指す.
     * <p>
     * {@link TefAction.Attr#STATUS status} を {@link TefAction.Status#DISPOSED disposed} 
     * に設定するとリセットされる.
     * <p>
     * 設定時動作:
     * <ol>
     *  <li>owner の initial-action-attr が null の場合は initial-action-attr を設定 (逆参照).</li>
     *  <li>owner の last-action を {@link TefAction.Attr#PREDECESSOR} に設定.
     * </ol>
     * <p>
     * リセット時動作:
     * <ol>
     *  <li>owner の initial-action-attr が action だった場合, initial-action-attr をリセットする.
     *      </li>
     * </ol>
     */
    public abstract static class OwnerAttr<T extends AbstractModel, A extends TefAction<?>>
        extends Attribute.SingleModel<T, A>
    {
        public OwnerAttr(String name, Class<T> klass) {
            super(name, klass);
        }

        @Override public void set(A model, T value) {
            if (get(model) == value) {
                return;
            }

            if (Attr.PREDECESSOR.get(model) != null
                || Attr.SUCCESSOR.get(model) != null)
            {
                throw new ConfigurationException("前後関係が設定されています.");
            }

            Actions.InitialActionAttr<A, T> initialactionattr = getOwnerInitialActionAttr();
            if (value != null) {
                A last = Actions.last(initialactionattr, value);
                if (last == null) {
                    initialactionattr.set(value, model);
                }
                Attr.PREDECESSOR.set(model, last);
            } else {
                T owner = get(model);
                if (initialactionattr.get(owner) == model) {
                    initialactionattr.set(owner, null);
                }
            }

            super.set(model, value);
        }

        abstract protected Actions.InitialActionAttr<A, T> getOwnerInitialActionAttr();
    }

    public static class Attr {

        public static final Attribute.SingleEnum<Status, TefAction<?>>
            STATUS = new Attribute.SingleEnum<Status, TefAction<?>>("tef-action.status", Status.class)
        {
            @Override public Status get(TefAction<?> model) {
                Status result = super.get(model);
                return result == null ? Status.NULL : result;
            }

            @Override public void set(TefAction<?> model, Status newValue) {
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

                for (Attribute<? extends TefConnector<?>, Model> attrMeta : model.getConnectorAttrs()) {
                    TefConnector<?> conn = model.get(attrMeta);
                    if (conn == null) {
                        continue;
                    }

                    TefConnector.Status connStatus = convertToConnectorStatus(newValue);
                    TefConnector.Attr.STATUS.set(conn, connStatus);
                }
            }

            private TefConnector.Status convertToConnectorStatus(Status s) {
                return Enum.valueOf(TefConnector.Status.class, s.name());
            }
        };

        public static final Attribute.SingleModel<TefAction<?>, TefAction<?>>
            PREDECESSOR = new Attribute.SingleModel<TefAction<?>, TefAction<?>>(
            "tef-action.predecessor", TefAction.class)
        {
            @Override public void set(TefAction<?> model, TefAction<?> value) {
                if (get(model) == value) {
                    return;
                }

                if (STATUS.get(model) != Status.NULL) {
                    throw new ConfigurationException("構成変更が可能な状態ではありません.");
                }
                if (value != null && model.getClass() != value.getClass()) {
                    throw new ValueException("異種の設定はできません.");
                }
                if (value != null && get(model) != null) {
                    throw new ConfigurationException("既に値が設定されています.");
                }

                TefAction<?> oldValue = get(model);

                super.set(model, value);

                if (oldValue != null) {
                    SUCCESSOR.set(oldValue, null);
                }
                if (value != null) {
                    SUCCESSOR.set(value, model);
                }
            }
        };

        public static final Attribute.SingleModel<TefAction<?>, TefAction<?>>
            SUCCESSOR = new Attribute.SingleModel<TefAction<?>, TefAction<?>>("tef-action.successor", TefAction.class)
        {
            @Override public void set(TefAction<?> model, TefAction<?> value) {
                if (get(model) == value) {
                    return;
                }

                if (value != null) {
                    if (model.getClass() != value.getClass()) {
                        throw new ValueException("異種の設定はできません.");
                    }
                    if (get(model) != null) {
                        throw new ConfigurationException("既に値が設定されています.");
                    }
                    if (TefAction.Attr.STATUS.get(value) != TefAction.Status.NULL) {
                        throw new ValueException("successor の状態が不正です.");
                    }
                }

                TefAction<?> oldValue = get(model);

                super.set(model, value);

                if (oldValue != null) {
                    PREDECESSOR.set(oldValue, null);
                }
                if (value != null) {
                    PREDECESSOR.set(value, model);
                }

                if (value == null) {
                    for (Attribute<? extends TefConnector<?>, Model> attrMeta : model.getConnectorAttrs()) {
                        TefConnector<?> conn = model.get(attrMeta);
                        if (conn == null) {
                            continue;
                        }

                        conn.validateBounds();
                    }
                }
            }
        };

        public static final Attribute.SingleDouble<TefAction<?>>
            TIME = new Attribute.SingleDouble<TefAction<?>>("tef-action.time")
        {
            @Override public void validateValue(TefAction<?> model, Double value) {
                if (STATUS.get(model) != Status.NULL) {
                    throw new ConfigurationException("構成変更が可能な状態ではありません.");
                }

                TefAction<?> predecessor = PREDECESSOR.get(model);
                TefAction<?> successor = SUCCESSOR.get(model);
                Double predecessorTime = predecessor == null ? null : TIME.get(predecessor);
                Double successorTime = successor == null ? null : TIME.get(successor);
                if (value == null) { 
                    if (successorTime != null) {
                        throw new ValueException("後actionの " + getName() + " が設定済のためリセットできません.");
                    }
                } else { 
                    if (predecessor != null) {
                        if (predecessorTime == null) {
                            throw new ConfigurationException("前actionの " + getName() + " が未設定です.");
                        } else {
                            if (value.doubleValue() < predecessorTime.doubleValue()) {
                                throw new ValueException("前actionの " + getName() + " と前後関係が整合しません.");
                            }
                        }
                    }

                    if (successorTime != null) {
                        throw new ConfigurationException("後actionの " + getName() + " が設定済のため設定できません.");
                    }
                }
            }
        };
    }

    public TefAction(MvoId id) {
        super(id);
    }

    public TefAction() {
    }

    public T getPredecessor() {
        return (T) Attr.PREDECESSOR.get(this);
    }

    public T getSuccessor() {
        return (T) Attr.SUCCESSOR.get(this);
    }

    public int getSequenceIndex() {
        T predecessor = getPredecessor();
        return predecessor == null
            ? 0
            : predecessor.getSequenceIndex() + 1;
    }

    private List<Attribute<? extends TefConnector<?>, Model>> getConnectorAttrs() {
        List<Attribute<? extends TefConnector<?>, Model>> result
            = new ArrayList<Attribute<? extends TefConnector<?>, Model>>();
        for (Attribute<?, ?> attrMeta : Attribute.getAttributes(this.getClass())) {
            if (attrMeta.getType() != null
                && attrMeta.getType().getJavaType() != null
                && TefConnector.class.isAssignableFrom(attrMeta.getType().getJavaType()))
            {
                result.add((Attribute<? extends TefConnector<?>, Model>) attrMeta);
            }
        }
        return result;
    }

    /**
     * 指定された attr の connector がなければ見つかるまで predecessor を辿る.
     */
    private TefConnector<?> getLastConnector(Attribute<? extends TefConnector<?>, Model> attr) {
        T predecessor = getPredecessor();
        if (predecessor == null) {
            return null;
        }

        TefConnector<?> connector = predecessor.get(attr);
        return connector == null
                ? ((TefAction<?>) predecessor).getLastConnector(attr)
                : connector;
    }

    public int countConstraintors() {
        int result = 0;
        for (Attribute<? extends TefConnector<?>, Model> attrMeta : getConnectorAttrs()) {
            TefConnector<?> conn = get(attrMeta);
            if (conn == null) {
                continue;
            }

            result += conn.getConstraintors().size();
        }
        return result;
    }

    /**
     * utility class.
     */
    public static class Actions {

        private Actions() {
            throw new RuntimeException();
        }

        public static class InitialActionAttr<A extends TefAction<?>, T extends AbstractModel>
            extends Attribute.SingleModel<A, T>
        {
            public InitialActionAttr(String name, Class<A> klass) {
                super(name, klass);
            }

            @Override public void set(T model, A value) {
                if (value != null) {
                    if (TefAction.Attr.PREDECESSOR.get(value) != null
                        || TefAction.Attr.SUCCESSOR.get(value) != null)
                    {
                        throw new IllegalArgumentException();
                    }

                    if (get(model) != null) {
                        throw new IllegalStateException();
                    }
                }

                super.set(model, value);
            }
        }

        public static <A extends TefAction<?>, T extends AbstractModel> List<A> list(
            InitialActionAttr<A, T> attr,
            T model)
        {
            List<A> result = new ArrayList<A>();
            for (A action = attr.get(model);
                 action != null;
                 action = (A) action.getSuccessor())
            {
                result.add(action);
            }
            return result;
        }

        public static <A extends TefAction<?>, T extends AbstractModel> A last(
            InitialActionAttr<A, T> attr,
            T model)
        {
            for (A action = attr.get(model);
                 action != null;
                 action = (A) action.getSuccessor())
            {
                if (action.getSuccessor() == null) {
                    return action;
                }
            }
            return null;
        }

        public static <A extends TefAction<?>, T extends AbstractModel> A running(
            InitialActionAttr<A, T> attr,
            T model)
        {
            for (A action = last(attr, model);
                 action != null;
                 action = (A) action.getPredecessor())
            {
                if (action.get(TefAction.Attr.STATUS) == TefAction.Status.RUNNING) {
                    return action;
                }
            }
            return null;
        }

        /**
         * 設計中actionのうち最も若いものを返す.
         */
        public static <A extends TefAction<?>, T extends AbstractModel> A firstScheduled(
            InitialActionAttr<A, T> attr,
            T model)
        {
            for (A action = attr.get(model);
                 action != null;
                 action = (A) action.getSuccessor())
            {
                if (action.get(TefAction.Attr.STATUS) == TefAction.Status.SCHEDULED) {
                    return action;
                }
            }
            return null;
        }

        public static <A extends TefAction<?>, T extends AbstractModel> A at(
            InitialActionAttr<A, T> attr,
            T model,
            double time)
        {
            for (A action = last(attr, model);
                 action != null;
                 action = (A) action.getPredecessor())
            {
                Double actionTime = action.get(TefAction.Attr.TIME);
                if (actionTime != null && actionTime <= time) {
                    return action;
                }
            }
            return null;
        }
    }
}
