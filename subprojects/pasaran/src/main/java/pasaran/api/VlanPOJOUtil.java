package pasaran.api;


import naef.dto.NetworkDto;
import naef.dto.PortDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIdPoolDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vlan.VlanSegmentDto;
import naef.mvo.Network;
import naef.mvo.Port;
import naef.mvo.vlan.Vlan;
import naef.mvo.vlan.VlanIdPool;
import naef.mvo.vlan.VlanIf;
import naef.mvo.vlan.VlanSegment;
import naef.ui.NaefDtoFacade;
import pasaran.NaefConnector;
import pasaran.pojo.PasaranLinkPOJO;
import pasaran.pojo.PasaranVlanPOJO;
import pasaran.util.MvoUtil;
import pasaran.util.TxUtil;
import tef.MVO;

import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

public class VlanPOJOUtil {
    /**
     * 時間とバージョンを指定してvlanを取得
     * @param time 時間
     * @param version バージョン
     * @return vlanのリスト
     */
    public static List<PasaranVlanPOJO> getVlans(String time, String version) throws RemoteException {
        long start = System.currentTimeMillis();
        List<PasaranVlanPOJO> pojos = new ArrayList<>();

        NaefDtoFacade facade = NaefConnector.getInstance().getConnection().getDtoFacade();
        Set<VlanIdPoolDto> pools = facade.getRootIdPools(VlanIdPoolDto.class);
        for (VlanIdPoolDto pool : pools) {
            pojos.addAll(
                    pool.getUsers().stream()
                            .map(VlanPOJOUtil::toPOJO)
                            .collect(Collectors.toList()));
        }

        pojos.sort(Comparator.comparing(pojo -> pojo.vlanId));

        long end = System.currentTimeMillis();
        System.out.printf("vlan end. %dms\n", (end - start));

        return pojos;
    }


    /**
     * Vlan の POJO を作る
     */
    public static PasaranVlanPOJO toPOJO(VlanDto vlan) {
        PasaranVlanPOJO pojo = new PasaranVlanPOJO();
        pojo.mvoId = vlan.getOid().toString();
        pojo.name = "vlan" + vlan.getVlanId();
        pojo.vlanId = vlan.getVlanId();


        Set<VlanSegmentDto> vlanLinks = new HashSet<>();
        Set<String> vlanIfs = new HashSet<>();
        Set<String> taggedPorts = new HashSet<>();
        Set<String> untaggedPorts = new HashSet<>();

        vlanIfs.addAll(MvoUtil.getRefIds(vlan, VlanDto.ExtAttr.MEMBER_VLAN_IF));

        System.out.printf("[VLAN %4d] %s ------\n", vlan.getVlanId(), vlan.getOid().toString());
        for (PortDto memberPort : vlan.getMemberPorts()) {
            //System.out.printf("\tvlan-if %s\n", memberPort.getOid().toString());
            if (memberPort instanceof VlanIfDto) {
                VlanIfDto vlanIf = (VlanIfDto) memberPort;

                taggedPorts.addAll(MvoUtil.getRefIds(vlanIf, VlanIfDto.ExtAttr.TAGGED_PORTS));
                untaggedPorts.addAll(MvoUtil.getRefIds(vlanIf, VlanIfDto.ExtAttr.UNTAGGED_PORTS));
                vlanLinks.addAll(vlanIf.getVlanLinks());
            } else {
                throw new IllegalStateException();
            }
        }

        pojo.taggedPorts = new ArrayList<>(taggedPorts);
        pojo.untaggedPorts = new ArrayList<>(untaggedPorts);
        pojo.vlanIfs = new ArrayList<>(vlanIfs);

        pojo.vlanLinks = new ArrayList<>();
        for (VlanSegmentDto link : vlanLinks) {
            //System.out.printf("\tvlan-link %s\n", link.getOid().toString());
            pojo.vlanLinks.add(createVlanLink(link));
        }

        //System.out.println("-------");
        return pojo;
    }

