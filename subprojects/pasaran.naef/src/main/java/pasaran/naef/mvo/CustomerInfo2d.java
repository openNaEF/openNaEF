package pasaran.naef.mvo;

import naef.dto.CustomerInfoDto;
import naef.dto.NaefDto;
import naef.mvo.CustomerInfo;
import pasaran.naef.dto.CustomerInfo2dDto;
import tef.skelton.AbstractModel;
import tef.skelton.Attribute;
import tef.skelton.AttributeType.ModelType;
import tef.skelton.Model;
import tef.skelton.dto.EntityDto;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * モデル一式をまとめて定義したファイル.
 * <p>
 * - NAEF での CustomerInfo とネットワーク モデル オブジェクトの間の関連は予定時間を持たないいわゆる 1d 関連として定義されているが,
 * これを予定時間を管理できる 2d 関連として扱いたいという要望がある. 既存の 1d 実装はそのまま残しておき,
 * 新たに 2d 関連をアプリ拡張としてサンプル実装する.
 * - TEF には extension point が用意されているので,
 * それを用いることで既存の NAEF オブジェクト自体を extends せずにアプリ独自の属性を定義することができる.
 * ただし, 素の状態では予定時間軸に非対応であるため, 予定時間軸を保持するホルダ オブジェクトを別途定義する.
 * # 予定時間軸を持たない 1d 関連を追加する場合は標準機能だけで, 属性の宣言だけで拡張が可能.
 * ホルダやシェル コマンドを新たに定義する必要は無い.
 * - また, 追加したモデル要素を操作するシェル コマンドも定義する.
 * <p>
 * - TefShellConfig.xml に CustomerInfo2dCommand のマッピング定義を追加.
 * <tef-shell-plugins-config> セクションに <plug-in name="customerinfo-2d" class="sample.CustomerInfo2dCommand" /> というエントリを加える.
 */
public class CustomerInfo2d {

    /**
     * CustomerInfo 側の拡張定義一式をまとめたもの.
     */
    public static class CustomerInfoAttr {

        /**
         * 予定時間軸関連 S2 フィールドを保持するホルダ オブジェクトの定義. 永続化オブジェクト.
         * S2 の REFERENCES 属性は関連の対向ネットワーク モデル オブジェクトへの参照を保持する. その getter/setter も定義.
         */
        public static class Holder extends AbstractModel {

            private final S2<Model> REFERENCES = new S2<>();

            public Holder(MvoId id) {
                super(id);
            }

            public Holder() {
            }

            public void addReference(Model obj) {
                REFERENCES.add(obj);
            }

            public void removeReference(Model obj) {
                REFERENCES.remove(obj);
            }

            public Set<Model> getReferences() {
                return new HashSet<>(REFERENCES.get());
            }

            /**
             * REFERENCES の時間変化の Map を返す
             * @return 変化が起きる時間とその時間における値の Map
             */
            public Map<Long, List<Model>> getReferencesChanges() {
                return REFERENCES.getChanges();
            }
        }

        /**
         * MVO 側の拡張属性メタ定義. naef.mvo.CustomerInfo を拡張する.
         */
        public static class Mvo {

            public static final Attribute.SingleAttr<Holder, CustomerInfo> REFERENCES_2D
                    = new Attribute.SingleAttr<>("REFERENCES_2D", new ModelType<>(Holder.class));
        }

        /**
         * DTO 側の拡張属性メタ定義. naef.dto.CustomerInfoDto を拡張する.
         */
        public static class Dto {

            public static final EntityDto.SetRefAttr<NaefDto, CustomerInfoDto> REFERENCES_2D
                    = new EntityDto.SetRefAttr<NaefDto, CustomerInfoDto>("REFERENCES_2D");

            public static final EntityDto.MapAttr<Long, List<EntityDto.Desc<NaefDto>>, CustomerInfoDto> REFERENCES_2D_CHANGES
                    = new EntityDto.MapAttr<Long, List<EntityDto.Desc<NaefDto>>, CustomerInfoDto>("REFERENCES_2D_CHANGES");
        }
    }

    /**
     * ネットワーク モデル側の拡張定義一式をまとめたもの.
     */
    public static class NetworkModelAttr {

        /**
         * 予定時間軸関連 S2 フィールドを保持するホルダ オブジェクトの定義. 永続化オブジェクト.
         * S2 の CUSTOMER_INFOS_2D 属性は関連の対向 CustomerInfo への参照を保持する. その getter/setter も定義.
         */
        public static class Holder extends AbstractModel {

            private final S2<CustomerInfo> CUSTOMER_INFOS = new S2<>();

            public Holder(MvoId id) {
                super(id);
            }

            public Holder() {
            }

            public void addCustomerInfo(CustomerInfo customerinfo) {
                CUSTOMER_INFOS.add(customerinfo);
            }

            public void removeCustomerInfo(CustomerInfo customerinfo) {
                CUSTOMER_INFOS.remove(customerinfo);
            }

            public Set<CustomerInfo> getCustomerInfos() {
                return new HashSet<>(CUSTOMER_INFOS.get());
            }

            /**
             * CUSTOMER_INFOS の時間変化の Map を返す
             * @return 変化が起きる時間とその時間における値の Map
             */
            public Map<Long, List<CustomerInfo>> getCustomerInfosChanges() {
                return CUSTOMER_INFOS.getChanges();
            }
        }

        /**
         * MVO 側の拡張属性メタ定義. tef.skelton.Model を拡張する.
         */
        public static class Mvo {

            public static final Attribute.SingleAttr<Holder, Model> CUSTOMER_INFOS_2D
                    = new Attribute.SingleAttr<Holder, Model>("CUSTOMER_INFOS_2D", new ModelType<>(Holder.class));
        }

        /**
         * DTO 側の拡張属性メタ定義. naef.dto.NaefDto を拡張する.
         */
        public static class Dto {

            public static final EntityDto.SetRefAttr<CustomerInfoDto, NaefDto> CUSTOMER_INFOS_2D
                    = new EntityDto.SetRefAttr<>("CUSTOMER_INFOS_2D");

            public static final EntityDto.MapAttr<Long, List<EntityDto.Desc<CustomerInfoDto>>, NaefDto> CUSTOMER_INFOS_2D_CHANGES
                    = new EntityDto.MapAttr<Long, List<EntityDto.Desc<CustomerInfoDto>>, NaefDto>("CUSTOMER_INFOS_2D_CHANGES");
        }
    }
}
