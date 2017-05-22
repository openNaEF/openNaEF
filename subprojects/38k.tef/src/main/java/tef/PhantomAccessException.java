package tef;

/**
 * <p>ファントムになった MVO へのアクセスが検出された時に発生する例外です。
 */
public class PhantomAccessException extends TransactionCanNotProgressException {

    PhantomAccessException() {
        super("phantom object access detected.");
    }
}
