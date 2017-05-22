package naef.shell;

import naef.mvo.L1Link;
import naef.mvo.Network;
import naef.mvo.P2pLink;
import naef.mvo.Port;
import naef.mvo.atm.AtmApsIf;
import naef.mvo.atm.AtmApsLink;
import naef.mvo.atm.AtmLink;
import naef.mvo.atm.AtmPvc;
import naef.mvo.atm.AtmPvcIf;
import naef.mvo.atm.AtmPvp;
import naef.mvo.atm.AtmPvpIf;
import naef.mvo.eth.EthLag;
import naef.mvo.eth.EthLagIf;
import naef.mvo.eth.EthLink;
import naef.mvo.eth.EthPort;
import naef.mvo.fr.FrPvc;
import naef.mvo.fr.FrPvcIf;
import naef.mvo.isdn.IsdnLink;
import naef.mvo.pos.PosApsIf;
import naef.mvo.pos.PosApsLink;
import naef.mvo.pos.PosLink;
import naef.mvo.serial.SerialLink;
import naef.mvo.vlan.VlanIf;
import naef.mvo.vlan.VlanLink;
import naef.mvo.wdm.WdmLink;
import naef.mvo.wdm.WdmPort;
import tef.skelton.ObjectResolver;
import tef.skelton.ResolveException;

import java.lang.reflect.Constructor;
import java.util.Arrays;

public class ConfigureLinkCommand extends NaefShellCommand {

    private enum Operation {

        CONNECT, DISCONNECT
    }

    @Override public String getArgumentDescription() {
        return "[operation] [type] [port1 qualified name] [port2 qualified name]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 2, 4);
        Operation op = resolveEnum("operation", Operation.class, args.arg(0));

        beginWriteTransaction();

        switch(op) {
            case CONNECT: {
                checkArgsSize(args, 4);
                Class<?> type = resolveNetworkTypeName(args.arg(1)).type();
                String port1QualifiedName = args.arg(2);
                String port2QualifiedName = args.arg(3);

                validateTypeInstantiatable(type);

                Network link = newLink(type, port1QualifiedName, port2QualifiedName);

                setContext(link, null);
                break;
            }
            case DISCONNECT: {
                Network link;
                if (args.args().size() == 2) {
                    try {
                        link = ObjectResolver.<Network>resolve(Network.class, null, getSession(), args.arg(1));
                    } catch (ResolveException re) {
                        throw new ShellCommandException(re.getMessage());
                    }
                } else if (args.args().size() == 4) {
                    Class<? extends Network> type
                        = (Class<? extends Network>) resolveNetworkTypeName(args.arg(1)).type();
                    String port1QualifiedName = args.arg(2);
                    String port2QualifiedName = args.arg(3);
                    link = resolveNetwork(type, Arrays.<String>asList(port1QualifiedName, port2QualifiedName));

                } else {
                    throw new ShellCommandException("引数を確認してください.");
                }

                if (! (link instanceof P2pLink || link instanceof VlanLink)) {
                    throw new ShellCommandException("リンク型ではありません.");
                }
                if (link instanceof Network.LowerStackable
                    && ((Network.LowerStackable) link).getCurrentUpperLayers(false).size() > 0)
                {
                    throw new ShellCommandException("上位層が存在するため解除できません.");
                }

                for (Port port : link.getCurrentMemberPorts()) {
                    port.disjoinNetwork(link);
                }

                break;
            }
        default:
            throw new RuntimeException();
        }

        commitTransaction();
    }

