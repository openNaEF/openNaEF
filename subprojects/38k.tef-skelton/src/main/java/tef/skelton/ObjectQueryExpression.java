package tef.skelton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lib38k.parser.Ast;
import lib38k.parser.ParseException;
import lib38k.parser.Syntax;
import lib38k.parser.Token;
import lib38k.parser.TokenStream;
import tef.MVO;
import tef.TefService;

public class ObjectQueryExpression {

    public static class EvaluationException extends KnownRuntimeException {

        public EvaluationException(String message) {
            super(message);
        }
    }

    public static class Row {

        private List<AbstractModel> columns_ = new ArrayList<AbstractModel>();

        public Row(AbstractModel... columns) {
            this(Arrays.asList(columns));
        }

        public Row(List<AbstractModel> columns) {
            for (AbstractModel column : columns) {
                columns_.add(column);
            }
        }

        public List<AbstractModel> columns() {
            return columns_;
        }

        void addColumn(AbstractModel column) {
            columns_.add(column);
        }

        AbstractModel lastColumn() {
            return columns_.size() == 0 ? null : columns_.get(columns_.size() - 1);
        }
    }

    private static final class Context {

        List<Row> rows;
        Object object;
        Object conditionClauseValue;
    }

    public interface CollectionValueAccess {

        public Collection<?> get(Object o);
    }

    private static final Map<String, CollectionValueAccess> collectionValueAccesses__
        = new HashMap<String, CollectionValueAccess>();

    public static void installCollectionValueAccess(String name, CollectionValueAccess cva) {
        collectionValueAccesses__.put(name, cva);
    }

    public interface SingleValueAccess {

        public Object get(Object o);
    }

    private static final Map<String, SingleValueAccess> singleValueAccesses__
        = new HashMap<String, SingleValueAccess>();

    public static void installSingleValueAccess(String name, SingleValueAccess sva) {
        singleValueAccesses__.put(name, sva);
    }

    private static final ThreadLocal<Context> context__ = new ThreadLocal<Context>();


    public static final Syntax.Leaf.Symbol EQUAL = new Syntax.Leaf.Symbol("=");
    public static final Syntax.Leaf.Symbol DOT = new Syntax.Leaf.Symbol(".");
    public static final Syntax.Leaf.Symbol DOTDOT = new Syntax.Leaf.Symbol("..");
    public static final Syntax.Leaf.Symbol COLON = new Syntax.Leaf.Symbol(":");
    public static final Syntax.Leaf.Symbol COLONCOLON = new Syntax.Leaf.Symbol("::");
    public static final Syntax.Leaf.Symbol LCB = new Syntax.Leaf.Symbol("{");
    public static final Syntax.Leaf.Symbol RCB = new Syntax.Leaf.Symbol("}");
    public static final Syntax.Leaf.Symbol LP = new Syntax.Leaf.Symbol("(");
    public static final Syntax.Leaf.Symbol RP = new Syntax.Leaf.Symbol(")");
    public static final Syntax.Leaf.Symbol HYPHENGT = new Syntax.Leaf.Symbol("->");

    public static final Syntax.Leaf.Symbol ALL = new Syntax.Leaf.Symbol("all");
    public static final Syntax.Leaf.Symbol AND = new Syntax.Leaf.Symbol("and");
    public static final Syntax.Leaf.Symbol OR = new Syntax.Leaf.Symbol("or");
    public static final Syntax.Leaf.Symbol NOT = new Syntax.Leaf.Symbol("not");
    public static final Syntax.Leaf.Symbol PARTIAL_MATCH = new Syntax.Leaf.Symbol("partial-match");
    public static final Syntax.Leaf.Symbol REGEXP_MATCH = new Syntax.Leaf.Symbol("regexp-match");
    public static final Syntax.Leaf.Symbol IS_NULL = new Syntax.Leaf.Symbol("is-null");

