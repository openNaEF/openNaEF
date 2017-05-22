package tef.skelton.fqn;

import java.util.List;

public class Fqn {

    public static class Single extends Fqn {

        private final List<Term> terms_;

        public Single(List<Term> terms) {
            terms_ = terms;
        }

        public List<Term> terms() {
            return terms_;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            for (Term term : terms_) {
                result.append(result.length() == 0 ? "" : " ");
                result.append(term.toString());
            }
            return result.toString();
        }
    }

    public static class Clustering extends Fqn {

        private final String typeName_;
        private final List<Single> elements_;

        public Clustering(String typeName, List<Single> elements) {
            typeName_ = typeName;
            elements_ = elements;
        }

        public String typeName() {
            return typeName_;
        }

        public List<Single> elements() {
            return elements_;
        }

        @Override
        public String toString() {
            StringBuilder elementsStr = new StringBuilder();
            for (Single element : elements_) {
                elementsStr.append(elementsStr.length() == 0 ? "" : ",");
                elementsStr.append(element.toString());
            }
            return typeName_ + "{" + elementsStr.toString() + "}";
        }
    }
}