    private Network newLink(Class<?> type, String port1Name, String port2Name)
        throws ShellCommandException
    {
        if (type == AtmApsLink.class) {
            return new AtmApsLink(
                resolvePort(AtmApsIf.class, AtmApsLink.class, port1Name),
                resolvePort(AtmApsIf.class, AtmApsLink.class, port2Name));
        } else if (type == AtmLink.class) {
            return new AtmLink(
                resolvePort(Port.class, AtmLink.class, port1Name),
                resolvePort(Port.class, AtmLink.class, port2Name));
        } else if (type == AtmPvc.class) {
            return new AtmPvc(
                resolvePort(AtmPvcIf.class, AtmPvc.class, port1Name),
                resolvePort(AtmPvcIf.class, AtmPvc.class, port2Name));
        } else if (type == AtmPvp.class) {
            return new AtmPvp(
                resolvePort(AtmPvpIf.class, AtmPvp.class, port1Name),
                resolvePort(AtmPvpIf.class, AtmPvp.class, port2Name));
        } else if (type == EthLag.class) {
            return new EthLag(
                resolvePort(EthLagIf.class, EthLag.class, port1Name),
                resolvePort(EthLagIf.class, EthLag.class, port2Name));
        } else if (type == EthLink.class) {
            return new EthLink(
                resolvePort(EthPort.class, EthLink.class, port1Name),
                resolvePort(EthPort.class, EthLink.class, port2Name));
        } else if (type == FrPvc.class) {
            return new FrPvc(
                resolvePort(FrPvcIf.class, FrPvc.class, port1Name),
                resolvePort(FrPvcIf.class, FrPvc.class, port2Name));
        } else if (type == IsdnLink.class) {
            return new IsdnLink(
                resolvePort(Port.class, IsdnLink.class, port1Name),
                resolvePort(Port.class, IsdnLink.class, port2Name));
        } else if (type == L1Link.class) {
            return new L1Link(
                resolvePort(Port.class, L1Link.class, port1Name),
                resolvePort(Port.class, L1Link.class, port2Name));
        } else if (type == PosApsLink.class) {
            return new PosApsLink(
                resolvePort(PosApsIf.class, PosApsLink.class, port1Name),
                resolvePort(PosApsIf.class, PosApsLink.class, port2Name));
        } else if (type == PosLink.class) {
            return new PosLink(
                resolvePort(Port.class, PosLink.class, port1Name),
                resolvePort(Port.class, PosLink.class, port2Name));
        } else if (type == SerialLink.class) {
            return new SerialLink(
                resolvePort(Port.class, SerialLink.class, port1Name),
                resolvePort(Port.class, SerialLink.class, port2Name));
        } else if (type == VlanLink.class) { 
            return new VlanLink(
                resolvePort(VlanIf.class, VlanLink.class, port1Name),
                resolvePort(VlanIf.class, VlanLink.class, port2Name));
        } else if (type == WdmLink.class) {
            return new WdmLink(
                resolvePort(WdmPort.class, WdmLink.class, port1Name),
                resolvePort(WdmPort.class, WdmLink.class, port2Name));
        } else if (P2pLink.class.isAssignableFrom(type)) { 
            Constructor<? extends P2pLink> constructor = null;
            for (Constructor<?> c : type.getDeclaredConstructors()) {
                Class<?>[] paramTypes = c.getParameterTypes();
                if (paramTypes.length != 2) {
                    continue;
                }
                if (Port.class.isAssignableFrom(paramTypes[0])
                    && Port.class.isAssignableFrom(paramTypes[1]))
                {
                    if (constructor == null) {
                        constructor = (Constructor<? extends P2pLink>) c;
                    } else {
                        throw new RuntimeException("constructor が一意に定まりません.");
                    }
                }
            }
            if (constructor == null) {
                throw new RuntimeException("constructor が定義されていません.");
            }

            Port port1 = resolvePort(Port.class, (Class<? extends P2pLink>) type, port1Name);
            Port port2 = resolvePort(Port.class, (Class<? extends P2pLink>) type, port2Name);
            try {
                return constructor.newInstance(port1, port2);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new ShellCommandException("リンク型ではありません.");
        }
    }

    private <T extends Port> T resolvePort(Class<T> portType, Class<? extends Network> linkType, String name)
        throws ShellCommandException 
    {
        T port = resolve(portType, name);

        if (Network.Exclusive.class.isAssignableFrom(linkType)
            && port.getHereafterNetworks(linkType).size() > 0)
        {
            throw new ShellCommandException("既にリンクが設定されています.");
        }

        return port;
    }
}
