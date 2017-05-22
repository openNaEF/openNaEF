package tef;

import java.io.Serializable;
import java.util.Arrays;

class TransactionDigest implements Serializable {

    final TransactionId.W transactionId;
    final byte[] digest;

    TransactionDigest(TransactionId.W transactionId, byte[] digest) {
        this.transactionId = transactionId;
        this.digest = digest;
    }

    @Override
    public int hashCode() {
        return TransactionId.hashCode(transactionId) + Arrays.hashCode(digest);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != getClass()) {
            return false;
        }

        TransactionDigest another = (TransactionDigest) o;
        return TransactionId.equals(another.transactionId, this.transactionId)
                && Arrays.equals(another.digest, this.digest);
    }
}
