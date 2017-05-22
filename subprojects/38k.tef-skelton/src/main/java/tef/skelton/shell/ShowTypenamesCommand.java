package tef.skelton.shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import lib38k.text.TextTable;
import tef.skelton.SkeltonTefService;
import tef.skelton.UiTypeName;

public class ShowTypenamesCommand extends SkeltonShellCommand {

    @Override public String getArgumentDescription() {
        return "";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 0);

        List<UiTypeName> typenames = new ArrayList<UiTypeName>(SkeltonTefService.instance().uiTypeNames().instances());
        Collections.sort(typenames, new Comparator<UiTypeName>() {

            @Override public int compare(UiTypeName o1, UiTypeName o2) {
                return o1.name().compareTo(o2.name());
            }
        });

        TextTable table = new TextTable(new String[] { "type name", "java type" });
        for (UiTypeName typename : typenames) {
            table.addRow(typename.name(), typename.type().getName());
        }
        printTable(table);
    }
}
