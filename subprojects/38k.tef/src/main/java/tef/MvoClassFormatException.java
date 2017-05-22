package tef;

/**
 * <p>規約に適合しない MVO クラスを生成しようとした時に発生する例外です。
 */
public class MvoClassFormatException extends RuntimeException {

    /**
     * <p>MvoField のアクセス修飾子が規約に適合しない場合に発生する例外です。
     *
     * @see MVO.MvoField
     */
    public static class FieldDefinition extends MvoClassFormatException {

        FieldDefinition(String message) {
            super(message);
        }
    }

    /**
     * <p>MvoId を取るコンストラクタが定義されていない MVO のサブタイプを生成した時に
     * 発生する例外です。
     */
    public static class ConstructorDefinition extends MvoClassFormatException {

        ConstructorDefinition(String message) {
            super(message);
        }
    }

    protected MvoClassFormatException(String message) {
        super(message);
    }
}
