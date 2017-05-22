package pasaran.naef.rmc;

import lib38k.rmc.MethodCall1;
import lib38k.rmc.MethodExec1;
import tef.skelton.SkeltonTefService;
import tef.skelton.UiTypeName;

/**
 * object-type-name からMVOクラスを解決する
 * <p>
 * TefServiceConfig.xml <rmc-server> セクションへ以下を追加する必要がある
 * <rmc-service call="pasaran.naef.rmc.GetObjectType$CallMvo" exec="pasaran.naef.rmc.GetObjectType$MvoType"/>
 * <rmc-service call="pasaran.naef.rmc.GetObjectType$CallDto" exec="pasaran.naef.rmc.GetObjectType$DtoType"/>
 */
public class GetObjectType {
    public static class MvoType extends MethodExec1<CallMvo, Class<?>, String> {
        @Override
        public Class<?> execute(String objectType) {
            UiTypeName mvoType = SkeltonTefService.instance().uiTypeNames().getByName(objectType);
            return mvoType == null ? null : mvoType.type();
        }
    }

    public static class DtoType extends MethodExec1<CallDto, Class<?>, String> {
        @Override
        public Class<?> execute(String objectType) {
            UiTypeName mvoType = SkeltonTefService.instance().uiTypeNames().getByName(objectType);
            if (mvoType == null) return null;

            return SkeltonTefService.instance().getMvoDtoMapping().getDtoClass(mvoType.type());
        }
    }

    public static class CallMvo extends MethodCall1<Class<?>, String> {
        public CallMvo(String objectType) {
            super(objectType);
        }
    }

    public static class CallDto extends MethodCall1<Class<?>, String> {
        public CallDto(String objectType) {
            super(objectType);
        }
    }
}
