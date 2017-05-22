package tef.ui.shell;

import tef.NoTransactionContextFoundException;
import tef.TransactionContext;
import tef.TransactionId;

public class KillTransactionCommand extends ShellCommand {

    public String getArgumentDescription() {
        return "[transaction id]";
    }

    @Override
    public void process(Commandline commandline) throws ShellCommandException {
        checkArgsSize(commandline, 1);

        try {
            TransactionContext
                    .killTransaction(TransactionId.getInstance(commandline.arg(0)));
        } catch (NoTransactionContextFoundException ntcfe) {
            println("no such transaction.");
        }
    }
}
