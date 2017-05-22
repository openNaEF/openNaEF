package tef.skelton.shell;

import tef.DateTime;
import tef.MVO;
import tef.TefService;
import tef.skelton.SkeltonTefService;
import tef.skelton.UiTypeName;
import tef.skelton.dto.Dto2Mvo;
import tef.skelton.dto.EntityDto;
import tef.skelton.dto.MvoDtoDesc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class DumpCommand extends SkeltonShellCommand {

    public static class Renderer {

        private DateFormat dateformat_ = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss.SSS");

        public String render(Object o) {
            if (o == null) {
                return "<null>";
            } else if (o instanceof String) {
                return renderString((String) o);
            } else if (o instanceof tef.DateTime) {
                return renderDateTime((DateTime) o);
            } else if (o instanceof EntityDto) {
                return renderEntityDto((EntityDto) o);
            } else if (o instanceof EntityDto.Desc<?>) {
                return renderEntityDescriptor((EntityDto.Desc<?>) o);
            } else {
                return renderOther(o);
            }
        }

        protected String renderString(String o) {
            return "\"" + o + "\"";
        }

        protected String renderDateTime(DateTime o) {
            return dateformat_.format(o.toJavaDate());
        }

        protected String renderEntityDto(EntityDto o) {
            return o.getOid().toString();
        }

        protected String renderEntityDescriptor(EntityDto.Desc<?> o) {
            return "ref:" + renderEntityDto(buildDto(Dto2Mvo.toMvo((MvoDtoDesc<?>) o)));
        }

        protected String renderOther(Object o) {
            return o.toString();
        }
    }

    public static class ObjectComparator implements Comparator<Object> {

        @Override public int compare(Object o1, Object o2) {
            return render(o1).compareTo (render(o2));
        }
    }

    public static void sort(List<?> list) {
        Collections.sort(list, new ObjectComparator());
    }

    private static Renderer renderer__ = new Renderer();

    public static void setRenderer(Renderer renderer) {
        renderer__ = renderer;
    }

    public static String render(Object o) {
        return renderer__.render(o);
    }

    private static EntityDto buildDto(MVO mvo) {
        return SkeltonTefService.instance().getMvoDtoFactory().build(null, mvo);
    }

    @Override public String getArgumentDescription() {
        return "";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 0);

        beginReadTransaction();

        try {
            File outDir = new File(".", "dump");
            if (! outDir.exists()) {
                outDir.mkdir();
            } else if (! outDir.isDirectory()) {
                throw new ShellCommandException("出力先エラー: " + outDir.getCanonicalPath());
            }

            for (UiTypeName typename : SkeltonTefService.instance().uiTypeNames().instances()) {
                dump(outDir, typename);
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private void dump(File outDir, UiTypeName typename)
        throws ShellCommandException, IOException
    {
        File file = new File(outDir, typename.name() + ".dump.txt");
        if (file.exists()) {
            throw new ShellCommandException("出力ファイル " + file.getName() + " は既に存在します.");
        }

        PrintStream out = null;
        try {
            out = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)));

            for (EntityDto dto : buildDtos(typename)) {
                dump(out, dto);
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private List<EntityDto> buildDtos(UiTypeName typename) {
        List<EntityDto> result = new ArrayList<EntityDto>();
        for (MVO mvo : TefService.instance().getMvoRegistry().list()) {
            if (typename.type().isInstance(mvo)) {
                result.add(buildDto(mvo));
            }
        }
        Collections.sort(result, new ObjectComparator());
        return result;
    }

    private void dump(PrintStream out, EntityDto dto) {
        out.println(render(dto));
        for (String attrName : dto.getAttributeNames()) {
            Object attrValue = dto.getValue(attrName);
            print(out, "  ", attrName, attrValue);
        }
    }

    private void print(PrintStream out, String indent, String attrName, Object attrValue) {
        if (attrValue instanceof Collection<?>) {
            out.println(indent + attrName);

            List<Object> values;
            if (attrValue instanceof List<?>) {
                values = new ArrayList<Object>((List<?>) attrValue);
            } else if (attrValue instanceof Set<?>) {
                values = new ArrayList<Object>((Set<?>) attrValue);
                Collections.sort(values, new ObjectComparator());
            } else {
                throw new RuntimeException("unsupported, " + attrValue.getClass().getName());
            }

            for (Object o : values) {
                out.println(indent + " " + render(o));
            }
        } else if (attrValue instanceof Map<?, ?>) {
            out.println(indent + attrName);

            SortedMap<Object, Object> map;
            if (attrValue instanceof SortedMap<?, ?>) {
                map = new TreeMap<Object, Object>((SortedMap<?, ?>) attrValue);
            } else {
                map = new TreeMap<Object, Object>(new ObjectComparator());
                map.putAll((Map<?, ?>) attrValue);
            }

            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                out.println(indent + " " + render(entry.getKey()) + "\t" + render(entry.getValue()));
            }
        } else {
            out.println(indent + attrName + "\t" + render(attrValue));
        }
    }
}
