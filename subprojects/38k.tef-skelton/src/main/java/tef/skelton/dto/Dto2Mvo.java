package tef.skelton.dto;

import tef.MVO;
import tef.TefService;
import tef.skelton.NamedModel;
import tef.skelton.SkeltonTefService;

public class Dto2Mvo {

    private static MvoDtoDesc<?> toMvoDtoRef(EntityDto.Desc desc) {
        if (! (desc instanceof MvoDtoDesc<?>)) {
            throw new IllegalArgumentException("解決できない記述子です: " + desc);
        }
        return (MvoDtoDesc<?>) desc;
    }

    public static MVO toMvo(EntityDto.Desc<?> desc) {
        MvoDtoDesc<?> ref = toMvoDtoRef(desc);
        return ref.getMvoId() == null 
            ? null 
            : TefService.instance().getMvoRegistry().get(ref.getMvoId());
    }

    public static MVO toMvo(EntityDto.Desc<?> desc, boolean isToCheckVersion) {
        MvoDtoDesc<?> ref = toMvoDtoRef(desc);
        MVO mvo = toMvo(ref);
        if (mvo != null
            && isToCheckVersion
            && ! MvoDtoDesc.computeMvoVersion(mvo).equals(ref.getVersion()))
        {
            throw new DtoOutOfDateException(
                ref,
                SkeltonTefService.instance().uiTypeNames().getName(mvo.getClass()) + " "
                    + (mvo instanceof NamedModel
                        ? ((NamedModel) mvo).getName() + ", "
                        : "")
                    + "mvo-id:" + mvo.getMvoId().getLocalStringExpression()
                    + " は更新されています.");
        }
        return mvo;
    }

    public static MVO toMvo(EntityDto dto) {
        return toMvo(dto.getDescriptor());
    }

    public static MVO toMvo(EntityDto dto, boolean isToCheckVersion) {
        return toMvo(dto.getDescriptor(), isToCheckVersion);
    }
}
