package naef.mvo.ip;

import tef.MvoHome;
import tef.skelton.FormatException;
import tef.skelton.Range;
import tef.skelton.ValueException;

public class IpAddressRange extends Range<IpAddress> {

    public static final MvoHome<IpAddressRange> home = new MvoHome<IpAddressRange>(IpAddressRange.class);

    /**
     * 正規化されたサブネット アドレスである場合に値を返します.
     */
    public static IpAddressRange gainAsSubnetAddressRange(IpAddress lowerBound, IpAddress upperBound)
        throws ValueException
    {
        if (upperBound.compareTo(lowerBound) < 0) {
            throw new ValueException("引数の順序が不正です.");
        }
        IpAddress.SubnetAddressUtils.checkCanonicalSubnetAddressRange(lowerBound, upperBound);

        return gain(lowerBound, upperBound);
    }

    public static IpAddressRange gain(IpAddress lowerBound, IpAddress upperBound) {
        for (IpAddressRange instance : home.list()) {
            if (instance.getLowerBound().equals(lowerBound)
                && instance.getUpperBound().equals(upperBound))
            {
                return instance;
            }
        }
        return new IpAddressRange(lowerBound, upperBound);
    }

    public static IpAddressRange gainByRangeStr(java.lang.String rangeStr)
        throws FormatException
    {
        if (0 < rangeStr.indexOf("/")) {
            java.lang.String[] tokens = rangeStr.split("/");
            IpAddress networkAddress = IpAddress.gain(tokens [0]);
            int maskLength;
            try {
                maskLength = java.lang.Integer.parseInt(tokens[1]);
            } catch (NumberFormatException nfe) {
                throw new FormatException("マスク長には数値を指定してください: " + tokens[1]);
            }

            return gainAsSubnetAddressRange(
                networkAddress,
                IpAddress.SubnetAddressUtils.endAddress(networkAddress, maskLength));
        } else {
            java.lang.String[] rangeToken = Range.tokenizeRangeStr(rangeStr);
            IpAddress lowerBound = IpAddress.gain(rangeToken[0]);
            IpAddress upperBound = IpAddress.gain(rangeToken[1]);
            Range.checkOrder(lowerBound, upperBound);
            return gain(lowerBound, upperBound);
        }
    }

    private IpAddressRange(MvoId id) {
        super(id);
    }

    private IpAddressRange(IpAddress lowerBound, IpAddress upperBound) {
        super(lowerBound, upperBound);
    }

    @Override public IpAddressRange newInstance(IpAddress lowerBound, IpAddress upperBound) {
        return gain(lowerBound, upperBound);
    }

    /**
     * この範囲オブジェクトがサブネット アドレスを表す場合のマスク長を計算します.
     */
    public java.lang.Integer computeSubnetMaskLength() {
        return IpAddress.SubnetAddressUtils.maskLength(getLowerBound(), getUpperBound());
    }
}
