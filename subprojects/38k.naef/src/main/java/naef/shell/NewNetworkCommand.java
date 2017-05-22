package naef.shell;

import naef.mvo.AbstractNetwork;
import tef.MVO;
import tef.skelton.Attribute;
import tef.skelton.IdPool;
import tef.skelton.Model;

public class NewNetworkCommand extends NaefShellCommand {

    @Override public String getArgumentDescription() {
        return "[network type] or [network type] [id]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        if (args.argsSize() == 2) {
            deprecatedLogic(args);
            return;
        }


        checkArgsSize(args, 1);
        Class<?> type = resolveNetworkTypeName(args.arg(0)).type();

        beginWriteTransaction();

        Object network;
        try {
            network = type.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        setContext((Model) network, ((MVO) network).getMvoId().getLocalStringExpression());

        commitTransaction();
    }

    @Deprecated private void deprecatedLogic(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 2);
        Class<?> type = resolveNetworkTypeName(args.arg(0)).type();
        String idStr = args.arg(1);

        beginWriteTransaction();

        IdPool idPool;
        AbstractNetwork network;
        Attribute idPoolAttr;

        if (type == naef.mvo.of.OfPatchLink.class) {
            idPool = contextAs(naef.mvo.of.OfPatchLink.PatchIdPool.class, "OF-Patch Link IDプール");
            network = new naef.mvo.of.OfPatchLink();
            idPoolAttr = naef.mvo.of.OfPatchLink.PATCH_ID_POOL;
        } else if (type == naef.mvo.wdm.OpticalPath.class) {
            idPool = contextAs(naef.mvo.wdm.OpticalPathIdPool.class, "光パスIDプール");
            network = new naef.mvo.wdm.OpticalPath();
            idPoolAttr = naef.mvo.wdm.OpticalPath.Attr.ID_POOL;
        } else if (type == naef.mvo.mpls.Pseudowire.class) {
            idPool = contextAs(naef.mvo.mpls.PseudowireIdPool.class, "Pseudowire IDプール");
            network = new naef.mvo.mpls.Pseudowire();
            idPoolAttr = naef.mvo.mpls.Pseudowire.Attr.ID_POOL;
        } else if (type == naef.mvo.mpls.RsvpLspHopSeries.class) {
            idPool = contextAs(naef.mvo.mpls.RsvpLspHopSeriesIdPool.class, "RSVP LSPホップ列IDプール");
            network = new naef.mvo.mpls.RsvpLspHopSeries();
            idPoolAttr = naef.mvo.mpls.RsvpLspHopSeries.Attr.ID_POOL;
        } else if (type == naef.mvo.vlan.Vlan.class) {
            idPool = contextAs(naef.mvo.vlan.VlanIdPool.class, "VLAN IDプール");
            network = new naef.mvo.vlan.Vlan();
            idPoolAttr = naef.mvo.vlan.Vlan.Attr.ID_POOL;
        } else if (type == naef.mvo.vpls.Vpls.class) {
            idPool = contextAs(naef.mvo.vpls.VplsIdPool.class, "VPLS IDプール");
            network = new naef.mvo.vpls.Vpls();
            idPoolAttr = naef.mvo.vpls.Vpls.Attr.ID_POOL;
        } else if (type == naef.mvo.vrf.Vrf.class) {
            idPool = contextAs(naef.mvo.vrf.VrfIdPool.class, "VRF IDプール");
            network = new naef.mvo.vrf.Vrf();
            idPoolAttr = naef.mvo.vrf.Vrf.Attr.ID_POOL;
        } else if (type == naef.mvo.vxlan.Vxlan.class) {
            idPool = contextAs(naef.mvo.vxlan.VxlanIdPool.class, "VXLAN IDプール");
            network = new naef.mvo.vxlan.Vxlan();
            idPoolAttr = naef.mvo.vxlan.Vxlan.ID_POOL;
        } else {
            throw new ShellCommandException("サポートされていない型です.");
        }

        try {
            idPool.assignUser(idPool.parseId(idStr), network);
        } catch (IdPool.PoolException pe) {
            throw new ShellCommandException(pe.getMessage());
        }
        network.set(idPoolAttr, idPool);

        setContext(network, idStr);

        commitTransaction();
    }
}
