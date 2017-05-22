package naef.shell;

import lib38k.text.TextTable;
import naef.mvo.NodeElement;
import tef.skelton.AbstractHierarchicalModel;
import tef.skelton.Model;
import tef.skelton.NamedModel;
import tef.skelton.SkeltonTefService;
import tef.skelton.UiTypeName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ShowElementsCommand extends NaefShellCommand {

    @Override public String getArgumentDescription() {
        return "";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 0);

        Model context = getContext();
        if (context == null) {
            throw new ShellCommandException("コンテキストが指定されていません.");
        }

        beginReadTransaction();

        TextTable table;
        if (context instanceof NodeElement) {
            List<NodeElement> subelems = new ArrayList<NodeElement>(((NodeElement) context).getCurrentSubElements());
            Collections.sort(subelems, new Comparator<NodeElement>() {

                @Override public int compare(NodeElement o1, NodeElement o2) {
                    String name1 = o1.getName();
                    String name2 = o2.getName();
                    if (name1 == null && name2 == null) {
                        return 0;
                    }
                    if (name1 == null) {
                        return -1;
                    }
                    if (name2 == null) {
                        return 1;
                    }
                    return name1.compareTo(name2);
                }
            });

            table = new TextTable(new String[] { "type", "name" });
            for (NodeElement subelem : subelems) {
                UiTypeName typename = SkeltonTefService.instance().uiTypeNames().getAdaptive(subelem);
                table.addRow(typename == null ? "?" : typename.name(), subelem.getName());
            }
        } else if (context instanceof AbstractHierarchicalModel
                   && context instanceof NamedModel)
        {
            table = new TextTable(new String[] { "name" });
            List<String[]> tableData = new ArrayList<String[]>();
            for (AbstractHierarchicalModel<?> child : ((AbstractHierarchicalModel<?>) context).getChildren()) {
                table.addRow(((NamedModel) child).getName());
            }
        } else {
            throw new ShellCommandException("現在指定されているコンテキストはサブ要素を表示できる対象ではありません.");
        }
        printTable(table);
    }
}
