package opennaef.rest.api;

import pasaran.naef.PasaranNaefService;
import tef.MVO;
import tef.skelton.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * MVO に関する処理を行う static関数の集まり
 */
public class MVOs {
    /**
     * すべての MVO から types で指定したクラスのオブジェクトのみを取り出す
     *
     * @param types フィルターする MVO のクラスのリスト
     * @return MVOのリスト
     */
    public static List<MVO> findMVOs(Set<Class<? extends Model>> types) {
        List<MVO> list = new ArrayList<>();
        for (MVO mvo : PasaranNaefService.instance().getMvoRegistry().list()) {
            for (Class<?> c : types) {
                if (c.isInstance(mvo)) {
                    list.add(mvo);
                    break;
                }
            }
        }
        return list;
    }
}
