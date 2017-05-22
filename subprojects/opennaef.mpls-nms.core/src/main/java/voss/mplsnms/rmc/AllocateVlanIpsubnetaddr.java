package voss.mplsnms.rmc;

import lib38k.rmc.MethodCall5;
import lib38k.rmc.MethodExec5;
import naef.NaefTefService;
import naef.mvo.Network;
import naef.mvo.ip.IpAddress;
import naef.mvo.ip.IpAddressRange;
import naef.mvo.ip.IpSubnet;
import naef.mvo.ip.IpSubnetAddress;
import naef.mvo.ip.IpSubnetNamespace;
import naef.mvo.ip.Ipv4Address;
import naef.mvo.vlan.Vlan;
import tef.MVO;
import tef.TefService;
import tef.TransactionContext;
import tef.skelton.ConfigurationException;
import tef.skelton.IdPool;
import tef.skelton.Range;
import voss.mplsnms.MplsnmsAttrs;

public class AllocateVlanIpsubnetaddr {

    public static class Call
        extends MethodCall5<String, String, String, String, IpAddress, Integer>
    {
        public Call(
            String vlanMvoIdStr,
            String masterSubnetaddressName,
            String masterSubnetNamespaceName,
            IpAddress address,
            Integer masklength)
        {
            super(vlanMvoIdStr, masterSubnetaddressName, masterSubnetNamespaceName, address, masklength);
        }
    }

    public static class Exec
        extends MethodExec5<Call, String, String, String, String, IpAddress, Integer>
    {
        @Override public String execute(
            String vlanMvoIdStr,
            String masterSubnetaddressName,
            String masterSubnetNamespaceName,
            IpAddress address,
            Integer masklength)
        {
            if ((address != null && masklength == null)
                || (address == null && masklength != null))
            {
                throw new IllegalArgumentException(
                    "address と masklength は両方nullかまたは両方非nullでなければなりません.");
            }

            NaefTefService.instance().beginWriteTransaction(null);

            try {
                Vlan vlan
                    = (Vlan) TefService.instance().getMvoRegistry().get(MVO.MvoId.getInstanceByLocalId(vlanMvoIdStr));
                if (address == null) {
                    IpSubnetAddress masterSubnetaddr = IpSubnetAddress.home.getByName(masterSubnetaddressName);
                    if (masterSubnetaddr == null) {
                        throw new ConfigurationException(
                            "指定された ip-subnet-address が見つかりません: " + masterSubnetaddressName);
                    }

                    IpSubnetNamespace masterSubnetNamespace
                        = IpSubnetNamespace.home.getByName(masterSubnetNamespaceName);
                    if (masterSubnetNamespace == null) {
                        throw new ConfigurationException(
                            "指定された ip.subnet-namespace が見つかりません: " + masterSubnetNamespace);
                    }

                    try {
                        allocateAuto(vlan, masterSubnetaddr, masterSubnetNamespace);
                    } catch(IdPool.PoolException pe) {
                        throw new ConfigurationException(pe.getMessage());
                    }
                } else {
                    throw new RuntimeException("未実装");
                }

                IpSubnet subnet = getIpSubnet(vlan);
                IpSubnetAddress subnetaddress = IpSubnet.Attr.SUBNET_ADDRESS.get(subnet);
                Ipv4Address lowerbound = getLowerBound(subnetaddress);
                Ipv4Address upperbound = getUpperBound(subnetaddress);
                long size = (upperbound.getRawAddress() & 0xffffffffl) - (lowerbound.getRawAddress() & 0xffffffffl);
                String result
                    = lowerbound.toString()
                    + "/"
                    + Integer.toString((int)(32 - Math.log(size) / Math.log(2)));

                TransactionContext.commit();

                return result;
            } finally {
                TransactionContext.close();
            }
        }

        private Ipv4Address getLowerBound(IpSubnetAddress subnetaddr) {
            return (Ipv4Address) subnetaddr.getMasterRanges().iterator().next().getLowerBound();
        }

        private Ipv4Address getUpperBound(IpSubnetAddress subnetaddr) {
            return (Ipv4Address) subnetaddr.getMasterRanges().iterator().next().getUpperBound();
        }

        private void allocateAuto(Vlan vlan, IpSubnetAddress masterSubnetaddr, IpSubnetNamespace masterSubnetNamespace)
            throws IdPool.PoolException 
        {
            IpSubnet subnet = getIpSubnet(vlan);
            if (subnet == null) {
                subnet = new IpSubnet();
                subnet.stackOver(vlan);
            }

            Long blocksize = MplsnmsAttrs.IpSubnetAddressAttr.ADDRESS_JIDOU_HARAIDASHI_BLOCK_SIZE.get(masterSubnetaddr);
            if (blocksize == null) {
                throw new IllegalStateException(
                    MplsnmsAttrs.IpSubnetAddressAttr.ADDRESS_JIDOU_HARAIDASHI_BLOCK_SIZE.getName()
                        + " が設定されていません.");
            }

            Ipv4Address max = getMaxAddress(masterSubnetaddr);
            Ipv4Address lower;
            if (max == null) {
                lower = getLowerBound(masterSubnetaddr);
            } else {
                long maxUpper = max.getRawAddress() & 0xffffffffl;
                int usedBlockCount = (int)(maxUpper / blocksize.longValue() + 1);
                lower = Ipv4Address.gain((int)(blocksize * usedBlockCount));
            }
            Ipv4Address upper
                = Ipv4Address.gain((int)((lower.getRawAddress() & 0xffffffffl) + blocksize.longValue() - 1));

            IpSubnetAddress address = new IpSubnetAddress();
            address.setName(lower.toString());
            address.setParent(masterSubnetaddr);
            IpAddressRange addressRange = IpAddressRange.gain(lower, upper);
            address.allocateRange(addressRange);
            IpSubnet.Attr.SUBNET_ADDRESS.set(subnet, address);

            String addressStr
                = String.format(
                    "%0" + Integer.toString(lower.ipVersionBitLength() / 4) + "x",
                    lower.toBigInteger())
                + "/" + addressRange.computeSubnetMaskLength().toString();
            IpSubnet.Attr.NAMESPACE.set(subnet, masterSubnetNamespace);
            IpSubnet.Attr.SUBNET_NAME.set(subnet, addressStr);
        }

        private IpSubnet getIpSubnet(Vlan vlan) {
            IpSubnet result = null;
            for (Network upper : vlan.getCurrentUpperLayers(false)) {
                if (upper instanceof IpSubnet) {
                    if (result == null) {
                        result = (IpSubnet) upper;
                    } else {
                        throw new ConfigurationException("vlan 上に複数の ip-subnet が存在します: " + vlan.getMvoId());
                    }
                }
            }
            return result;
        }

        private Ipv4Address getMaxAddress(IpSubnetAddress master) {
            Range<IpAddress> result = null;
            for (Range<IpAddress> subrange : master.getSubRanges()) {
                if (result == null
                    || result.getUpperBound().compareTo(subrange.getUpperBound()) < 0)
                {
                    result = subrange;
                }
            }
            return result == null ? null : (Ipv4Address) result.getUpperBound();
        }
    }
}
