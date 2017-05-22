package voss.model;

import java.io.Serializable;
import java.math.BigInteger;

public class MplsTunnelKey implements Serializable {
    private static final long serialVersionUID = 1L;

    private final BigInteger tunnelIndex;
    private final BigInteger tunnelInstance;
    private final BigInteger ingressLsrId;
    private final BigInteger egressLsrId;

    public MplsTunnelKey(BigInteger[] keys) {
        if (keys.length != 4) {
            throw new IllegalArgumentException();
        }

        this.tunnelIndex = keys[0];
        this.tunnelInstance = keys[1];
        this.ingressLsrId = keys[2];
        this.egressLsrId = keys[3];
    }

    public BigInteger getTunnelIndex() {
        return this.tunnelIndex;
    }

    public BigInteger getTunnelInstance() {
        return this.tunnelInstance;
    }

    public BigInteger getIngressLsrId() {
        return this.ingressLsrId;
    }

    public BigInteger getEgressLsrId() {
        return this.egressLsrId;
    }

    @Override
    public String toString() {
        return this.getKey();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MplsTunnelKey) {
            return this.getKey().equals(((MplsTunnelKey) o).getKey());
        }
        return false;
    }

    public String getKey() {
        String key = "MplsTunnelKey:" + this.tunnelIndex.toString() + ":" + this.tunnelInstance.toString()
                + ":" + this.ingressLsrId.toString() + "->" + this.egressLsrId.toString();
        return key;
    }

    @Override
    public int hashCode() {
        int value = getKey().hashCode();
        return value * value + value + 41;
    }

}