    private static PasaranLinkPOJO createVlanLink(VlanSegmentDto link) {
        PasaranLinkPOJO pojo = new PasaranLinkPOJO();
        Set<NetworkDto> lowerLinks = link.getLowerLayerLinks();
        if(lowerLinks.isEmpty()) {
            System.err.printf("\tno lowers %s\n", link.getOid().toString());
        }
        if(lowerLinks.size() >= 2) {
            System.out.printf("\tmany lowers %s\n", link.getOid().toString());
        }

        NetworkDto lower = lowerLinks.iterator().next();

        Iterator<PortDto> it = lower.getMemberPorts().iterator();
        pojo.children = new ArrayList<>();
        pojo.children.add(lower.getOid().toString());
        pojo.source = it.next().getOid().toString();
        pojo.target = it.next().getOid().toString();
        pojo.mvoId = MvoUtil.toMvoId(link);
        pojo.objectType = "vlan-link";
        return pojo;
    }

    /**
     * 時間とバージョンを指定してvlanを取得
     * @param time 時間
     * @param version バージョン
     * @return vlanのリスト
     */
    public static List<PasaranVlanPOJO> getMvoVlans(String time, String version) throws RemoteException {
        long start = System.currentTimeMillis();
        try {
            TxUtil.beginReadTx(time, version);
            List<PasaranVlanPOJO> pojos = new ArrayList<>();
            for (VlanIdPool.Dot1q pool : VlanIdPool.Dot1q.home.list()) {
                pojos.addAll(
                        pool.getUsers().stream()
                                .map(VlanPOJOUtil::toPOJO)
                                .collect(Collectors.toList()));
            }
            pojos.sort(Comparator.comparing(pojo -> pojo.vlanId));

            long end = System.currentTimeMillis();
            System.out.printf("vlan end. %dms\n", (end - start));

            return pojos;
        } finally {
            TxUtil.closeTx();
        }
    }

    /**
     * Vlan の POJO を作る
     */
    public static PasaranVlanPOJO toPOJO(Vlan vlan) {
        PasaranVlanPOJO pojo = new PasaranVlanPOJO();
        pojo.mvoId = MvoUtil.toMvoId(vlan);

        pojo.vlanId = Vlan.Attr.ID.get(vlan);
        pojo.name = "vlan" + pojo.vlanId;


        Set<String> vlanIfs = new HashSet<>();
        Set<String> taggedPorts = new HashSet<>();
        Set<String> untaggedPorts = new HashSet<>();

        // System.out.printf("[VLAN %4d] %s ------\n", pojo.mvoId, vlan.getMvoId());
        for (VlanIf vlanIf : vlan.getCurrentMemberPorts()) {
            vlanIfs.add(MvoUtil.toMvoId(vlanIf));
            taggedPorts.addAll(vlanIf.getCurrentTaggedPorts().stream().map(p -> MvoUtil.toMvoId((MVO) p)).collect(Collectors.toList()));
            untaggedPorts.addAll(vlanIf.getCurrentUntaggedPorts().stream().map(p -> MvoUtil.toMvoId((MVO) p)).collect(Collectors.toList()));
        }

        pojo.taggedPorts = new ArrayList<>(taggedPorts);
        pojo.untaggedPorts = new ArrayList<>(untaggedPorts);
        pojo.vlanIfs = new ArrayList<>(vlanIfs);

        pojo.vlanLinks = new ArrayList<>();
        pojo.vlanLinks.addAll(vlan.getCurrentParts(true).stream().map(VlanPOJOUtil::createVlanLink).collect(Collectors.toList()));

        return pojo;
    }


    private static PasaranLinkPOJO createVlanLink(VlanSegment link) {
        PasaranLinkPOJO pojo = new PasaranLinkPOJO();
        Set<? extends Network.LowerStackable> lowerLinks = link.getCurrentLowerLayers(false);
        if(lowerLinks.size() >= 2) {
            System.out.printf("\tmany lowers %s\n", MvoUtil.toMvoId(link));
        }

        if(! lowerLinks.isEmpty()) {
            Network.LowerStackable lower = lowerLinks.iterator().next();
            pojo.children = new ArrayList<>();
            pojo.children.add(MvoUtil.toMvoId((MVO) lower));
            Iterator<? extends Port> it = lower.getCurrentMemberPorts().iterator();
            pojo.source = MvoUtil.toMvoId((MVO) it.next());
            pojo.target = MvoUtil.toMvoId((MVO) it.next());
        } else {
            // lower-layerが存在しないvlan-link
            // vlanからの参照をはずすべき？
            //System.err.println(link.getMvoId());
        }

        pojo.mvoId = MvoUtil.toMvoId(link);
        pojo.objectType = "vlan-link";
        return pojo;
    }

}
