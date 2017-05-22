package voss.mplsnms.shell;

import naef.mvo.ip.IpAddress;
import naef.mvo.ip.IpIf;
import naef.mvo.ip.IpSubnet;
import naef.mvo.ip.IpSubnetAddress;
import naef.shell.NaefShellCommand;
import tef.skelton.Range;

public class ZanteiSetIpifAddress extends NaefShellCommand {

    @Override public String getArgumentDescription() {
        return "[ip address]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 1);

        String ipaddrStr = args.arg(0);

        beginWriteTransaction();

        IpIf ipif = contextAs(IpIf.class, "ip-if");

        IpAddress ipaddr = IpAddress.gain(ipaddrStr);

        IpIf.Attr.IP_ADDRESS.set(ipif, ipaddr);

        IpSubnetAddress subnetaddr = getSubnetAddress(ipaddr);
        if (subnetaddr != null) {
            IpIf.Attr.IP_SUBNET_ADDRESS.set(ipif, subnetaddr);

            IpSubnet subnet = IpSubnetAddress.Attr.IP_SUBNET.get(subnetaddr);
            if (subnet != null) {
                subnet.addMemberPort(ipif);
            }
        }

        commitTransaction();
    }

    private IpSubnetAddress getSubnetAddress(IpAddress ipaddr) {
        IpSubnetAddress result = null;
        for (IpSubnetAddress subnetaddr : IpSubnetAddress.home.list()) {
            Range<IpAddress> range = getRange(subnetaddr);
            if (range != null) {
                if (range.contains(ipaddr, ipaddr)) {
                    if (result == null) {
                        result = subnetaddr;
                    } else {
                        result = getRange(result).contains(getRange(subnetaddr))
                            ? subnetaddr
                            : result;
                    }
                }
            }
        }
        return result;
    }

    private Range<IpAddress> getRange(IpSubnetAddress subnetaddr) {
        return subnetaddr.getMasterRanges().size() == 0
            ? null
            : subnetaddr.getMasterRanges().iterator().next();
    }
}
