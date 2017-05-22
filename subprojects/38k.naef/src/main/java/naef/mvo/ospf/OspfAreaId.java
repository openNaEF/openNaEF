package naef.mvo.ospf;

import naef.NaefUtils;
import tef.ExtraObjectCoder;
import tef.skelton.FormatException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OspfAreaId implements Comparable<OspfAreaId>, Serializable {

    public static ExtraObjectCoder<OspfAreaId> EXTRA_OBJECT_CODER
        = new ExtraObjectCoder<OspfAreaId>("ospf.area-id", OspfAreaId.class)
    {
        @Override public String encode(OspfAreaId obj) {
            return Integer.toHexString(obj.idValue_);
        }

        @Override public OspfAreaId decode(String str) {
            return OspfAreaId.gain(NaefUtils.parseHexInt(str));
        }
    };

    private static final List<OspfAreaId> instances__ = new ArrayList<OspfAreaId>();

    public static synchronized OspfAreaId gain(int plainIdValue) {
        OspfAreaId obj = new OspfAreaId(plainIdValue);
        int index = Collections.<OspfAreaId>binarySearch(instances__, obj);
        if (0 <= index) {
            return instances__.get(index);
        } else {
            instances__.add(obj);
            Collections.<OspfAreaId>sort(instances__);
            return obj;
        }
    }

    /**
     * 文字列を OSPF area ID として解釈します. まず数値形式として解釈を試み, エラーとなる場合は
     * dotted octet 形式としての解釈を試みます.
     */
    public static synchronized OspfAreaId gain(String str) throws FormatException {
        try {
            long value = Long.parseLong(str);
            if (0xffffffffl < value) {
                throw new FormatException("OSPFエリアIDの最大値を越えています: " + str);
            }
            return gain((int)(0xffffffffl & value));
        } catch (NumberFormatException nfe) {
        }

        return gain(NaefUtils.parseIntAsDottedOctet(str));
    }

    private final int idValue_;

    private OspfAreaId(int idValue) {
        idValue_ = idValue;
    }

    public long getIdValue() {
        return idValue_ & 0xffffffffl;
    }

    public String toDottedOctet() {
        return NaefUtils.formatIntAsDottedOctet(idValue_);
    }

    @Override public int compareTo(OspfAreaId another) {
        return Long.signum(getIdValue() - another.getIdValue());
    }

    @Override public String toString() {
        return toDottedOctet();
    }

    @Override public int hashCode() {
        return idValue_;
    }

    @Override public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        } else {
            OspfAreaId another = (OspfAreaId) o;
            return another.idValue_ == this.idValue_;
        }
    }

    public Object readResolve() throws java.io.ObjectStreamException {
        return gain(idValue_);
    }
}
