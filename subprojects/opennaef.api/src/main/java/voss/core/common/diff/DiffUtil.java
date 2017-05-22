package voss.core.common.diff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DiffUtil {
    private static final Logger log = LoggerFactory.getLogger(DiffUtil.class);

    public static <T> boolean isAllSame(ValueResolver<T> resolver, Collection<T> bases, Collection<T> targets,
                                        boolean setResolver) {
        List<DiffEntry<T>> diffs = getDiff(resolver, bases, targets, setResolver);
        for (DiffEntry<T> diff : diffs) {
            if (!diff.isSame()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAllSameString(Collection<String> bases, Collection<String> targets) {
        List<DiffEntry<String>> diffs = getStringDiff(bases, targets);
        for (DiffEntry<String> diff : diffs) {
            if (!diff.isSame()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAllSameString(Map<String, String> bases, Map<String, String> targets) {
        List<DiffEntry<String>> diffs = getStringDiff(bases, targets);
        for (DiffEntry<String> diff : diffs) {
            if (!diff.isSame()) {
                return false;
            }
        }
        return true;
    }

    public static List<DiffEntry<String>> getStringDiff(Collection<String> bases, Collection<String> targets) {
        return getDiff(new StringValueResolver(), bases, targets, false);
    }

    public static List<DiffEntry<String>> getStringDiff(Map<String, String> bases, Map<String, String> targets) {
        return getDiff(new StringValueResolver(), bases, targets, false);
    }

    public static <T> List<DiffEntry<T>> getDiff(ValueResolver<T> resolver, Collection<T> bases, Collection<T> targets,
                                                 boolean setResolver) {
        Map<String, DiffEntry<T>> map = toBaseMap(resolver, bases, setResolver);
        for (T target : targets) {
            String key = getKey(resolver, target);
            if (key == null) {
                log.debug("key is null: " + target.toString());
                continue;
            }
            DiffEntry<T> entry = map.get(key);
            if (entry == null) {
                entry = new DiffEntry<T>();
                entry.setKey(key);
                map.put(key, entry);
            }
            entry.setTarget(target);
        }
        List<DiffEntry<T>> result = new ArrayList<DiffEntry<T>>(map.values());
        return result;
    }

    private static <T> Map<String, DiffEntry<T>> toBaseMap(ValueResolver<T> resolver, Collection<T> collection,
                                                           boolean setResolver) {
        Map<String, DiffEntry<T>> map = new HashMap<String, DiffEntry<T>>();
        for (T element : collection) {
            String key = getKey(resolver, element);
            if (key == null) {
                log.debug("key is null: " + element.toString());
                continue;
            }
            DiffEntry<T> entry;
            if (setResolver) {
                entry = new DiffEntry<T>(resolver);
            } else {
                entry = new DiffEntry<T>();
            }
            entry.setKey(key);
            entry.setBase(element);
            map.put(key, entry);
        }
        return map;
    }

    private static <T> String getKey(ValueResolver<T> resolver, T element) {
        if (element == null) {
            return null;
        }
        String key = resolver.getKey(element);
        if (key == null) {
            return null;
        }
        return key;
    }

    public static <T> List<DiffEntry<T>> getDiff(ValueResolver<T> resolver, Map<T, T> bases, Map<T, T> targets,
                                                 boolean setResolver) {
        Map<String, DiffEntry<T>> map = toBaseMap(resolver, bases, setResolver);
        for (Map.Entry<T, T> target : targets.entrySet()) {
            String key = getKey(resolver, target.getKey());
            if (key == null) {
                log.debug("key is null: " + target.toString());
                continue;
            }
            DiffEntry<T> entry = map.get(key);
            if (entry == null) {
                entry = new DiffEntry<T>();
                entry.setKey(key);
                map.put(key, entry);
            }
            entry.setTarget(target.getValue());
        }
        List<DiffEntry<T>> result = new ArrayList<DiffEntry<T>>(map.values());
        return result;
    }

    private static <T> Map<String, DiffEntry<T>> toBaseMap(ValueResolver<T> resolver, Map<T, T> map,
                                                           boolean setResolver) {
        Map<String, DiffEntry<T>> result = new HashMap<String, DiffEntry<T>>();
        for (Map.Entry<T, T> element : map.entrySet()) {
            String key = getKey(resolver, element.getKey());
            if (key == null) {
                log.debug("key is null: " + element.toString());
                continue;
            }
            DiffEntry<T> entry;
            if (setResolver) {
                entry = new DiffEntry<T>(resolver);
            } else {
                entry = new DiffEntry<T>();
            }
            entry.setKey(key);
            entry.setBase(element.getValue());
            result.put(key, entry);
        }
        return result;
    }
}