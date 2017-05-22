package tef;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class TransactionDigestComputer {

    private TransactionId.W transactionId_;
    private final MessageDigest digest_;

    TransactionDigestComputer() {
        transactionId_ = null;
        try {
            digest_ = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae);
        }
    }

    synchronized void update(TransactionId.W transactionId, byte[] journalContents) {
        transactionId_ = transactionId;
        digest_.update(journalContents);
    }

    synchronized TransactionDigest getDigest() {
        try {
            return new TransactionDigest
                    (transactionId_, ((MessageDigest) digest_.clone()).digest());
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse);
        }
    }
}
