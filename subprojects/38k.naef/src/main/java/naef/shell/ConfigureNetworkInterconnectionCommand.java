package naef.shell;

import naef.mvo.Network;
import tef.skelton.KnownRuntimeException;
import tef.skelton.ObjectResolver;
import tef.skelton.ResolveException;

public class ConfigureNetworkInterconnectionCommand extends NaefShellCommand {

    private enum Operation {

        INCLUDE {

            @Override void process(Network connector, Network connectee)
                throws ShellCommandException {
                if (! (connector instanceof Network.Container)) {
                    throw new ShellCommandException("コンテキストはcontainerではありません.");
                }
                if (! (connectee instanceof Network.Containee)) {
                    throw new ShellCommandException("引数はcontaineeではありません.");
                }

                ((Network.Container) connector).addPart((Network.Containee) connectee);
            }
        },

        EXCLUDE {

            @Override void process(Network connector, Network connectee)
                throws ShellCommandException {
                if (! (connector instanceof Network.Container)) {
                    throw new ShellCommandException("コンテキストはcontainerではありません.");
                }
                if (! (connectee instanceof Network.Containee)) {
                    throw new ShellCommandException("引数はcontaineeではありません.");
                }

                ((Network.Container) connector).removePart((Network.Containee) connectee);
            }
        },

        STACK {

            @Override void process(Network connector, Network connectee)
                throws ShellCommandException {
                if (! (connector instanceof Network.UpperStackable)) {
                    throw new ShellCommandException("コンテキストはupper-stackableではありません.");
                }
                if (! (connectee instanceof Network.LowerStackable)) {
                    throw new ShellCommandException("引数はlower-stackableではありません.");
                }

                ((Network.UpperStackable) connector).stackOver((Network.LowerStackable) connectee);
            }
        },

        UNSTACK {

            @Override void process(Network connector, Network connectee)
                throws ShellCommandException {
                if (! (connector instanceof Network.UpperStackable)) {
                    throw new ShellCommandException("コンテキストはupper-stackableではありません.");
                }
                if (! (connectee instanceof Network.LowerStackable)) {
                    throw new ShellCommandException("引数はlower-stackableではありません.");
                }

                ((Network.UpperStackable) connector).unstackOver((Network.LowerStackable) connectee);
            }
        };

        abstract void process(Network connector, Network connectee)
            throws ShellCommandException;
    }

    @Override public String getArgumentDescription() {
        return "[operation] ([network descriptor(fqn, mvo-id)] | [connectee network type] "
            + "[connectee network port]...)";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        Network context = contextAsNetwork();

        checkArgsSize(args, 2, Integer.MAX_VALUE);
        Operation op = resolveEnum("operation", Operation.class, args.arg(0));

        beginWriteTransaction();

        Network connectee;
        if (args.args().size() == 2) {
            try {
                connectee = ObjectResolver.<Network>resolve(Network.class, null, getSession(), args.arg(1));
            } catch (ResolveException re) {
                throw new ShellCommandException(re.getMessage());
            }
        } else {
            Class<? extends Network> networkType = resolveNetworkType(args.arg(1));
            connectee = resolveNetwork(networkType, args.args().subList(2, args.args().size()));
        }

        try {
            op.process(context, connectee);
        } catch (KnownRuntimeException kre) {
            throw new ShellCommandException(kre.getMessage());
        }

        commitTransaction();
    }
}
