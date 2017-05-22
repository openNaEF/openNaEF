package voss.discovery.iolib.snmp;

import net.snmp.VarBind;


public class UnexpectedVarBindException extends Exception {
    private static final long serialVersionUID = 1L;

    public UnexpectedVarBindException(VarBind varbind) {
        super(varbind.getOid().getOidString() + " = " + varbind.getValueAsString());
    }

}