package voss.discovery.iolib.simulation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class SimulationUtil {

    public static SimulationArchive selectSimulationArchive(File file) throws IOException {
        ZipFile archiveZip = new ZipFile(file);
        if (isOldFormat(archiveZip)) {
            return new OldSimulationArchive(file);
        } else {
            return new SimulationArchiveImpl(file);
        }
    }

    private static boolean isOldFormat(ZipFile archiveZip) {
        ZipEntry entry = archiveZip.getEntry("META-INFO.txt");
        return entry == null;
    }

    public static List<String> getEntryList(File file) throws IOException {
        List<String> result = new ArrayList<String>();

        ZipFile archiveZip = new ZipFile(file);
        boolean oldFormat = isOldFormat(archiveZip);
        Enumeration<?> list = archiveZip.entries();
        while (list.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) list.nextElement();
            String name = entry.getName();
            if (oldFormat && name.endsWith(".pdu")) {
                name = name.replace(".pdu", "");
                result.add(name);
            } else if (!oldFormat && name.endsWith("/")) {
                int first = name.indexOf('/');
                int last = name.lastIndexOf('/');
                if (first == last) {
                    name = name.replace("/", "");
                    result.add(name);
                }
            }
        }
        return result;
    }

}