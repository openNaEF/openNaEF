package tef.skelton.fqn;

public class Term {

    public final String typeName;
    public final String objectName;

    public Term(String typeName, String objectName) {
        this.typeName = typeName;
        this.objectName = objectName;
    }

    @Override public String toString() {
        return typeName + "=" + objectName;
    }
}
