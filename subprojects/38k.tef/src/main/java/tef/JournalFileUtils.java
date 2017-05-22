package tef;

import lib38k.io.IoUtils;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

final class JournalFileUtils {

    private static final DateFormat dateDirNameFormatter__ = new SimpleDateFormat("yyyyMMdd");

    static File getJournalFilePath(int transactionRawId, long time, String suffix) {
        File dateDir = getDateDir(time);

        String filename = TefUtils.hexInt(transactionRawId) + suffix;
        File logFile = new File(dateDir, filename);
        if (logFile.exists()) {
            throw new Error("log file already exists: " + logFile.getAbsolutePath());
        }

        return logFile;
    }

    static File getDateDir(long time) {
        String dateDirName;
        synchronized (dateDirNameFormatter__) {
            dateDirName = dateDirNameFormatter__.format(new java.util.Date(time));
        }

        return IoUtils.initializeDirectory
                (TefService.instance().getLogs().getJournalsDirectory(), dateDirName);
    }

    static Long parseDate(String str) {
        try {
            synchronized (dateDirNameFormatter__) {
                return new Long(dateDirNameFormatter__.parse(str).getTime());
            }
        } catch (ParseException pe) {
            return null;
        }
    }
}
