package tef.skelton.fqn;

import lib38k.parser.Ast;
import lib38k.parser.ParseException;
import lib38k.parser.ScannerToken;
import lib38k.parser.Syntax;
import lib38k.parser.Token;
import lib38k.parser.TokenStream;

public class FqnSyntax {

    public static Fqn parse(String delimiter1, String delimiter2, String delimiter3, String fqn)
        throws ParseException
    {
        return ((FqnAst.Root) Ast.parse(new FqnSyntax(delimiter1, delimiter2, delimiter3).ROOT, getTokenStream(fqn)))
            .buildFqn();
    }

    private static TokenStream getTokenStream(String str) {
        return ScannerToken.newTokenStream(str);
    }

    public final Syntax.Leaf.Symbol DELIMITER1;
    public final Syntax.Leaf.Symbol DELIMITER2;
    public final Syntax.Leaf.Symbol DELIMITER3;
    public final Syntax.Leaf.Symbol LEFT_BRACKET = new Syntax.Leaf.Symbol("{");
    public final Syntax.Leaf.Symbol RIGHT_BRACKET = new Syntax.Leaf.Symbol("}");
    public final Syntax.Leaf.Symbol ESCAPE_CHAR = new Syntax.Leaf.Symbol("\\");

    public final Syntax.Branch ROOT = new Syntax.Branch(FqnAst.Root.class) {

        @Override protected Syntax definition() {
            return choice(
                SINGLE_FQN,
                CLUSTERING_FQN);
        }
    };

    public final Syntax.Branch SINGLE_FQN = new Syntax.Branch(FqnAst.SingleFqn.class) {

        @Override protected Syntax definition() {
            return sequence(
                TERM,
                optional(repeat(sequence(DELIMITER2, TERM))));
        }
    };

    public final Syntax.Branch CLUSTERING_FQN = new Syntax.Branch(FqnAst.ClusteringFqn.class) {

        @Override protected Syntax definition() {
            return sequence(TYPE_NAME, DELIMITER1, LEFT_BRACKET, CLUSTER, RIGHT_BRACKET);
        }
    };

    public final Syntax.Branch CLUSTER = new Syntax.Branch(FqnAst.Cluster.class) {

        @Override protected Syntax definition() {
            return sequence(
                SINGLE_FQN,
                optional(repeat(sequence(DELIMITER3, SINGLE_FQN))));
        }
    };

    public final Syntax.Branch TERM = new Syntax.Branch(FqnAst.AstTerm.class) {

        @Override protected Syntax definition() {
            return choice(
                TERM_WITH_TYPE_NAME,
                SIMPLE_TERM);
        }
    };

    public final Syntax.Branch TERM_WITH_TYPE_NAME = new Syntax.Branch(FqnAst.AstTermWithTypeName.class) {

        @Override protected Syntax definition() {
            return sequence(TYPE_NAME, DELIMITER1, optional(OBJECT_NAME));
        }
    };

    public final Syntax.Branch SIMPLE_TERM = new Syntax.Branch(FqnAst.AstSimpleTerm.class) {

        @Override protected Syntax definition() {
            return sequence(OBJECT_NAME);
        }
    };

    public final Syntax.Branch TYPE_NAME = new Syntax.Branch("type-name") {

        @Override protected Syntax definition() {
            return sequence(NAME);
        }
    };

    public final Syntax.Branch OBJECT_NAME = new Syntax.Branch("object-name") {

        @Override protected Syntax definition() {
            return sequence(NAME);
        }
    };

    public final Syntax.Branch NAME = new Syntax.Branch("name") {

        @Override protected Syntax definition() {
            return repeat(
                sequence(LETTER)
                    .except(choice(DELIMITER1, DELIMITER2, DELIMITER3, LEFT_BRACKET, RIGHT_BRACKET)));
        }
    };

    public final Syntax.Leaf LETTER = new Syntax.Leaf("any") {

        @Override protected boolean matches(Token<?> token) {
            return true;
        }
    };

    public FqnSyntax(String delimiter1, String delimiter2, String delimiter3) {
        DELIMITER1 = new Syntax.Leaf.Symbol(delimiter1);
        DELIMITER2 = new Syntax.Leaf.Symbol(delimiter2);
        DELIMITER3 = new Syntax.Leaf.Symbol(delimiter3);
    }
}
