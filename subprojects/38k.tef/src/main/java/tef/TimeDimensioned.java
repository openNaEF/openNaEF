package tef;

import java.lang.annotation.*;

/**
 * <p>型、コンストラクタ、メソッドが時間次元を持つことを示す注釈型です。
 * <p>この注釈型がコンストラクタやメソッドに付けられている場合、それらの呼び出しは
 * value で示される時間次元の影響を受けることを意味します。
 * この注釈型が型に対して付けられている場合、その型のメンバーであるコンストラクタや
 * メソッドに時間次元を持つものがあることを意味します。
 * <p>MVO のサブタイプを定義する場合、この注釈型を付けることを推奨します。
 * <p>また、MVO でないオブジェクトであっても、メソッド内でこの注釈型が付いたコンストラクタや
 * メソッドを呼び出す場合、明示的にコンテキスト トランザクションの時間操作を行わない限り、
 * 呼び出し先の時間次元が連鎖的に波及することになるため、そのメソッドにもこの注釈型を
 * 付けることを推奨します。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface TimeDimensioned {

    /**
     * この注釈型が付けられた対象の時間次元です。
     */
    TimeDimension value();
}
