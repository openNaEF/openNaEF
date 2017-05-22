package tef.ui.shell;

import lib38k.text.TextTable;
import tef.MvoSummaryComputer;
import tef.TransactionContext;
import tef.TransactionId;

import java.text.DecimalFormat;

public class ComputeMvoSummaryCommand extends ShellCommand {

    public String getArgumentDescription() {
        return "[transaction-id (optional)]";
    }

    @Override
    public void process(Commandline commandline) throws ShellCommandException {
        checkArgsSize(commandline, 0, 1);

        beginReadTransaction();

        TransactionId.W targetTransaction
                = commandline.arg(0) == null
                ? TransactionContext.getLastCommittedTransactionId()
                : new TransactionId.W(Integer.parseInt(commandline.arg(0), 16));

        MvoSummaryComputer computer = new MvoSummaryComputer(targetTransaction);
        computer.compute();

        DecimalFormat format = new DecimalFormat();

        TextTable table = new TextTable
                (new TextTable.Column[]{
                        new TextTable.Column("", TextTable.Column.Alignment.RIGHT),
                        new TextTable.Column("count", TextTable.Column.Alignment.RIGHT),
                        new TextTable.Column("elements", TextTable.Column.Alignment.RIGHT)});
        table.addRow
                ("mvos", format.format(computer.mvoCount), "");
        table.addRow
                ("f1", format.format(computer.f1HistoryCount), "");
        table.addRow
                ("m1",
                        format.format(computer.m1HistoryCount),
                        format.format(computer.m1ElementsCount));
        table.addRow
                ("s1",
                        format.format(computer.s1HistoryCount),
                        format.format(computer.s1ElementsCount));
        table.addRow
                ("f2", format.format(computer.f2HistoryCount), "");
        table.addRow
                ("s2",
                        format.format(computer.s2HistoryCount),
                        format.format(computer.s2ElementsCount));
        printTable(table);
    }
}
