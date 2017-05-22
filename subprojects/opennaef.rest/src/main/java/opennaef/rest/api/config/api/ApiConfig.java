package opennaef.rest.api.config.api;

import opennaef.rest.api.helper.BuildHelper;
import tef.skelton.Model;
import tef.skelton.dto.Dto;

import java.util.Collections;
import java.util.Set;

/**
 * Rest APIの設定
 */
public class ApiConfig {
    public final String context;
    public final Set<Class<? extends Model>> mvoClasses;
    public final Set<Class<? extends Dto>> dtoClasses;
    public final BuildHelper helper;
    public final BuilderConfig builderConfig;

    public ApiConfig(
            String context,
            Set<Class<? extends Model>> mvoClasses,
            Set<Class<? extends Dto>> dtoClasses,
            BuildHelper helper,
            BuilderConfig builder) {
        this.context = context;
        this.mvoClasses = Collections.unmodifiableSet(mvoClasses);
        this.dtoClasses = Collections.unmodifiableSet(dtoClasses);
        this.helper = helper;
        this.builderConfig = builder;
    }

    /**
     * 指定した mvo がこのApiConfigで処理すべきものかを判定する
     *
     * @param mvo
     * @return mvo がこのApiConfigで処理できるものであれば true
     */
    @SuppressWarnings("unchecked")
    public boolean isAllowedType(Model mvo) {
        return isAllowedType(mvo, (Set<Class<?>>) (Set<?>) dtoClasses);
    }

    /**
     * 指定した dto がこのApiConfigで処理すべきものかを判定する
     *
     * @param dto
     * @return dto がこのApiConfigで処理できるものであれば true
     */
    @SuppressWarnings("unchecked")
    public boolean isAllowedType(Dto dto) {
        return isAllowedType(dto, (Set<Class<?>>) (Set<?>) dtoClasses);
    }

    private boolean isAllowedType(Object target, Set<Class<?>> allowed) {
        for (Class<?> c : allowed) {
            if (c.isInstance(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return BuilderConfigの設定がある場合にtrue
     */
    public boolean hasBuilderConfig() {
        return builderConfig != null;
    }
}
