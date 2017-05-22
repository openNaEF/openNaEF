package tef.skelton.dto;

import tef.MVO;

public class MvoOid implements EntityDto.Oid {

    public final MVO.MvoId oid;

    public MvoOid(MVO.MvoId oid) {
        this.oid = oid;
    }

    @Override public String toString() {
        return oid.toString();
    }

    @Override public int hashCode() {
        return oid.hashCode();
    }

    @Override public boolean equals(Object o) {
        if (o == null || o.getClass() != getClass()) {
            return false;
        }

        MvoOid another = getClass().cast(o);
        return oid.equals(another.oid);
    }
}
