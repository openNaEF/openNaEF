package tef;

import lib38k.io.IoUtils;
import lib38k.storage.Catalog;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class StackTraceCatalog {

    public static final class CatalogId implements java.io.Serializable {

        final String tefServiceId;
        final String type;
        final String catalogElementId;

        CatalogId(String type, String catalogElementId) {
            this.tefServiceId = TefService.instance().getServiceName();
            this.type = type;
            this.catalogElementId = catalogElementId;
        }

        public String getLocalId() {
            return type + ":" + catalogElementId;
        }

        public String getGlobalId() {
            return tefServiceId + ":" + getLocalId();
        }

        public static CatalogId parseAsLocalId(String idStr) {
            String[] tokens = idStr.split(":");
            if (tokens.length != 2) {
                return null;
            }

            String type = tokens[0];
            String catalogElementId = tokens[1];
            return new CatalogId(type, catalogElementId);
        }
    }

    private static final String OMITTED_LINE = "...";
    private static Collection<String> regExpsOfStackTraceLineToOmit__ = null;

    private String type_;
    private File dir_;
    private Catalog storageCatalog_;

    public StackTraceCatalog(String type) {
        type_ = type;
        dir_ = IoUtils.initializeDirectory
                (TefService.instance().getLogs().getStacktraceCatalogsDirectory(), type);

        storageCatalog_ = new Catalog(dir_);
    }

    public CatalogId getCurrentThreadStackTraceCatalogId(int omitDepth) {
        try {
            StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
            byte[] stacktrace = getStacktrace(stackTraceElements, omitDepth + 1);
            String catalogId = storageCatalog_.store(stacktrace);
            return new CatalogId(getType(), catalogId);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private byte[] getStacktrace(StackTraceElement[] stacktraces, int omitDepth) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(new BufferedOutputStream(baos));
        for (int i = omitDepth; i < stacktraces.length; i++) {
            String line = stacktraces[i].toString();
            if (isToOmit(line)) {
                if (0 < i && isToOmit(stacktraces[i - 1].toString())) {
                    continue;
                }

                line = OMITTED_LINE;
            }

            out.println(line);
        }
        out.close();
        return baos.toByteArray();
    }

    private static boolean isToOmit(String stackTraceLine) {
        if (regExpsOfStackTraceLineToOmit__ == null) {
            TefServiceConfig.StackTraceCatalogConfig config
                    = TefService.instance().getTefServiceConfig().stackTraceCatalogConfig;
            if (config == null || config.omissionLineRegExps == null) {
                regExpsOfStackTraceLineToOmit__ = Collections.<String>emptyList();
            } else {
                regExpsOfStackTraceLineToOmit__ = config.omissionLineRegExps;
            }
        }

        for (String regExpsOfStackTraceLineToOmit : regExpsOfStackTraceLineToOmit__) {
            if (stackTraceLine.matches(regExpsOfStackTraceLineToOmit)) {
                return true;
            }
        }
        return false;
    }

    String getType() {
        return type_;
    }

    synchronized List<String> getStacktraceLines(String catalogId) {
        try {
            byte[] contents = storageCatalog_.retrieve(catalogId);
            if (contents == null) {
                return null;
            }

            List<String> result = new ArrayList<String>();
            BufferedReader in
                    = new BufferedReader
                    (new InputStreamReader(new ByteArrayInputStream(contents)));
            String line;
            while ((line = in.readLine()) != null) {
                result.add(line);
            }
            in.close();
            return result;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