    public static final Syntax.Branch COLLECTION = new Syntax.Branch("collection") {

        @Override protected Syntax definition() {
            return choice(BASE_COLLECTION, NESTED_COLLECTION);
        }

        @Override public List<Row> evaluate(Ast ast) {
            context__.set(new Context());

            Ast.Choice choice = (Ast.Choice) ast;

            return (List<Row>) choice.term().evaluate();
        }
    };

    public static final Syntax.Branch BASE_COLLECTION = new Syntax.Branch("base-collection") {

        @Override protected Syntax definition() {
            return sequence(ALL, TYPE_NAME);
        }

        @Override public List<Row> evaluate(Ast ast) {
            Ast.Sequence sequence = (Ast.Sequence) ast;

            String typenameStr = (String) sequence.term(1).evaluate();

            List<Row> result = new ArrayList<Row>();
            for (Object o : getInstances(typenameStr)) {
                result.add(new Row((AbstractModel) o));
            }
            return result;
        }

        private List<?> getInstances(String typenameStr) {
            Class<?> type = resolveType(typenameStr);

            Resolver<?> resolver = SkeltonTefService.instance().getResolver(type);
            if (resolver != null && resolver instanceof UniquelyNamedModelResolver<?>) {
                UniquelyNamedModelHome<?> home = ((UniquelyNamedModelResolver<?>) resolver).getHome();
                return home.list();
            }

            return TefService.instance().getMvoRegistry().select((Class<? extends MVO>) type);
        }

        private Class<?> resolveType(String typenameStr) {
            UiTypeName typename = SkeltonTefService.instance().uiTypeNames().getByName(typenameStr);
            if (typename != null) {
                return typename.type();
            }

            try {
                Class<?> result = Class.forName(typenameStr);
                if (! AbstractModel.class.isAssignableFrom(result)) {
                    throw new EvaluationException("unsupported type: " + typenameStr);
                }
                return result;
            } catch (ClassNotFoundException cnfe) {
                throw new EvaluationException("no such type: " + typenameStr);
            }
        }
    };

    public static final Syntax.Branch NESTED_COLLECTION = new Syntax.Branch("nested-collection") {

        @Override protected Syntax definition() {
            return sequence(LCB, COLLECTION, RCB, choice(TRAVERSE, NARROWING));
        }

        @Override public List<Row> evaluate(Ast ast) {
            Ast.Sequence sequence = (Ast.Sequence) ast;

            List<Row> rows = (List<Row>) sequence.term(1).evaluate();

            context__.get().rows = rows;

            return (List<Row>) sequence.term(3).evaluate();
        }
    };

    public static final Syntax.Branch TRAVERSE = new Syntax.Branch("traverse") {

        @Override protected Syntax definition() {
                return sequence(HYPHENGT, choice(SINGLE_ACCESS, COLLECTION_ACCESS));
            }

            @Override public List<Row> evaluate(Ast ast) {
                Ast.Sequence sequence = (Ast.Sequence) ast;

                Syntax accessType = sequence.term(1).syntax();

                List<Row> result = new ArrayList<Row>();
                for (Row row : context__.get().rows) {
                    AbstractModel obj = row.lastColumn();

                    context__.get().object = obj;

                    if (accessType == SINGLE_ACCESS) {
                        Object value = sequence.term(1).evaluate();
                        if (value != null) {
                            result.add(widening(row, value));
                        }
                    } else if (accessType == COLLECTION_ACCESS) {
                        Collection<?> elems = (Collection<?>) sequence.term(1).evaluate();
                        if (elems != null) {
                            for (Object elem : elems) {
                                result.add(widening(row, elem));
                            }
                        }
                    } else {
                        throw new RuntimeException();
                    }
                }
                return result;
            }

            private Row widening(Row originalRow, Object columnToAdd) {
                Row widedRow = new Row(originalRow.columns());
                widedRow.addColumn((AbstractModel) columnToAdd);
                return widedRow;
            }
        };

