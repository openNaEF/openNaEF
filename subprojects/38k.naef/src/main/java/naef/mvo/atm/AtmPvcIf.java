package naef.mvo.atm;

import naef.mvo.AbstractPort;
import naef.mvo.NaefMvoUtils;
import tef.skelton.Attribute;
import tef.skelton.ConfigurationException;
import tef.skelton.ValueException;

public class AtmPvcIf extends AbstractPort {

    public static class Attr {

        public static final Attribute.SingleInteger<AtmPvcIf> VCI
            = new Attribute.SingleInteger<AtmPvcIf>("naef.atm.pvc-if.vci")
        {
            @Override public void validateValue(AtmPvcIf model, Integer vci)
                throws ValueException, ConfigurationException
            {
                super.validateValue(model, vci);

                if (vci == null) {
                    return;
                }

                if (vci.intValue() < 0 || 65535 < vci.intValue()) {
                    throw new ValueException("VCI の範囲を超えています.");
                }

                checkUnique(model, vci);
            }

            private void checkUnique(AtmPvcIf objectPvcif, int objectVci)
                throws ValueException
            {
                for (AtmPvcIf sibling
                    : NaefMvoUtils.getHereafterSubElements(objectPvcif.getOwner(), AtmPvcIf.class, false))
                {
                    if (sibling == objectPvcif) {
                        continue;
                    }

                    if (sibling.getVci() == null) {
                        continue;
                    }

                    if (objectVci == sibling.getVci().intValue()) {
                        throw new ValueException("割当済のVCIです: " + objectPvcif.getFqn() + ", " + objectVci);
                    }
                }
            }
        };

        public static final Attribute.SingleInteger<AtmPvcIf> CBR
            = new Attribute.SingleInteger<AtmPvcIf>("naef.atm.pvc-if.cbr");
    }

    public AtmPvcIf(MvoId id) {
        super(id);
    }

    public AtmPvcIf() {
    }

    public Integer getVci() {
        return get(Attr.VCI);
    }
}
