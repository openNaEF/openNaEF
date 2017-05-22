package naef.mvo.fr;

import naef.mvo.AbstractPort;
import naef.mvo.NaefMvoUtils;
import naef.mvo.NodeElement;
import tef.skelton.Attribute;
import tef.skelton.ConfigurationException;
import tef.skelton.ValueException;

public class FrPvcIf extends AbstractPort {

    public static class Attr {

        public static final Attribute.SingleInteger<FrPvcIf> DLCI
            = new Attribute.SingleInteger<FrPvcIf>("naef.fr.fr-if.dlci")
        {
            @Override public void validateValue(FrPvcIf model, Integer dlci)
                throws ValueException, ConfigurationException
            {
                super.validateValue(model, dlci);

                assertFrEnabled(model.getOwner());

                if (dlci == null) {
                    return;
                }

                checkDlciDuplication(model, dlci);
            }
        };

        private static void assertFrEnabled(NodeElement obj) throws ConfigurationException {
            if (FrPvcType.ATTRIBUTE.get(obj) == null) {
                throw new ConfigurationException(
                    obj.getFqn() + " に " + FrPvcType.ATTRIBUTE.getName() + " が設定されていません.");
            }
        }

        private static void checkDlciDuplication(FrPvcIf objectFrpvcif, int objectDlci)
            throws ValueException 
        {
            for (FrPvcIf sibling
                : NaefMvoUtils.getHereafterSubElements(objectFrpvcif.getOwner(), FrPvcIf.class, false))
            {
                if (sibling == objectFrpvcif) {
                    continue;
                }

                Integer siblingDlci = sibling.getDlci();
                if (siblingDlci == null) {
                    continue;
                }

                if (objectDlci == siblingDlci.intValue()) {
                    throw new ValueException("割当済のDLCIです.");
                }
            }
        }
    }

    public FrPvcIf(MvoId id) {
        super(id);
    }

    public FrPvcIf() {
    }

    public Integer getDlci() {
        return get(Attr.DLCI);
    }
}