    public static final Syntax.Branch NARROWING = new Syntax.Branch("narrowing") {

        @Override protected Syntax definition() {
                return sequence(CONDITIONAL_EXPRESSION);
            }

            @Override public List<Row> evaluate(Ast ast) {
                Ast.Sequence sequence = (Ast.Sequence) ast;

                List<Row> result = new ArrayList<Row>();
                for (Row row : context__.get().rows) {
                    context__.get().object = row.lastColumn();

                    Boolean matches = (Boolean) sequence.term(0).evaluate();
                    if (matches.booleanValue()) {
                        result.add(row);
                    }
                }
                return result;
            }
        };

    public static final Syntax.Branch CONDITIONAL_EXPRESSION = new Syntax.Branch("conditional-expression") {

        @Override protected Syntax definition() {
            return choice(NEGATIVE_CONDITIONAL_EXPRESSION, POSITIVE_CONDITIONAL_EXPRESSION);
        }

        @Override public Boolean evaluate(Ast ast) {
            return (Boolean) ((Ast.Choice) ast).term().evaluate();
        }
    };

    public static final Syntax.Branch NEGATIVE_CONDITIONAL_EXPRESSION
        = new Syntax.Branch("negative-conditional-expression")
    {
        @Override protected Syntax definition() {
            return sequence(NOT, CONDITIONAL_EXPRESSION);
        }

        @Override public Boolean evaluate(Ast ast) {
            return ((Boolean) ((Ast.Sequence) ast).term(1).evaluate()).booleanValue()
                ? Boolean.FALSE
                : Boolean.TRUE;
        }
    };

    public static final Syntax.Branch POSITIVE_CONDITIONAL_EXPRESSION
        = new Syntax.Branch("positive-conditional-expression")
    {
        @Override protected Syntax definition() {
            return choice(CONDITIONAL_TERM, CONDITIONAL_LOGICAL_EXPRESSION);
        }

        @Override public Boolean evaluate(Ast ast) {
            return (Boolean) ((Ast.Choice) ast).term().evaluate();
        }
    };

    public static final Syntax.Branch CONDITIONAL_TERM = new Syntax.Branch("conditional-term") {

        @Override protected Syntax definition() {
            return choice(SINGLE_VALUE_CONDITION, COLLECTION_VALUE_CONDITION);
        }

        @Override public Boolean evaluate(Ast ast) {
            return (Boolean) ((Ast.Choice) ast).term().evaluate();
        }
    };

    public static final Syntax.Branch CONDITIONAL_LOGICAL_EXPRESSION
        = new Syntax.Branch("conditional-logical-expression")
    {
        @Override protected Syntax definition() {
            return sequence(LP, CONDITIONAL_EXPRESSION, choice(AND, OR), CONDITIONAL_EXPRESSION, RP);
        }

        @Override public Boolean evaluate(Ast ast) {
            Ast.Sequence sequence = (Ast.Sequence) ast;

            Boolean eval1 = (Boolean) sequence.term(1).evaluate();
            Syntax operator = sequence.term(2).syntax();
            Boolean eval2 = (Boolean) sequence.term(3).evaluate();

            if (operator == AND) {
                return eval1.booleanValue() && eval2.booleanValue()
                    ? Boolean.TRUE : Boolean.FALSE;
            } else if (operator == OR) {
                return eval1.booleanValue() || eval2.booleanValue()
                    ? Boolean.TRUE : Boolean.FALSE;
            } else {
                throw new RuntimeException();
            }
        }
    };

    public static final Syntax.Branch SINGLE_VALUE_CONDITION = new Syntax.Branch("single-value-condition") {

        @Override protected Syntax definition() {
            return sequence(SINGLE_ACCESS, CONDITION_CLAUSE);
        }

        @Override public Boolean evaluate(Ast ast) {
            Ast.Sequence sequence = (Ast.Sequence) ast;

            Object actualValue = sequence.term(0).evaluate();
            context__.get().conditionClauseValue = actualValue;

            return (Boolean) sequence.term(1).evaluate();
        }
    };

