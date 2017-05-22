package naef.shell;

import naef.mvo.Network;
import naef.mvo.Port;
import tef.skelton.ConfigurationException;

public class ConfigureNetworkPortCommand extends NaefShellCommand {

    private enum Operation {

        ADD_MEMBER {

            @Override void process(Network.MemberPortConfigurable network, Port port) {
                network.addMemberPort(port);
            }
        },

        REMOVE_MEMBER {

            @Override void process(Network.MemberPortConfigurable network, Port port) {
                network.removeMemberPort(port);
            }
        };

        abstract void process(Network.MemberPortConfigurable network, Port port)
            throws ShellCommandException;
    }

    @Override public String getArgumentDescription() {
        return "[operation] [port qualified name]";
    }

    @Override public void process(Commandline args)
        throws ShellCommandException
    {
        Network context = contextAsNetwork();
        if (! (context instanceof Network.MemberPortConfigurable)) {
            throw new ShellCommandException("コンテキストはport configurableではありません.");
        }

        checkArgsSize(args, 2);
        Operation op = resolveEnum("operation", Operation.class, args.arg(0));
        String portQualifiedName = args.arg(1);

        beginWriteTransaction();

        try {
            op.process((Network.MemberPortConfigurable) context, resolve(Port.class, portQualifiedName));
        } catch (ConfigurationException ce) {
            throw new ShellCommandException(ce.getMessage());
        }

        commitTransaction();
    }
}
