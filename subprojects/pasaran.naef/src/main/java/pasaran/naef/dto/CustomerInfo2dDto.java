package pasaran.naef.dto;

import naef.dto.CustomerInfoDto;
import naef.dto.NaefDto;
import pasaran.naef.mvo.CustomerInfo2d;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 時間軸・バージョン軸をもつreference属性を追加したCustomerInfoDto.
 * <p>
 * config/MvoDtoMapping.xml へ Dto のマッピング定義を追加.
 * <map mvo="naef.mvo.CustomerInfo" dto="pasaran.naef.dto.CustomerInfo2dDto"/>
 */
public class CustomerInfo2dDto extends CustomerInfoDto {
    public CustomerInfo2dDto() {
    }

    /**
     * 時間軸・バージョン軸を持つreference.
     * ExtAttr.REFERENCES とは関係のない属性である.
     *
     * @return references
     */
    public Set<NaefDto> getReferences2d() {
        return CustomerInfo2d.CustomerInfoAttr.Dto.REFERENCES_2D.deref(this);
    }

    /**
     * 時間軸・バージョン軸を持つreference の時間変化の Map を返す
     * @return 変化が起きる時間とその時間における値の Map
     */
    public Map<Long, List<Desc<NaefDto>>> getReferences2dChanges() {
        return CustomerInfo2d.CustomerInfoAttr.Dto.REFERENCES_2D_CHANGES.get(this);
    }

    /**
     * 時間軸・バージョン軸を持たないreference.
     * REFERENCES_2D とは関係のない属性である.
     *
     * @return references
     */
    @Override
    @Deprecated
    public Set<NaefDto> getReferences() {
        return super.getReferences();
    }
}
