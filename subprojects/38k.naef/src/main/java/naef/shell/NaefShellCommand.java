package naef.shell;

import naef.mvo.Hardware;
import naef.mvo.Jack;
import naef.mvo.Network;
import naef.mvo.NetworkUtils;
import naef.mvo.Node;
import naef.mvo.NodeElement;
import naef.mvo.Port;
import tef.skelton.ResolveException;
import tef.skelton.SkeltonTefService;
import tef.skelton.UiTypeName;
import tef.skelton.shell.SkeltonShellCommand;

import java.util.List;

public abstract class NaefShellCommand extends SkeltonShellCommand {

    @Override protected NaefShellSession getSession() {
        return (NaefShellSession) super.getSession();
    }
    protected UiTypeName resolvePortTypeName(String typeName) throws ShellCommandException {
        return resolveTypeNameAs(Port.class, typeName);
    }

    protected UiTypeName resolveNetworkTypeName(String typeName) throws ShellCommandException {
        return resolveTypeNameAs(Network.class, typeName);
    }

    protected Node resolveNode(String nodeName) throws ShellCommandException {
        Node node = Node.home.getByName(nodeName);
        if (node == null) {
            throw new ShellCommandException("ノードが見つかりません: " + nodeName);
        }

        return node;
    }

    protected Class<? extends Port> resolvePortType(String portTypeName)
        throws ShellCommandException 
    {
        return resolvePortType(Port.class, portTypeName);
    }

    protected <T extends Port> Class<? extends T> resolvePortType(Class<T> expectedType, String portTypeName)
        throws ShellCommandException 
    {
        UiTypeName typename = resolveTypeName(portTypeName);
        if (! expectedType.isAssignableFrom(typename.type())) {
            String expectedTypeName = SkeltonTefService.instance().uiTypeNames().getName(expectedType);
            throw new ShellCommandException(
                "指定された型は" + SkeltonTefService.instance().uiTypeNames().getName(expectedType) + "ではありません.");
        }
        return typename.type().asSubclass(expectedType);
    }

    protected Class<? extends Hardware> resolveHardwareType(String hardwareTypeName)
        throws ShellCommandException
    {
        UiTypeName typename = resolveTypeName(hardwareTypeName);
        if (! Hardware.class.isAssignableFrom(typename.type())) {
            throw new ShellCommandException("指定された型はハードウェアではありません: " + hardwareTypeName);
        }

        return typename.type().asSubclass(Hardware.class);
    }

    protected Class<? extends Network> resolveNetworkType(String networkTypeName)
        throws ShellCommandException
    {
        UiTypeName typename = resolveTypeName(networkTypeName);
        if (! Network.class.isAssignableFrom(typename.type())) {
            throw new ShellCommandException("指定された型はネットワークではありません.");
        }
        return typename.type().asSubclass(Network.class);
    }

    protected <T extends Network> T resolveNetwork(Class<T> networkType, List<String> portQualifiedNames)
        throws ShellCommandException
    {
        List<Port> ports = resolveQualifiedNames(Port.class, portQualifiedNames);

        T result;
        try {
            result = NetworkUtils.resolveHereafterNetwork(networkType, ports);
        } catch (ResolveException re) {
            throw new ShellCommandException(re.getMessage());
        }
        if (result == null) {
            throw new ShellCommandException("ネットワークが見つかりません.");
        }
        return result;
    }

    protected NodeElement contextAsNodeElement() throws ShellCommandException {
        return contextAs(NodeElement.class, "ノード要素");
    }

    protected Node contextAsNode() throws ShellCommandException {
        return contextAs(Node.class, "ノード");
    }

    protected Hardware contextAsHardware() throws ShellCommandException {
        return contextAs(Hardware.class, "ハードウェア");
    }

    protected Jack contextAsJack() throws ShellCommandException {
        return contextAs(Jack.class, "ジャック");
    }

    protected Network contextAsNetwork() throws ShellCommandException {
        return contextAs(Network.class, "ネットワーク/リンク");
    }

    protected Port contextAsPort() throws ShellCommandException {
        return contextAs(Port.class, "ポート");
    }

    protected Hardware resolveHardwareByContext(String typeName, String name)
        throws ShellCommandException
    {
        Hardware owner = contextAsHardware();

        Class<? extends Hardware> type = resolveHardwareType(typeName);

        Hardware subelem = owner.getHereafterSubElement(type, name);
        if (subelem == null) {
            throw new ShellCommandException("ハードウェアが見つかりません.");
        }
        return subelem;
    }
}
