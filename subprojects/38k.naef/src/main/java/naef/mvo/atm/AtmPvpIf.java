package naef.mvo.atm;

import naef.mvo.AbstractPort;
import naef.mvo.NaefMvoUtils;
import naef.mvo.NodeElement;
import tef.skelton.Attribute;
import tef.skelton.ConfigurationException;
import tef.skelton.ValueException;

public class AtmPvpIf extends AbstractPort {

    public static class Attr {

        public static final Attribute.SingleInteger<AtmPvpIf> VPI
            = new Attribute.SingleInteger<AtmPvpIf>("naef.atm.pvp-if.vpi")
        {
            @Override public void validateValue(AtmPvpIf model, Integer vpi)
                throws ValueException, ConfigurationException
            {
                super.validateValue(model, vpi);

                assertAtmEnabled(model.getOwner());

                if (vpi == null) {
                    return;
                }

                if (vpi.intValue() < 0 || 4095 < vpi.intValue()) {
                    throw new ValueException("VPI の範囲を超えています.");
                }

                checkUnique(model, vpi);
            }

            private void assertAtmEnabled(NodeElement obj) throws ConfigurationException {
                if (! Boolean.TRUE.equals(AtmPort.Attr.ATM_ENABLED.get(obj))) {
                    throw new ConfigurationException(
                        obj.getFqn() + " に " + AtmPort.Attr.ATM_ENABLED.getName() + " が設定されていません.");
                }
            }

            private void checkUnique(AtmPvpIf objectPvpif, int objectVpi)
                throws ValueException
            {
                for (AtmPvpIf sibling
                    : NaefMvoUtils.getHereafterSubElements(objectPvpif.getOwner(), AtmPvpIf.class, false))
                {
                    if (sibling == objectPvpif) {
                        continue;
                    }

                    if (sibling.getVpi() == null) {
                        continue;
                    }

                    if (objectVpi == sibling.getVpi().intValue()) {
                        throw new ValueException("割当済のVPIです: " + objectPvpif.getFqn() + ", " + objectVpi);
                    }
                }
            }
        };

        public static final Attribute.SingleInteger<AtmPvpIf> CBR
            = new Attribute.SingleInteger<AtmPvpIf>("naef.atm.pvp-if.cbr");
    }

    public AtmPvpIf(MvoId id) {
        super(id);
    }

    public AtmPvpIf() {
    }

    public Integer getVpi() {
        return get(Attr.VPI);
    }
}
