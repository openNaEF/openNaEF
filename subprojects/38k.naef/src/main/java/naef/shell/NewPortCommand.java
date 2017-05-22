package naef.shell;

import naef.mvo.NodeElement;
import naef.mvo.Port;
import tef.skelton.ConstraintException;

public class NewPortCommand extends NaefShellCommand {

    @Override public String getArgumentDescription() {
        return "[port type] [name]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 2);

        Class<?> type = resolvePortTypeName(args.arg(0)).type();
        String name = args.arg(1);

        beginWriteTransaction();

        Port port;
        try {
            port = newPort(type, name);
        } catch (ConstraintException ce) {
            throw new ShellCommandException(ce.getMessage());
        }

        setContext(port, port.getFqn());

        commitTransaction();
    }

    private Port newPort(Class<?> type, String name)
        throws ConstraintException, ShellCommandException 
    {
        Port port;
        NodeElement owner;
        if (type == naef.mvo.InterconnectionIf.class) {
            port = new naef.mvo.InterconnectionIf();
            owner = contextAsNode();
        } else if (type == naef.mvo.atm.AtmApsIf.class) {
            port = new naef.mvo.atm.AtmApsIf();
            owner = contextAsNodeElement();
        } else if (type == naef.mvo.atm.AtmPort.class) {
            port = new naef.mvo.atm.AtmPort();
            owner = contextAsJack();
        } else if (type == naef.mvo.atm.AtmPvcIf.class) {
            port = new naef.mvo.atm.AtmPvcIf();
            owner = contextAs(naef.mvo.atm.AtmPvpIf.class, "atm-pvp");
        } else if (type == naef.mvo.atm.AtmPvpIf.class) {
            port = new naef.mvo.atm.AtmPvpIf();
            owner = contextAsPort();
        } else if (type == naef.mvo.eth.EthPort.class) {
            port = new naef.mvo.eth.EthPort();
            owner = contextAsNodeElement();
        } else if (type == naef.mvo.eth.EthLagIf.class) {
            port = new naef.mvo.eth.EthLagIf();
            owner = contextAsNode();
        } else if (type == naef.mvo.fr.FrPvcIf.class) {
            port = new naef.mvo.fr.FrPvcIf();
            owner = contextAsNodeElement();
        } else if (type == naef.mvo.ip.IpIf.class) {
            port = new naef.mvo.ip.IpIf();
            owner = contextAsNodeElement();
        } else if (type == naef.mvo.isdn.IsdnChannelIf.class) {
            port = new naef.mvo.isdn.IsdnChannelIf();
            owner = contextAsNodeElement();
        } else if (type == naef.mvo.isdn.IsdnPort.class) {
            port = new naef.mvo.isdn.IsdnPort();
            owner = contextAsNodeElement();
        } else if (type == naef.mvo.pos.PosApsIf.class) {
            port = new naef.mvo.pos.PosApsIf();
            owner = contextAsNodeElement();
        } else if (type == naef.mvo.pos.PosPort.class) {
            port = new naef.mvo.pos.PosPort();
            owner = contextAsJack();
        } else if (type == naef.mvo.serial.SerialPort.class) {
            port = new naef.mvo.serial.SerialPort();
            owner = contextAsNodeElement();
        } else if (type == naef.mvo.serial.TdmSerialIf.class) {
            port = new naef.mvo.serial.TdmSerialIf();
            owner = contextAsNodeElement();
        } else if (type == naef.mvo.vlan.VlanIf.class) {
            port = new naef.mvo.vlan.VlanIf();
            owner = contextAsNodeElement();
        } else if (type == naef.mvo.vlan.VlanSegmentGatewayIf.class) {
            port = new naef.mvo.vlan.VlanSegmentGatewayIf();
            owner = contextAsNodeElement();
        } else if (type == naef.mvo.vpls.VplsIf.class) {
            port = new naef.mvo.vpls.VplsIf();
            owner = contextAsNode();
        } else if (type == naef.mvo.vrf.VrfIf.class) {
            port = new naef.mvo.vrf.VrfIf();
            owner = contextAsNode();
        } else if (type == naef.mvo.vxlan.VtepIf.class) {
            port = new naef.mvo.vxlan.VtepIf();
            owner = contextAsNode();
        } else if (type == naef.mvo.wdm.WdmPort.class) {
            port = new naef.mvo.wdm.WdmPort();
            owner = contextAsJack();
        } else {
            throw new ShellCommandException("サポートされていない型です.");
        }

        port.setName(name);
        port.setOwner(owner);

        return port;
    }
}
