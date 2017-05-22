package tef.skelton.dto;

/**
 * DTO の初期化を定義します.
 * <p>
 * DTO 生成時に {@link DtoInitializer#initialize(Object,Object) initialize(MODEL,DTO)} 
 * が実行されます.
 *
 * @param <MODEL> 初期化対象の DTO のソースオブジェクトの型です.
 * @param <DTO> 初期化対象の DTO の型です.
 */
public abstract class DtoInitializer<MODEL, DTO> {

    private final Class<?> dtoClass_;

    protected DtoInitializer(Class<?> dtoClass) {
        dtoClass_ = dtoClass;
    }

    boolean isToInitialize(Object o) {
        return dtoClass_.isInstance(o);
    }

    /**
     * DTO 生成時に {@link DtoFactory} によって呼び出されます.
     * <p>
     * サブタイプではこのメソッドをオーバーライドし, DTO の初期化を記述してください.
     */
    public abstract void initialize(MODEL model, DTO dto);
}
