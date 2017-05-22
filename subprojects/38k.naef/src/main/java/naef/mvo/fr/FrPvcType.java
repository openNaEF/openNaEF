package naef.mvo.fr;

import tef.skelton.Attribute;
import tef.skelton.Model;

public enum FrPvcType {

    ANSI("ANSI"), CISCO("Cisco"), Q933A("Q933a");

    public static final Attribute.SingleEnum<FrPvcType, Model> ATTRIBUTE
        = new Attribute.SingleEnum<FrPvcType, Model>("naef.enabled-networking-function.fr.pvc", FrPvcType.class);

    private final String displayName_;

    FrPvcType(String displayName) {
        displayName_ = displayName;
    }

    @Override public String toString() {
        return displayName_;
    }
}
