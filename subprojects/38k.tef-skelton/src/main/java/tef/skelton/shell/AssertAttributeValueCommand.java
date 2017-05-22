package tef.skelton.shell;

import lib38k.parser.Ast;
import lib38k.parser.ParseException;
import lib38k.parser.StringToken;
import lib38k.parser.Syntax;
import lib38k.parser.Token;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.Model;
import tef.skelton.MvoCollection;
import tef.skelton.ObjectResolver;
import tef.skelton.ResolveException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssertAttributeValueCommand extends SkeltonShellCommand {

    private static class EvaluationException extends RuntimeException {

        EvaluationException(String message) {
            super(message);
        }
    }

    private static class Parser {

        /*
         * [expression]:
         *   assertion-term | logical-expression
         * [logical-expression]:
         *   "(" expression ( "and" | "or" ) expression ")"
         * [assertion-term]:
         *   isnull-assertion | is-assertion | contains-assertion
         * [isnull-assertion]:
         *   "is-null"
         * [is-assersion]:
         *   "is" any-str
         * [contains-assertion]:
         *   "contains" any-str
         * [any-str]:
         *   <terminal>
         */

        public static final Syntax.Leaf.Symbol LP = new Syntax.Leaf.Symbol("(");
        public static final Syntax.Leaf.Symbol RP = new Syntax.Leaf.Symbol(")");
        public static final Syntax.Leaf.Symbol AND = new Syntax.Leaf.Symbol("and");
        public static final Syntax.Leaf.Symbol OR = new Syntax.Leaf.Symbol("or");
        public static final Syntax.Leaf.Symbol IS_NULL = new Syntax.Leaf.Symbol("is-null");
        public static final Syntax.Leaf.Symbol IS = new Syntax.Leaf.Symbol("is");
        public static final Syntax.Leaf.Symbol CONTAINS = new Syntax.Leaf.Symbol("contains");

        public static final Syntax.Branch EXPRESSION = new Syntax.Branch("expression") {

            @Override protected Syntax definition() {
                return choice(ASSERTION_TERM, LOGICAL_EXPRESSION);
            }

            @Override public Boolean evaluate(Ast ast) {
                Ast.Choice option = (Ast.Choice) ast;
                return (Boolean) option.term().evaluate();
            }
        };

        public static final Syntax.Branch LOGICAL_EXPRESSION = new Syntax.Branch("logical-expression") {

            @Override protected Syntax definition() {
                return sequence(LP, EXPRESSION, choice(AND, OR), EXPRESSION, RP);
            }

            @Override public Boolean evaluate(Ast ast) {
                Ast.Sequence sequence = (Ast.Sequence) ast;

                boolean eval1 = ((Boolean) sequence.term(1).evaluate()).booleanValue();
                Syntax symbol = ((Ast.Choice) sequence.term(2)).term().syntax();
                boolean eval2 = ((Boolean) sequence.term(3).evaluate()).booleanValue();

                boolean eval;
                if (symbol == AND) {
                    return new Boolean(eval1 && eval2);
                } else if (symbol == OR) {
                    return new Boolean(eval1 || eval2);
                } else {
                    throw new RuntimeException();
                }
            }
        };

        public static final Syntax.Branch ASSERTION_TERM = new Syntax.Branch("assertion-term") {

            @Override protected Syntax definition() {
                return choice(ISNULL_ASSERTION, IS_ASSERTION, CONTAINS_ASSERTION);
            }

            @Override public Boolean evaluate(Ast ast) {
                Ast.Choice option = (Ast.Choice) ast;
                return (Boolean) option.term().evaluate();
            }
        };

        public static final Syntax.Branch ISNULL_ASSERTION = new Syntax.Branch("isnull-assertion") {

            @Override protected Syntax definition() {
                return sequence(IS_NULL);
            }

            @Override public Boolean evaluate(Ast ast) {
                EvalContext evalContext = evalContext__.get();

                Model model = evalContext.obj;
                Attribute<?, Model> attr = evalContext.attr;
                if (isCollectionType(attr.getType())) {
                    throw new EvaluationException("指定された属性は単数値属性ではありません.");
                }

                Object actualValue = model.get(attr);

                return new Boolean(actualValue == null);
            }
        };

        public static final Syntax.Branch IS_ASSERTION = new Syntax.Branch("is-assertion") {

            @Override protected Syntax definition() {
                return sequence(IS, ANY_STR);
            }

            @Override public Boolean evaluate(Ast ast) {
                EvalContext evalContext = evalContext__.get();
                Ast.Sequence sequence = (Ast.Sequence) ast;

                Model model = evalContext.obj;
                Attribute<?, Model> attr = evalContext.attr;
                if (isCollectionType(attr.getType())) {
                    throw new EvaluationException("指定された属性は単数値属性ではありません.");
                }

                String expectedValueStr = ((Ast.Leaf) sequence.term(1)).token().image();
                Object expectedValue = resolveValue(evalContext.session, model, attr.getType(), expectedValueStr);
                Object actualValue = model.get(attr);

                return new Boolean(expectedValue == null ? actualValue == null : expectedValue.equals(actualValue));
            }
        };

        public static final Syntax.Branch CONTAINS_ASSERTION = new Syntax.Branch("contains-assertion") {

            @Override protected Syntax definition() {
                return sequence(CONTAINS, ANY_STR);
            }

            @Override public Boolean evaluate(Ast ast) {
                EvalContext evalContext = evalContext__.get();
                Ast.Sequence sequence = (Ast.Sequence) ast;

                Model model = evalContext.obj;
                Attribute<?, Model> attr = evalContext.attr;

                String expectedValueStr = ((Ast.Leaf) sequence.term(1)).token().image();
                Object expectedValue = resolveValue(evalContext.session, model, attr.getType(), expectedValueStr);

                if (attr.getType() instanceof AttributeType.MvoCollectionType<?, ?>) {
                    MvoCollection<Object, ?> c = (MvoCollection<Object, ?>) model.get(attr);
                    return new Boolean(c != null && c.contains(expectedValue));
                }

                throw new EvaluationException("指定された属性は複数値属性ではありません.");
            }
        };

        public static final Syntax ANY_STR = new Syntax.Leaf("any-str") {

            @Override protected boolean matches(Token<?> token) {
                return true;
            }
        };
    }

    private static boolean isCollectionType(AttributeType<?> type) {
        return type instanceof AttributeType.MvoCollectionType<?, ?>;
    }

    static class EvalContext {

        final SkeltonShellSession session;
        final Model obj;
        final Attribute<?, Model> attr;

        private Map<Ast, Boolean> evaluatedValues_ = new HashMap<Ast, Boolean>();

        EvalContext(SkeltonShellSession session, Model obj, Attribute<?, ?> attr) {
            this.session = session;
            this.obj = obj;
            this.attr = (Attribute<?, Model>) attr;
        }
    }

    private static final ThreadLocal<EvalContext> evalContext__ = new ThreadLocal<EvalContext>();

    @Override public String getArgumentDescription() {
        return "[attribute name] [evaluation-expression]...";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        Model context = getContext();
        if (context == null) {
            throw new ShellCommandException("コンテキストが指定されていません.");
        }

        checkArgsSize(args, 2, Integer.MAX_VALUE);
        String attrName = args.arg(0);
        List<String> evalExpressionTokens = new ArrayList<String>(args.args());
        evalExpressionTokens.remove(0);

        Attribute<?, ?> attr = Attribute.getAttribute(context.getClass(), attrName);
        if (attr == null) {
            throw new ShellCommandException("属性名を確認してください: " + attrName);
        }

        beginReadTransaction();

        try {
            final EvalContext evalContext = new EvalContext(getSession(), context, attr);
            evalContext__.set(evalContext);

            Boolean evaluationResult
                = (Boolean) Ast.parse(Parser.EXPRESSION, StringToken.newTokenStream(evalExpressionTokens))
                .evaluate();
            if (evaluationResult == null) {
                throw new ShellCommandException("式の評価に失敗しました.");
            }

            if (! evaluationResult.booleanValue()) {
                throw new ShellCommandException("assertion failed.");
            }
        } catch (ParseException pe) {
            throw new ShellCommandException(pe.getMessage());
        } catch (EvaluationException ee) {
            throw new ShellCommandException(ee.getMessage());
        }
    }

    private static Object resolveValue(
        SkeltonShellSession shellsession,
        Model context,
        AttributeType<?> attrType,
        String valueStr)
        throws EvaluationException
    {
        if (attrType == null) {
            throw new EvaluationException("属性型の定義が不足しています.");
        }

        if (attrType instanceof AttributeType.ModelType) {
            try {
                return ObjectResolver.<Object>resolve(attrType.getJavaType(), context, shellsession, valueStr);
            } catch (ResolveException re) {
                throw new EvaluationException(re.getMessage());
            }
        } else if (attrType instanceof AttributeType.MvoCollectionType<?, ?>) {
            return ((AttributeType.MvoCollectionType<?, ?>) attrType).parseElement(valueStr);
        } else if (attrType != null) {
            try {
                return attrType.parse(valueStr);
            } catch (UnsupportedOperationException uoe) {
                throw new EvaluationException("parser が定義されていない属性型です.");
            }
        }

        throw new RuntimeException(attrType.getClass().getName());
    }
}
