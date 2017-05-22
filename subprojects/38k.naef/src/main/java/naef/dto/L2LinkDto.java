package naef.dto;

import tef.skelton.Attribute;

import java.util.Set;

public class L2LinkDto extends LinkDto {

    public static class ExtAttr {

        /**
         * このリンクが同種リンクでネストされている場合の外包リンク (包含の外側のリンク) です。
         * <p>
         * 外包リンクが異種の場合 (例えばリンクが eth-link で、その外包が eth-lag の場合) は null 
         * です。
         *
         * @see L2LinkDto#getNestingContainer()
         **/
        public static final SingleRefAttr<L2LinkDto, L2LinkDto> NESTING_CONTAINER
            = new SingleRefAttr<L2LinkDto, L2LinkDto>("naef.dto.l2link.nesting-container");

        /**
         * 最外包リンク フラグです。このリンクが同種リンク ネスティングの最外包であるかどうかを表し
         * ます。
         * <p>
         * 外包リンクが存在しても、それが異種の場合 (例えばリンクが eth-link で、その外包が eth-lag
         * の場合) は <code>true</code> が設定されます。
         * <p>
         * 言い換えると、{@link ExtAttr#NESTED_LINKS} が存在し、{@link ExtAttr#NESTING_CONTAINER} 
         * が null の場合に <code>true</code> となります。
         *
         * @see L2LinkDto#isNestingOutermost()
         **/
        public static final Attribute.SingleBoolean<L2LinkDto> IS_NESTING_OUTERMOST
            = new Attribute.SingleBoolean<L2LinkDto>("naef.dto.l2link.is-nesting-outermost");

        /**
         * このリンクが同種リンクをネストしている場合の内包リンク (包含の内側のリンク) です。
         * <p>
         * 内包リンクに異種リンクが含まれる場合はそれらは含まれません。
         *
         * @see L2LinkDto#getNestedLinks()
         **/
        public static final SetRefAttr<L2LinkDto, L2LinkDto> NESTED_LINKS
            = new SetRefAttr<L2LinkDto, L2LinkDto>("naef.dto.l2link.nested-links");

        /**
         * 最内包リンク フラグです。このリンクが同種リンク ネスティングの最内包であるかどうかを表し
         * ます。
         * <p>
         * 内包リンクが存在しても、それが異種の場合は <code>true</code> が設定されます。
         * <p>
         * 言い換えると、{@link ExtAttr#NESTING_CONTAINER} が null でなく、
         * {@link ExtAttr#NESTED_LINKS} が存在しない場合に <code>true</code> となります。
         *
         * @see L2LinkDto#isNestedInnermost()
         **/
        public static final Attribute.SingleBoolean<L2LinkDto> IS_NESTED_INNERMOST
            = new Attribute.SingleBoolean<L2LinkDto>("naef.dto.l2link.is-nested-innermost");
    }

    public L2LinkDto() {
    }

    /**
     * {@link ExtAttr#NESTING_CONTAINER} のラッパー メソッドです。
     **/
    public LinkDto getNestingContainer() {
        return ExtAttr.NESTING_CONTAINER.deref(this);
    }

    /**
     * {@link ExtAttr#IS_NESTING_OUTERMOST} のラッパー メソッドです。
     **/
    public boolean isNestingOutermost() {
        return ExtAttr.IS_NESTING_OUTERMOST.get(this).booleanValue();
    }

    /**
     * {@link ExtAttr#NESTED_LINKS} のラッパー メソッドです。
     **/
    public Set<L2LinkDto> getNestedLinks() {
        return ExtAttr.NESTED_LINKS.deref(this);
    }

    /**
     * {@link ExtAttr#IS_NESTED_INNERMOST} のラッパー メソッドです。
     **/
    public boolean isNestedInnermost() {
        return ExtAttr.IS_NESTED_INNERMOST.get(this).booleanValue();
    }
}
