package tef;

import java.util.HashMap;
import java.util.Map;

/**
 * アプリケーション独自オブジェクトをジャーナルに記録/復元するための extension-point です.
 * <p>
 * いずれのメソッドも実装を変更する場合は既存ジャーナルの変換が必要になることに注意してください.
 */
public abstract class ExtraObjectCoder<T> {

    static class Instances {

        private final Map<String, ExtraObjectCoder<?>> extraObjectCodersById_
                = new HashMap<String, ExtraObjectCoder<?>>();
        private final Map<Class<?>, ExtraObjectCoder<?>> extraObjectCodersByType_
                = new HashMap<Class<?>, ExtraObjectCoder<?>>();

        Instances() {
        }

        ExtraObjectCoder<?> getById(String id) {
            ExtraObjectCoder<?> result = extraObjectCodersById_.get(id);
            if (result == null) {
                throw new RuntimeException("no such extra-object-coder: " + id);
            }
            return result;
        }

        ExtraObjectCoder<?> getByType(Class<?> type) {
            ExtraObjectCoder<?> result = extraObjectCodersByType_.get(type);
            if (result == null) {
                throw new RuntimeException("no such extra-object-coder: " + type);
            }
            return result;
        }

        void addExtraObjectCoder(ExtraObjectCoder<?> eoc) {
            String id = eoc.getId();
            if (0 <= id.indexOf(" ")
                    || 0 <= id.indexOf(ExtraObjectCoder.ID_CONTENTS_DELIMITER)) {
                throw new IllegalArgumentException("invalid id.");
            }
            if (extraObjectCodersById_.get(id) != null) {
                throw new IllegalArgumentException
                        ("duplicated extra-object-coder: " + eoc.getId());
            }

            Class<?> type = eoc.getType();
            if (extraObjectCodersByType_.get(type) != null) {
                throw new IllegalArgumentException
                        ("duplicated extra-object-coder: " + eoc.getType());
            }

            extraObjectCodersById_.put(id, eoc);
            extraObjectCodersByType_.put(type, eoc);
        }
    }

    static final char ID_CONTENTS_DELIMITER = ':';

    private final String id_;
    private final Class<T> type_;

    public ExtraObjectCoder(String id, Class<T> type) {
        id_ = id;
        type_ = type;
    }

    public final String getId() {
        return id_;
    }

    public final Class<T> getType() {
        return type_;
    }

    public abstract String encode(T object);

    public abstract T decode(String str);
}
