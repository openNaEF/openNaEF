package naef.shell;

import naef.mvo.Port;
import naef.mvo.of.OfPatchLink;

public class ConfigureOfPatchLinkCommand extends NaefShellCommand {

    private enum Operation {

        SET_PATCH_PORT1, SET_PATCH_PORT2, RESET_PATCH_PORT1, RESET_PATCH_PORT2
    }

    @Override public String getArgumentDescription() {
        return "[set-patch-port1, set-patch-port2, reset-patch-port1, reset-patch-port2]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        Operation op = resolveEnum("operation", Operation.class, args.arg(0));

        beginWriteTransaction();

        switch(op) {
            case SET_PATCH_PORT1:
                checkArgsSize(args, 2);
                contextAsOfPatchLink().setPatchPort1(resolveAsPortFqn(args.arg(1)));
                break;
            case SET_PATCH_PORT2:
                checkArgsSize(args, 2);
                contextAsOfPatchLink().setPatchPort2(resolveAsPortFqn(args.arg(1)));
                break;
            case RESET_PATCH_PORT1:
                checkArgsSize(args, 1);
                contextAsOfPatchLink().resetPatchPort1();
                break;
            case RESET_PATCH_PORT2:
                checkArgsSize(args, 1);
                contextAsOfPatchLink().resetPatchPort2();
                break;
            default:
                throw new RuntimeException();
        }

        commitTransaction();
    }

    private OfPatchLink contextAsOfPatchLink() throws ShellCommandException {
        return contextAs(OfPatchLink.class, "of-patch-link");
    }

    private Port resolveAsPortFqn(String portFqn) throws ShellCommandException {
        return resolve(Port.class, portFqn);
    }
}
