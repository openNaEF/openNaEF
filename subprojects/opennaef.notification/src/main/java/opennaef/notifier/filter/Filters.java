package opennaef.notifier.filter;

import lib38k.rmc.RmcClientService;
import net.arnx.jsonic.JSON;
import opennaef.notifier.config.NotifierConfig;
import opennaef.notifier.util.Logs;
import pasaran.naef.rmc.GetObjectType;
import tef.skelton.dto.DtoChanges;
import tef.skelton.dto.EntityDto;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Filter Utilities
 */
public class Filters {
    private static final Map<String, Class<?>> _objectTypeCache = new ConcurrentHashMap<>();

    /**
     * object-type-name から Dto class を取得する
     * <p>
     * Dto class はキャッシュされる
     *
     * @param typeName object-type-name
     * @return Dto class
     */
    public static Class<?> getObjectType(String typeName) {
        if (_objectTypeCache.containsKey(typeName)) return _objectTypeCache.get(typeName);

        InetSocketAddress rmcAddr;
        try {
            NotifierConfig conf = NotifierConfig.instance();
            rmcAddr = new InetSocketAddress(conf.naefAddr(), conf.naefRmcPort());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        RmcClientService rmc = new RmcClientService.Remote(null, rmcAddr);
        GetObjectType.CallDto call = new GetObjectType.CallDto(typeName);
        Class<?> type = rmc.call(call);
        _objectTypeCache.put(typeName, type);
        return type;
    }

    /**
     * Filter条件を評価する
     * <p>
     * all が指定された場合には exists を評価しない
     *
     * @param dtoChanges DtoChanges
     * @return Filter条件に当てはまる場合に true
     */
    public static boolean matches(FilterQuery filter, DtoChanges dtoChanges) {
        if (filter == null) return true;
        if (filter.getForAll().isEmpty() && filter.getExists().isEmpty()) return true;

        Set<EntityDto> set = new HashSet<>();
        set.addAll(dtoChanges.getNewObjects());
        set.addAll(dtoChanges.getChangedObjects());

        if (!filter.getForAll().isEmpty()) {
            Set<Class<?>> objectTypes = getObjectTypes(filter.getForAll());
            boolean result =  set.parallelStream()
                    .allMatch(dto -> objectTypes.stream().anyMatch(clazz -> clazz.isAssignableFrom(dto.getClass())));
            Logs.filter.debug("all match: {} {}", result, JSON.encode(filter));
            return result;
        }

        if (!filter.getExists().isEmpty()) {
            Set<Class<?>> objectTypes = getObjectTypes(filter.getExists());
            Logs.filter.debug("{}", objectTypes);
            boolean result = set.parallelStream()
                    .anyMatch(dto -> objectTypes.stream().anyMatch(clazz -> clazz.isAssignableFrom(dto.getClass())));
            Logs.filter.debug("exists match: {} {}", result, JSON.encode(filter));
            return result;
        }

        return false;
    }

    private static Set<Class<?>> getObjectTypes(Set<String> typeNames) {
        if (typeNames == null || typeNames.isEmpty()) return Collections.emptySet();
        return typeNames.parallelStream()
                .map(Filters::getObjectType)
                .collect(Collectors.toSet());
    }
}