    public static final Syntax.Branch COLLECTION_VALUE_CONDITION = new Syntax.Branch("collection-value-condition") {

        @Override protected Syntax definition() {
            return sequence(COLLECTION_ACCESS, CONDITION_CLAUSE);
        }

        @Override public Boolean evaluate(Ast ast) {
            Ast.Sequence sequence = (Ast.Sequence) ast;

            Collection<?> elems = (Collection<?>) sequence.term(0).evaluate();
            if (elems == null) {
                return false;
            }

            for (Object elem : elems) {
                context__.get().conditionClauseValue = elem;

                boolean eval = ((Boolean) sequence.term(1).evaluate()).booleanValue();
                if (eval) {
                    return true;
                }
            }
            return false;
        }
    };

    public static final Syntax.Branch CONDITION_CLAUSE = new Syntax.Branch("condition-clause") {

        @Override protected Syntax definition() {
            return choice(UNARY_CONDITION_CLAUSE, BINARY_CONDITION_CLAUSE);
        }

        @Override public Boolean evaluate(Ast ast) {
            return (Boolean) ((Ast.Choice) ast).term().evaluate();
        }
    };

    public static final Syntax.Branch UNARY_CONDITION_CLAUSE = new Syntax.Branch("unary-condition-clause") {

        @Override protected Syntax definition() {
            return choice(IS_NULL);
        }

        @Override public Boolean evaluate(Ast ast) {
            Syntax op = ((Ast.Choice) ast).term().syntax();

            if (op == IS_NULL) {
                return context__.get().conditionClauseValue == null;
            } else {
                throw new RuntimeException();
            }
        }
    };

    public static final Syntax.Branch BINARY_CONDITION_CLAUSE = new Syntax.Branch("binary-condition-clause") {

        @Override protected Syntax definition() {
            return sequence(choice(EQUAL, PARTIAL_MATCH, REGEXP_MATCH), ANY_STR);
        }

        @Override public Boolean evaluate(Ast ast) {
            Ast.Sequence sequence = (Ast.Sequence) ast;

            Syntax operator = ((Ast.Choice) sequence.term(0)).term().syntax();
            String rightValueStr = (String) sequence.term(1).evaluate();

            String leftValueStr = toStr(context__.get().conditionClauseValue);

            return evaluateBinaryOperation(operator, leftValueStr, rightValueStr)
                ? Boolean.TRUE
                : Boolean.FALSE;
        }
    };

    public static final Syntax.Branch SINGLE_ACCESS = new Syntax.Branch("single-access") {

        @Override protected Syntax definition() {
            return choice(SINGLE_ATTR_ACCESS, SINGLE_FUNC_ACCESS);
        }

        @Override public Object evaluate(Ast ast) {
            return ((Ast.Choice) ast).term().evaluate();
        }
    };

    public static final Syntax.Branch SINGLE_ATTR_ACCESS = new Syntax.Branch("single-attr-access") {

        @Override protected Syntax definition() {
            return sequence(DOT, ATTR_NAME);
        }

        @Override public Object evaluate(Ast ast) {
            Ast.Sequence sequence = (Ast.Sequence) ast;

            String attrname = (String) sequence.term(1).evaluate();

            Model object = (Model) context__.get().object;
            Attribute<?, Model> attr = (Attribute<?, Model>) getAttribute(object.getClass(), attrname);
            if (isCollectionAttribute(attr)) {
                throw new EvaluationException(
                    "the attribute is collection type: "
                    + SkeltonTefService.instance().uiTypeNames().getName(object.getClass()) + ", " + attrname);
            }

            return object.get(attr);
        }
    };

    public static final Syntax.Branch SINGLE_FUNC_ACCESS = new Syntax.Branch("single-func-access") {

        @Override protected Syntax definition() {
            return sequence(DOTDOT, FUNC_NAME);
        }

        @Override public Object evaluate(Ast ast) {
            Ast.Sequence sequence = (Ast.Sequence) ast;

            String funcname = (String) sequence.term(1).evaluate();

            SingleValueAccess sva = singleValueAccesses__.get(funcname);
            if (sva == null) {
                throw new EvaluationException("no such function: " + funcname);
            }

            return sva.get(context__.get().object);
        }
    };

