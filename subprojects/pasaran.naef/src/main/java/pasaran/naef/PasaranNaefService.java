package pasaran.naef;

import naef.dto.CustomerInfoDto;
import naef.dto.NaefDto;
import naef.mvo.CustomerInfo;
import pasaran.naef.mvo.CustomerInfo2d;
import tef.skelton.Model;
import tef.skelton.dto.DtoAttrTranscript;
import tef.skelton.dto.EntityDto;
import tef.skelton.dto.MvoDtoDesc;
import voss.mplsnms.MplsnmsNaefService;
import voss.mplsnms.MplsnmsRmiServiceAccessPoint;
import voss.mplsnms.MplsnmsTefService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PasaranNaefService extends MplsnmsTefService {
/*
- SampleTefService では installTypes() メソッドを override し, 1. アプリ固有の拡張属性 および 2. MVO から DTO への転写定義 を登録する.
- アプリ固有の拡張属性の登録:
  installAtrribute() で拡張対象とするモデルのクラスと拡張属性が定義されているクラスを指定する.
- MVO から DTO への転写定義の登録:
  installDtoAttrTranscript() で MVO の属性を DTO の属性に転写するロジックを登録.
  2d 関連を保持するために導入したホルダ オブジェクトは DTO には不要であるため
  (ホルダは本来モデルの意味的には不要だがテクニカルな理由でやむなく存在するものであるため),
   MVO~DTO 転写においてはホルダに相当するオブジェクトは省略する.
 */

    @Override
    protected void installTypes() {
        super.installTypes();

        installAttributes(CustomerInfo.class, CustomerInfo2d.CustomerInfoAttr.class);
        installDtoAttrTranscript(
                new DtoAttrTranscript<Set<EntityDto.Desc<NaefDto>>, CustomerInfo>(
                        CustomerInfoDto.class,
                        CustomerInfo2d.CustomerInfoAttr.Dto.REFERENCES_2D,
                        DtoAttrTranscript.EvalStrategy.LAZY) {
                    @Override
                    public Set<EntityDto.Desc<NaefDto>> get(CustomerInfo model) {
                        final CustomerInfo2d.CustomerInfoAttr.Holder holder
                                = CustomerInfo2d.CustomerInfoAttr.Mvo.REFERENCES_2D.get(model);
                        return holder == null
                                ? Collections.emptySet()
                                : MvoDtoDesc.buildS(holder.getReferences());
                    }
                });
        installDtoAttrTranscript(
                new DtoAttrTranscript<Map<Long, List<EntityDto.Desc<NaefDto>>>, CustomerInfo>(
                        CustomerInfoDto.class,
                        CustomerInfo2d.CustomerInfoAttr.Dto.REFERENCES_2D_CHANGES,
                        DtoAttrTranscript.EvalStrategy.LAZY) {
                    @Override
                    public Map<Long, List<EntityDto.Desc<NaefDto>>> get(CustomerInfo model) {
                        final CustomerInfo2d.CustomerInfoAttr.Holder holder
                                = CustomerInfo2d.CustomerInfoAttr.Mvo.REFERENCES_2D.get(model);
                        return holder == null
                                ? Collections.emptyMap()
                                : holder.getReferencesChanges().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> MvoDtoDesc.buildL(entry.getValue())));
                    }
                });


        installAttributes(Model.class, CustomerInfo2d.NetworkModelAttr.class);
        installDtoAttrTranscript(
                new DtoAttrTranscript<Set<EntityDto.Desc<CustomerInfoDto>>, Model>(
                        NaefDto.class,
                        CustomerInfo2d.NetworkModelAttr.Dto.CUSTOMER_INFOS_2D,
                        DtoAttrTranscript.EvalStrategy.LAZY) {
                    @Override
                    public Set<EntityDto.Desc<CustomerInfoDto>> get(Model model) {
                        final CustomerInfo2d.NetworkModelAttr.Holder holder
                                = CustomerInfo2d.NetworkModelAttr.Mvo.CUSTOMER_INFOS_2D.get(model);
                        return holder == null
                                ? Collections.emptySet()
                                : MvoDtoDesc.buildS(holder.getCustomerInfos());
                    }
                });
        installDtoAttrTranscript(
                new DtoAttrTranscript<Map<Long, List<EntityDto.Desc<CustomerInfoDto>>>, Model>(
                        NaefDto.class,
                        CustomerInfo2d.NetworkModelAttr.Dto.CUSTOMER_INFOS_2D_CHANGES,
                        DtoAttrTranscript.EvalStrategy.LAZY) {
                    @Override
                    public Map<Long, List<EntityDto.Desc<CustomerInfoDto>>> get(Model model) {
                        final CustomerInfo2d.NetworkModelAttr.Holder holder
                                = CustomerInfo2d.NetworkModelAttr.Mvo.CUSTOMER_INFOS_2D.get(model);
                        return holder == null
                                ? Collections.emptyMap()
                                : holder.getCustomerInfosChanges().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> MvoDtoDesc.buildL(entry.getValue())));
                    }
                });
    }

    public static void main(String[] args) {
        new PasaranNaefService().start();
    }

    public static MplsnmsRmiServiceAccessPoint getRmiServiceAccessPoint() {
        return MplsnmsNaefService.getRmiServiceAccessPoint();
    }
}
