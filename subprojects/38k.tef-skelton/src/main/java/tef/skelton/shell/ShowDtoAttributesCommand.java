package tef.skelton.shell;

import lib38k.text.TextTable;
import tef.DateTime;
import tef.MVO;
import tef.skelton.Model;
import tef.skelton.NamedModel;
import tef.skelton.SkeltonTefService;
import tef.skelton.dto.Dto2Mvo;
import tef.skelton.dto.EntityDto;
import tef.skelton.dto.MvoDtoDesc;
import tef.skelton.dto.MvoDtoFactory;
import tef.skelton.dto.MvoDtoOriginator;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;

public class ShowDtoAttributesCommand extends SkeltonShellCommand {

    @Override public String getArgumentDescription() {
        return "";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 0);
        Model mvo = getContext();
        if (mvo == null) {
            throw new ShellCommandException("コンテキストが設定されていません.");
        }

        MvoDtoFactory dtoFactory = SkeltonTefService.instance().getMvoDtoFactory();

        beginReadTransaction();

        EntityDto dto = dtoFactory.build(new MvoDtoOriginator(null), (MVO) mvo);
        if (dto == null) {
            throw new ShellCommandException("DTOが未定義です.");
        }

        TextTable table = new TextTable(new String[] {"name", "value"});
        for (String attrName : new TreeSet<String>(dto.getAttributeNames())) {
            table.addRow(attrName, getValueString(dtoFactory, dto.getValue(attrName)));
        }
        printTable(table);
    }

    private String getValueString(MvoDtoFactory dtoFactory, Object value) {
        if (value == null) {
            return "";
        } else if (value instanceof NamedModel) {
            return ((NamedModel) value).getName();
        } else if (value instanceof EntityDto) {
            return "mvo-dto:" + ((EntityDto) value).getOid().toString();
        } else if (value instanceof MvoDtoDesc<?>) {
            return getValueString(dtoFactory, dtoFactory.build(null, Dto2Mvo.toMvo((MvoDtoDesc<?>) value)));
        } else if (value instanceof Collection<?>) {
            StringBuilder result = new StringBuilder();
            for (Object elem : (Collection<?>) value) {
                result.append(result.length() == 0 ? "" : ", ");
                result.append(getValueString(dtoFactory, elem));
            }
            return "{" + result.toString() + "}";
        } else if (value instanceof Map<?, ?>) {
            Map<Object, Object> map = (Map<Object, Object>) value;
            StringBuilder result = new StringBuilder();
            for (Object k : map.keySet()) {
                Object v = map.get(k);
                result.append(result.length() == 0 ? "" : ", ");
                result.append(getValueString(dtoFactory, k) + ":" + getValueString(dtoFactory, v));
            }
            return "{" + result.toString() + "}";
        } else if (value instanceof Enum) {
            return ((Enum) value).name().replace('_', '-').toLowerCase();
        } else if (value instanceof DateTime) {
            return new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss").format(((DateTime) value).toJavaDate());
        } else {
            return value.toString();
        }
    }
}
