package tef.skelton.fqn;

import lib38k.parser.Ast;
import lib38k.parser.Syntax;

import java.util.ArrayList;
import java.util.List;

public class FqnAst {

    public static class Root extends Ast.Syntactic {

        public Root(Syntax.Branch syntax, Ast contents) {
            super(syntax, contents);
        }

        public Fqn buildFqn() {
            Ast contents = wrapper().choice().ast;
            if (contents instanceof SingleFqn) {
                return ((SingleFqn) contents).buildFqn();
            } else if (contents instanceof ClusteringFqn) {
                return ((ClusteringFqn) contents).buildFqn();
            } else {
                throw new RuntimeException();
            }
        }
    }

    public static class SingleFqn extends Ast.Syntactic {

        public SingleFqn(Syntax.Branch syntax, Ast contents) {
            super(syntax, contents);
        }

        public List<AstTerm> getAstTerms() {
            List<AstTerm> result = new ArrayList<AstTerm>();
            result.add(wrapper().seq(0, AstTerm.class));
            for (Ast.Wrapper.Single term : wrapper().seq(1).opt().rep().terms()) {
                result.add(term.seq(1, AstTerm.class));
            }
            return result;
        }

        public Fqn.Single buildFqn() {
            List<Term> terms = new ArrayList<Term>();
            for (AstTerm astterm : getAstTerms()) {
                terms.add(astterm.getTerm());
            }
            return new Fqn.Single(terms);
        }
    }

    public static class ClusteringFqn extends Ast.Syntactic {

        public ClusteringFqn(Syntax.Branch syntax, Ast contents) {
            super(syntax, contents);
        }

        public String getTypeName() {
            return wrapper().seq(0).toTokensStr();
        }

        public Cluster getCluster() {
            return wrapper().seq(3, Cluster.class);
        }

        public Fqn.Clustering buildFqn() {
            return new Fqn.Clustering(getTypeName(), getCluster().buildSingleFqns());
        }
    }

    public static class Cluster extends Ast.Syntactic {

        public Cluster(Syntax.Branch syntax, Ast contents) {
            super(syntax, contents);
        }

        public List<SingleFqn> getSingleFqns() {
            List<SingleFqn> result = new ArrayList<SingleFqn>();
            result.add(wrapper().seq(0, SingleFqn.class));
            for (Ast.Wrapper.Single term : wrapper().seq(1).opt().rep().terms()) {
                result.add(term.seq(1, SingleFqn.class));
            }
            return result;
        }

        public List<Fqn.Single> buildSingleFqns() {
            List<Fqn.Single> result = new ArrayList<Fqn.Single>();
            for (SingleFqn single : getSingleFqns()) {
                result.add(single.buildFqn());
            }
            return result;
        }
    }

    public static class AstTerm extends Ast.Syntactic {

        public AstTerm(Syntax.Branch syntax, Ast contents) {
            super(syntax, contents);
        }

        public Term getTerm() {
            Ast contents = wrapper().choice().ast;
            if (contents instanceof AstTermWithTypeName) {
                return ((AstTermWithTypeName) contents).buildTerm();
            } else if (contents instanceof AstSimpleTerm) {
                return ((AstSimpleTerm) contents).buildTerm();
            } else {
                throw new RuntimeException();
            }
        }
    }

    public static class AstTermWithTypeName extends Ast.Syntactic {

        public AstTermWithTypeName(Syntax.Branch syntax, Ast contents) {
            super(syntax, contents);
        }

        public Term buildTerm() {
            String typename = wrapper().seq(0).toTokensStr();
            Ast objnameAst = wrapper().seq(2).opt().ast;
            return new Term(typename, objnameAst == null ? "" : objnameAst.getTokenImage());
        }
    }

    public static class AstSimpleTerm extends Ast.Syntactic {

        public AstSimpleTerm(Syntax.Branch syntax, Ast contents) {
            super(syntax, contents);
        }

        public Term buildTerm() {
            return new Term(null, wrapper().seq(0).toTokensStr());
        }
    }
}