    public static final Syntax.Branch COLLECTION_ACCESS = new Syntax.Branch("collection-access") {

        @Override protected Syntax definition() {
            return choice(COLLECTION_ATTR_ACCESS, COLLECTION_FUNC_ACCESS);
        }

        @Override public Collection<?> evaluate(Ast ast) {
            return (Collection<?>) ((Ast.Choice) ast).term().evaluate();
        }
    };

    public static final Syntax.Branch COLLECTION_ATTR_ACCESS = new Syntax.Branch("collection-attr-access") {

        @Override protected Syntax definition() {
            return sequence(COLON, ATTR_NAME);
        }

        @Override public Collection<?> evaluate(Ast ast) {
            Ast.Sequence sequence = (Ast.Sequence) ast;

            String attrname = (String) sequence.term(1).evaluate();

            AbstractModel obj = (AbstractModel) context__.get().object;
            Attribute<?, Model> attr = (Attribute<?, Model>) getAttribute(obj.getClass(), attrname);
            if (! isCollectionAttribute(attr)) {
                throw new EvaluationException(
                    "the attribute is not collection type: "
                    + SkeltonTefService.instance().uiTypeNames().getName(obj.getClass()) + ", " + attrname);
            }

            MvoCollection<?, ?> mvocollection = (MvoCollection<?, ?>) obj.get(attr);
            return mvocollection == null ? null : mvocollection.get();
        }
    };

    public static final Syntax.Branch COLLECTION_FUNC_ACCESS = new Syntax.Branch("collection-func-access") {

        @Override protected Syntax definition() {
            return sequence(COLONCOLON, FUNC_NAME);
        }

        @Override public Collection<?> evaluate(Ast ast) {
            Ast.Sequence sequence = (Ast.Sequence) ast;

            String funcname = (String) sequence.term(1).evaluate();

            CollectionValueAccess cva = collectionValueAccesses__.get(funcname);
            if (cva == null) {
                throw new EvaluationException("no such function: " + funcname);
            }

            return cva.get(context__.get().object);
        }
    };

    public static final Syntax.Leaf TYPE_NAME = new Syntax.Leaf("type-name") {

        @Override protected boolean matches(Token<?> token) {
            return true;
        }
    };

    public static final Syntax.Leaf ATTR_NAME = new Syntax.Leaf("attr-name") {

        @Override protected boolean matches(Token<?> token) {
            return true;
        }
    };

    public static final Syntax.Leaf FUNC_NAME = new Syntax.Leaf("func-name") {

        @Override protected boolean matches(Token<?> token) {
            return true;
        }
    };

    public static final Syntax.Leaf ANY_STR = new Syntax.Leaf("any-str") {

        @Override protected boolean matches(Token<?> token) {
            return true;
        }
    };

    private static String toStr(Object o) {
        return o == null ? null : o.toString();
    }

    private static boolean evaluateBinaryOperation(Syntax operator, String val1, String val2) {
        if (operator == EQUAL) {
            return val1 == null ? val2 == null : val1.equals(val2);
        } else if (operator == PARTIAL_MATCH) {
            return val1 != null && val1.contains(val2);
        } else if (operator == REGEXP_MATCH) {
            return val1 != null && val1.matches(val2);
        } else {
            throw new RuntimeException();
        }
    }

    private static Attribute<?, ?> getAttribute(Class<? extends Model> klass, String attrname) {
        Attribute<?, ?> result = Attribute.getAttribute(klass, attrname);
        if (result == null) {
            throw new EvaluationException(
                "no such attribute: "
                + SkeltonTefService.instance().uiTypeNames().getName(klass) + ", " + attrname);
        }
        return result;
    }

    private static boolean isCollectionAttribute(Attribute<?, ?> attr) {
        return attr.getType() instanceof AttributeType.MvoCollectionType<?, ?>;
    }
}
