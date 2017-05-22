package pasaran.api;

import naef.dto.PortDto;
import naef.dto.mpls.PseudowireDto;
import naef.ui.NaefDtoFacade;
import pasaran.NaefConnector;
import pasaran.pojo.PasaranPOJO;
import pasaran.pojo.PseudoWirePOJO;
import pasaran.util.MvoUtil;
import tef.skelton.dto.EntityDto;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PseudoWirePOJOUtil {

    /**
     * 時間とバージョンを指定してpseudo-wireを取得
     * @param time 時間
     * @param version バージョン
     * @return pseudo-wireのリスト
     */
    public static List<PasaranPOJO> getPseudoWire(String time, String version) throws RemoteException {
        long start = System.currentTimeMillis();
        NaefDtoFacade dtoFacade = NaefConnector.getInstance().getConnection().getDtoFacade();

        List<PasaranPOJO> pojos = new ArrayList<>();
        for (PseudowireDto pw : dtoFacade.getPseudowires()) {
            PseudoWirePOJO pojo = PseudoWirePOJOUtil.createPOJO(pw);
            pojos.add(pojo);
        }

        long end = System.currentTimeMillis();
        System.out.printf("pw end. %dms\n", (end - start));

        return pojos;
    }

    public static PseudoWirePOJO createPOJO(PseudowireDto pw) {
        if (pw == null) throw new IllegalArgumentException("pw is null.");

        System.out.printf("-- PW: %s // %s\n", pw.getOid().toString(), pw.getStringId());
        PseudoWirePOJO pwPojo = new PseudoWirePOJO();
        setCommonPOJOAttr(pwPojo, pw);
        pwPojo.name = Objects.toString(pw.getValue("PW名"), pw.getStringId());

        PortDto ac1 = pw.getAc1();
        pwPojo.ac1 = ac1 != null ? ac1.getOid().toString() : null;

        PortDto ac2 = pw.getAc2();
        pwPojo.ac2 = ac2 != null ? ac2.getOid().toString() : null;

        pwPojo.lowerLayer = pw.getLowerLayerLinks().stream()
                .map(network -> network.getOid().toString())
                .collect(Collectors.toList());

        return pwPojo;
    }

    private static void setCommonPOJOAttr(PasaranPOJO pojo, EntityDto dto) {
        pojo.mvoId = MvoUtil.toMvoId(dto);
        // FIXME 時間をPOJOへ入れる
//        pojo.readTxTime = TransactionContext.getTargetTime();
//        pojo.readTxVersion = TransactionContext.getTargetVersion().getIdString();
    }
}
