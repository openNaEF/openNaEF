package naef.shell;

import naef.mvo.Network;
import tef.skelton.Attribute;
import tef.skelton.IdPool;
import tef.skelton.Model;

public class ReleaseNetworkIdCommand extends NaefShellCommand {

    @Override public String getArgumentDescription() {
        return "";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 0);

        beginWriteTransaction();

        Network network = contextAsNetwork();
        Class<?> type = network.getClass();
        Attribute<? extends IdPool, ?> idPoolAttr;

        if (type == naef.mvo.ip.IpSubnet.class) {
            idPoolAttr = naef.mvo.ip.IpSubnet.Attr.NAMESPACE;
        } else if (type == naef.mvo.wdm.OpticalPath.class) {
            idPoolAttr = naef.mvo.wdm.OpticalPath.Attr.ID_POOL;
        } else if (type == naef.mvo.mpls.Pseudowire.class) {
            idPoolAttr = naef.mvo.mpls.Pseudowire.Attr.ID_POOL;
        } else if (type == naef.mvo.mpls.RsvpLspHopSeries.class) {
            idPoolAttr = naef.mvo.mpls.RsvpLspHopSeries.Attr.ID_POOL;
        } else if (type == naef.mvo.vlan.Vlan.class) {
            idPoolAttr = naef.mvo.vlan.Vlan.Attr.ID_POOL;
        } else if (type == naef.mvo.vpls.Vpls.class) {
            idPoolAttr = naef.mvo.vpls.Vpls.Attr.ID_POOL;
        } else if (type == naef.mvo.vrf.Vrf.class) {
            idPoolAttr = naef.mvo.vrf.Vrf.Attr.ID_POOL;
        } else if (type == naef.mvo.vxlan.Vxlan.class) {
            idPoolAttr = naef.mvo.vxlan.Vxlan.ID_POOL;
        } else {
            throw new ShellCommandException("サポートされていない型です.");
        }

        IdPool idPool = network.get((Attribute<? extends IdPool, Model>) idPoolAttr);
        try {
            idPool.unassignUser(network);
        } catch (IdPool.PoolException pe) {
            throw new ShellCommandException(pe.getMessage());
        }
        network.set((Attribute<? extends IdPool, Model>) idPoolAttr, null);

        commitTransaction();
    }
}
