package tef.skelton.shell;

import lib38k.parser.Ast;
import lib38k.parser.ParseException;
import lib38k.parser.StringToken;
import lib38k.text.TextTable;
import tef.MVO;
import tef.skelton.ObjectQueryExpression;

import java.util.ArrayList;
import java.util.List;

public class ObjectQueryExpressionCommand extends SkeltonShellCommand {

    @Override public String getArgumentDescription() {
        return "[expression]...";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 1, Integer.MAX_VALUE);

        beginReadTransaction();

        try {
            Ast ast = Ast.parse(ObjectQueryExpression.COLLECTION, StringToken.newTokenStream(args.args()));

            List<ObjectQueryExpression.Row> queryresult = (List<ObjectQueryExpression.Row>) ast.evaluate();
            List<String[]> tabledata = new ArrayList<String[]>();
            for (ObjectQueryExpression.Row row : queryresult) {
                List<String> columnStrs = new ArrayList<String>();
                for (Object column : row.columns()) {
                    String columnStr;
                    if (column == null) {
                        columnStr = "";
                    } else if (column instanceof MVO) {
                        columnStr = ((MVO) column).getMvoId().getLocalStringExpression();
                    } else {
                        columnStr = column.toString();
                    }
                    columnStrs.add(columnStr);
                }
                tabledata.add(columnStrs.toArray(new String[0]));
            }
            if (tabledata.size() == 0) {
                return;
            }

            int columnsCount = tabledata.get(0).length;
            List<String> tablecolumns = new ArrayList<String>();
            for (int i = 0; i < columnsCount; i++) {
                tablecolumns.add("c" + Integer.toString(i));
            }

            println("result: " + Integer.toString(tabledata.size()) + " rows.");

            TextTable table = new TextTable(tablecolumns.toArray(new String[0]));
            table.addRows((String[][]) tabledata.toArray(new String[0][]));

            printTable(table);
        } catch (ParseException pe) {
            throw new ShellCommandException(pe.getMessage());
        } catch (ObjectQueryExpression.EvaluationException ee) {
            throw new ShellCommandException(ee.getMessage());
        }
    }
}
